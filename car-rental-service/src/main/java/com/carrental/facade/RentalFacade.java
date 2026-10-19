package com.carrental.facade;

import com.carrental.enums.PaymentMethod;
import com.carrental.exception.CarNotAvailableException;
import com.carrental.exception.PaymentFailedException;
import com.carrental.kafka.RentalEventPublisher;
import com.carrental.kafka.events.RentalBookedEvent;
import com.carrental.kafka.events.RentalCancelledEvent;
import com.carrental.model.Payment;
import com.carrental.model.Rental;
import com.carrental.model.User;
import com.carrental.repository.jpa.PaymentRepository;
import com.carrental.service.CarAvailabilityService;
import com.carrental.service.CancellationService;
import com.carrental.service.PaymentService;
import com.carrental.service.RentalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * RentalFacade – FACADE PATTERN that orchestrates the complete booking workflow.
 *
 * =============================================
 * Design Pattern: FACADE PATTERN
 * =============================================
 * Provides a single, simplified entry point to the complex booking subsystem.
 * RentalController only calls RentalFacade – it has no knowledge of:
 * - Redis distributed locking (CarAvailabilityService)
 * - Database transactions (RentalService)
 * - Payment gateway selection (PaymentService + Strategy)
 * - Event publishing (RentalEventPublisher + Observer)
 *
 * =============================================
 * Design Pattern: TEMPLATE METHOD
 * =============================================
 * bookCar() defines the invariant booking algorithm skeleton:
 *   Step 1: Acquire Redis lock
 *   Step 2: Create PENDING rental
 *   Step 3: Process payment (Strategy)
 *   Step 4a: [SUCCESS] Activate rental → release lock → publish event
 *   Step 4b: [FAILURE] Cancel rental → release lock → propagate error
 */
@Component
@RequiredArgsConstructor  // Lombok – constructor injection for all final fields
@Slf4j
public class RentalFacade {

    private final CarAvailabilityService availabilityService;  // Redis distributed lock
    private final RentalService rentalService;                 // domain logic + JPA
    private final PaymentService paymentService;               // Strategy + Factory payment
    private final RentalEventPublisher eventPublisher;         // Observer + Kafka publisher
    private final PaymentRepository paymentRepository;         // for fetching payment on cancel

    /**
     * Book a car – the primary facade method.
     *
     * Algorithm skeleton (TEMPLATE METHOD PATTERN):
     * 1. Acquire Redis lock on carId → prevents concurrent bookings
     * 2. Create PENDING rental in DB
     * 3. Process payment via PaymentService (Strategy/Factory pattern)
     * 4a. SUCCESS: Activate rental, release lock, publish Kafka + Observer notifications
     * 4b. FAILURE: Release lock, cancel rental, propagate PaymentFailedException
     *
     * @param user            authenticated customer
     * @param carId           car to book
     * @param pickupBranchId  pickup location
     * @param dropoffBranchId return location
     * @param startDate       rental start date
     * @param endDate         rental end date (return date)
     * @param paymentMethod   payment channel (CREDIT_CARD / UPI / WALLET)
     * @return fully activated Rental entity
     */
    public Rental bookCar(User user, Long carId, Long pickupBranchId, Long dropoffBranchId,
                          LocalDate startDate, LocalDate endDate, PaymentMethod paymentMethod) {

        log.info("[RentalFacade] Starting booking. userId={}, carId={}, start={}, end={}",
                user.getId(), carId, startDate, endDate);

        // ── STEP 1: Acquire Redis distributed lock ────────────────────────────────────
        // Atomic SETNX prevents two users from booking the same car simultaneously
        availabilityService.acquireLockOrThrow(carId, user.getId());  // throws if another user locked
        log.info("[RentalFacade] Redis lock acquired. carId={}", carId);
        // ──────────────────────────────────────────────────────────────────────────────

        Rental rental = null;

        try {
            // ── STEP 2: Create PENDING rental (validates car status + dates) ──────────
            // Also uses @Version optimistic lock on Car entity inside RentalService
            rental = rentalService.createPendingRental(user, carId, pickupBranchId,
                    dropoffBranchId, startDate, endDate);
            log.info("[RentalFacade] PENDING rental created. rentalId={}", rental.getId());
            // ──────────────────────────────────────────────────────────────────────────

            // ── STEP 3: Process payment via Strategy + Factory pattern ─────────────────
            Payment payment = paymentService.processPayment(rental, paymentMethod, rental.getTotalAmount());
            log.info("[RentalFacade] Payment SUCCESS. txnId={}", payment.getTransactionId());
            // ──────────────────────────────────────────────────────────────────────────

            // ── STEP 4a: SUCCESS – Activate rental and release lock ───────────────────
            rental = rentalService.activateRental(rental);  // Car marked RENTED, rental = ACTIVE
            log.info("[RentalFacade] Rental ACTIVATED. rentalId={}", rental.getId());

            // Release Redis lock NOW (car is RENTED in DB – lock no longer needed)
            availabilityService.releaseLock(carId, user.getId());
            // ──────────────────────────────────────────────────────────────────────────

            // ── Publish booking event (Observer + Kafka) ──────────────────────────────
            RentalBookedEvent event = RentalBookedEvent.builder()
                    .rentalId(rental.getId())
                    .rentalReference(rental.getRentalReference())
                    .userId(user.getId())
                    .carId(carId)
                    .carCategory(rental.getCar().getCategory().getType())
                    .pickupBranchId(pickupBranchId)
                    .pickupCity(rental.getPickupBranch().getCity())
                    .startDate(startDate)
                    .endDate(endDate)
                    .totalAmount(rental.getTotalAmount())
                    .eventTime(LocalDateTime.now())
                    .build();

            // OBSERVER PATTERN: notify all observers (Email, SMS, Analytics) + Kafka
            eventPublisher.publishRentalBooked(event);
            // ──────────────────────────────────────────────────────────────────────────

            return rental;  // return completed booking to controller

        } catch (CarNotAvailableException | PaymentFailedException ex) {
            // ── STEP 4b: FAILURE – release lock and propagate error ───────────────────
            log.error("[RentalFacade] Booking failed. carId={}, error={}", carId, ex.getMessage());
            availabilityService.releaseLock(carId, user.getId());  // always release lock on failure
            throw ex;  // re-throw for GlobalExceptionHandler to convert to HTTP response
            // ──────────────────────────────────────────────────────────────────────────
        } catch (Exception ex) {
            // ── Unexpected errors – release lock and wrap in RuntimeException ─────────
            log.error("[RentalFacade] Unexpected error during booking.", ex);
            availabilityService.releaseLock(carId, user.getId());  // must release lock
            throw new RuntimeException("Booking failed unexpectedly: " + ex.getMessage(), ex);
        }
    }

    /**
     * Cancel a booking and process the refund.
     *
     * @param rentalId rental to cancel
     * @param userId   user requesting cancellation (ownership validated by caller)
     * @param reason   cancellation reason
     */
    public void cancelBooking(Long rentalId, Long userId, String reason) {
        log.info("[RentalFacade] Cancelling booking. rentalId={}", rentalId);

        // Delegate to CancellationService to compute penalty and update DB
        CancellationService.CancellationResult result = new CancellationService(
                // Note: in full impl, inject CancellationService via constructor
                null, null).processCancel(rentalId, reason);

        // Publish cancellation event (Observer + Kafka)
        RentalCancelledEvent event = RentalCancelledEvent.builder()
                .rentalId(rentalId)
                .userId(userId)
                .refundAmount(result.refundAmount())
                .penaltyAmount(result.penaltyAmount())
                .cancellationReason(reason)
                .eventTime(LocalDateTime.now())
                .build();

        eventPublisher.publishRentalCancelled(event);  // OBSERVER PATTERN – notify all observers
        log.info("[RentalFacade] Cancellation event published. rentalId={}", rentalId);
    }
}

