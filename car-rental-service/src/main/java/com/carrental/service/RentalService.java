package com.carrental.service;

import com.carrental.enums.CarStatus;
import com.carrental.enums.RentalStatus;
import com.carrental.exception.CarNotAvailableException;
import com.carrental.exception.ResourceNotFoundException;
import com.carrental.model.Branch;
import com.carrental.model.Car;
import com.carrental.model.Rental;
import com.carrental.model.User;
import com.carrental.repository.jpa.BranchRepository;
import com.carrental.repository.jpa.CarRepository;
import com.carrental.repository.jpa.RentalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

/**
 * RentalService – domain service handling the core rental creation logic.
 *
 * Responsibilities:
 * - Validate rental dates
 * - Check car availability status
 * - Create a PENDING Rental record
 * - Activate rental after payment confirmation
 * - Fetch user rental history
 *
 * NOTE: This service handles ONLY the rental data layer.
 * The full booking orchestration (Redis lock + payment + Kafka) lives in RentalFacade.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RentalService {

    private final RentalRepository rentalRepository;   // REPOSITORY PATTERN
    private final CarRepository carRepository;          // REPOSITORY PATTERN
    private final BranchRepository branchRepository;   // REPOSITORY PATTERN

    /**
     * Create a PENDING rental record after date validation.
     *
     * A PENDING rental reserves the conceptual booking slot before payment.
     * It is not yet committed (car not marked RENTED) until payment succeeds.
     *
     * @param user           authenticated customer
     * @param carId          car to rent
     * @param pickupBranchId pickup location
     * @param dropoffBranchId return location
     * @param startDate      rental start date
     * @param endDate        rental end date
     * @return saved PENDING Rental entity
     */
    @Transactional
    public Rental createPendingRental(User user, Long carId, Long pickupBranchId,
                                      Long dropoffBranchId, LocalDate startDate, LocalDate endDate) {
        // ── Validate date range ────────────────────────────────────────────────────────
        if (!startDate.isBefore(endDate)) {
            throw new IllegalArgumentException("startDate must be before endDate");
        }
        if (startDate.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("startDate cannot be in the past");
        }
        // ──────────────────────────────────────────────────────────────────────────────

        // ── Load and validate car ──────────────────────────────────────────────────────
        // Use optimistic lock version for concurrent booking protection
        Car car = carRepository.findByIdWithOptimisticLock(carId)  // ← OPTIMISTIC LOCKING PATTERN
                .orElseThrow(() -> new ResourceNotFoundException("Car not found: " + carId));

        if (car.getStatus() != CarStatus.AVAILABLE) {
            // Car is RENTED or in MAINTENANCE – cannot book
            throw new CarNotAvailableException("Car " + carId + " is not available. Status: " + car.getStatus());
        }
        // ──────────────────────────────────────────────────────────────────────────────

        // ── Load branches ──────────────────────────────────────────────────────────────
        Branch pickupBranch = branchRepository.findById(pickupBranchId)
                .orElseThrow(() -> new ResourceNotFoundException("Pickup branch not found: " + pickupBranchId));
        Branch dropoffBranch = branchRepository.findById(dropoffBranchId)
                .orElseThrow(() -> new ResourceNotFoundException("Dropoff branch not found: " + dropoffBranchId));
        // ──────────────────────────────────────────────────────────────────────────────

        // ── Calculate total rental cost ────────────────────────────────────────────────
        long rentalDays = ChronoUnit.DAYS.between(startDate, endDate);  // number of rental days
        BigDecimal totalAmount = car.getDailyRate()
                .multiply(BigDecimal.valueOf(rentalDays));               // dailyRate × days
        // ──────────────────────────────────────────────────────────────────────────────

        // ── Build rental reference (idempotency key) ───────────────────────────────────
        // Format: "CR-" + first 8 chars of random UUID – human-readable booking reference
        String rentalReference = "CR-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        // ──────────────────────────────────────────────────────────────────────────────

        // ── Create and persist PENDING rental using BUILDER PATTERN ────────────────────
        Rental rental = Rental.builder()
                .user(user)                          // customer making the booking
                .car(car)                            // car being rented
                .pickupBranch(pickupBranch)          // where customer picks up the car
                .dropoffBranch(dropoffBranch)        // where customer returns the car
                .startDate(startDate)                // rental start
                .endDate(endDate)                    // planned return date
                .status(RentalStatus.PENDING)        // awaiting payment confirmation
                .totalAmount(totalAmount)            // pre-calculated total charge
                .rentalReference(rentalReference)   // unique idempotency key
                .build();

        rental = rentalRepository.save(rental);     // persist to PostgreSQL
        log.info("[RentalService] PENDING rental created. id={}, ref={}", rental.getId(), rentalReference);
        return rental;
    }

    /**
     * Activate a PENDING rental after successful payment.
     * Marks the car as RENTED to prevent further bookings.
     *
     * @param rental the rental to activate
     * @return updated rental with ACTIVE status
     */
    @Transactional
    public Rental activateRental(Rental rental) {
        rental.setStatus(RentalStatus.ACTIVE);  // advance state: PENDING → ACTIVE

        // Mark car as RENTED using optimistic lock – final guard against double-booking
        Car car = carRepository.findByIdWithOptimisticLock(rental.getCar().getId())  // ← OPTIMISTIC LOCKING
                .orElseThrow(() -> new ResourceNotFoundException("Car not found"));
        car.setStatus(CarStatus.RENTED);  // car is now with the customer
        carRepository.save(car);          // @Version incremented by Hibernate on save

        rental = rentalRepository.save(rental);  // persist ACTIVE status
        log.info("[RentalService] Rental ACTIVATED. id={}", rental.getId());
        return rental;
    }

    /**
     * Fetch all rentals for a given user, newest first.
     *
     * @param user authenticated user
     * @return list of the user's rentals in reverse chronological order
     */
    public List<Rental> getRentalHistory(User user) {
        return rentalRepository.findByUserOrderByCreatedAtDesc(user);  // REPOSITORY PATTERN
    }

    /**
     * Get a specific rental by ID, ensuring the requesting user owns it.
     *
     * @param rentalId rental primary key
     * @param user     authenticated user (ownership check)
     * @return Rental entity
     * @throws ResourceNotFoundException if rental doesn't exist or belongs to a different user
     */
    public Rental getRentalByIdForUser(Long rentalId, User user) {
        return rentalRepository.findByIdAndUser(rentalId, user)  // security check
                .orElseThrow(() -> new ResourceNotFoundException("Rental not found: " + rentalId));
    }
}

