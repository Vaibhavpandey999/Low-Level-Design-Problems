package com.bookmyshow.service;

import com.bookmyshow.dto.request.BookingRequest;
import com.bookmyshow.dto.response.BookingResponse;
import com.bookmyshow.enums.BookingStatus;
import com.bookmyshow.enums.PaymentStatus;
import com.bookmyshow.enums.SeatStatus;
import com.bookmyshow.exception.PaymentFailedException;
import com.bookmyshow.exception.ResourceNotFoundException;
import com.bookmyshow.kafka.BookingEventProducer;
import com.bookmyshow.kafka.events.BookingCancelledEvent;
import com.bookmyshow.kafka.events.BookingConfirmedEvent;
import com.bookmyshow.model.*;
import com.bookmyshow.observer.BookingEventPublisher;
import com.bookmyshow.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * ============================================================
 * BookingService – Core Booking Orchestrator
 * ============================================================
 *
 * This is the most important service in the system.
 * It orchestrates the complete booking lifecycle.
 *
 * DESIGN PATTERN: Facade – single entry point that hides the
 *   complexity of seat locking, payment, notifications, and
 *   Kafka events from the controller.
 *
 * DESIGN PATTERN: Template Method – book() follows a fixed
 *   sequence of steps:
 *   1. Validate seats
 *   2. Lock seats (Redis)
 *   3. Create Booking (PENDING)
 *   4. Process Payment (Strategy via PaymentService)
 *   5. If SUCCESS: confirm → publish Kafka → notify Observers
 *   6. If FAILED: release locks → cancel booking
 *
 * DESIGN PATTERN: Observer – BookingEventPublisher notifies all
 *   registered observers (Email, SMS, Analytics) on confirmation.
 *
 * DESIGN PATTERN: Optimistic Locking – ShowSeat has @Version;
 *   concurrent updates fail fast rather than silently overwrite.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final ShowRepository showRepository;
    private final ShowSeatRepository showSeatRepository;
    private final UserRepository userRepository;
    private final PaymentRepository paymentRepository;

    private final SeatLockService seatLockService;        // Redis seat locking
    private final PaymentService paymentService;          // Strategy-based payment
    private final BookingEventPublisher eventPublisher;   // Observer pattern publisher
    private final BookingEventProducer kafkaProducer;     // Kafka event publisher

    /**
     * ============================================================
     * TEMPLATE METHOD: book() – The Booking Lifecycle
     * ============================================================
     *
     * This method defines the algorithm skeleton for booking.
     * Each step MUST execute in order; if any step fails, we
     * roll back appropriately.
     *
     * @param userId  Authenticated user's ID
     * @param request BookingRequest with showId, seatIds, and gateway
     * @return BookingResponse with confirmation details
     */
    @Transactional  // Outer transaction: all DB writes succeed or all roll back
    public BookingResponse book(Long userId, BookingRequest request) {
        log.info("[Booking] Starting booking for userId={} showId={} seats={}",
                userId, request.getShowId(), request.getShowSeatIds());

        // ── STEP 1: VALIDATE ────────────────────────────────────────────────
        // Load the Show entity (validates it exists)
        Show show = showRepository.findById(request.getShowId())
                .orElseThrow(() -> new ResourceNotFoundException("Show", request.getShowId()));

        // Load the User entity
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        // Load all requested ShowSeat entities
        List<ShowSeat> showSeats = showSeatRepository.findByIdIn(request.getShowSeatIds());

        // Verify all requested seats belong to the requested show
        for (ShowSeat ss : showSeats) {
            if (!ss.getShow().getId().equals(request.getShowId())) {
                throw new IllegalArgumentException(
                    "ShowSeat " + ss.getId() + " does not belong to Show " + request.getShowId()
                );
            }
            // Verify seat is not already BOOKED
            if (ss.getStatus() == SeatStatus.BOOKED) {
                throw new com.bookmyshow.exception.SeatAlreadyLockedException(
                    "Seat " + ss.getSeat().getRowNumber() + ss.getSeat().getSeatNumber() +
                    " is already booked."
                );
            }
        }

        // ── STEP 2: LOCK SEATS IN REDIS ────────────────────────────────────
        // Lock each seat atomically in Redis to prevent double-booking
        // If any lock fails (concurrent user grabbed it), throw exception
        for (ShowSeat ss : showSeats) {
            seatLockService.lockSeat(ss.getId(), userId);  // Redis SETNX atomic lock
        }

        // ── STEP 3: CALCULATE TOTAL AND CREATE PENDING BOOKING ────────────
        // Sum up the price of all selected seats
        double totalAmount = showSeats.stream()
                .mapToDouble(ss -> ss.getSeat().getBasePrice())  // Each seat's base price
                .sum();

        // Generate a human-readable booking reference
        String bookingRef = "BMS-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        // Build Booking entity in PENDING state
        Booking booking = Booking.builder()
                .user(user)                           // The booking user
                .show(show)                           // The show being booked
                .status(BookingStatus.PENDING)        // Initial state: awaiting payment
                .totalAmount(totalAmount)             // Pre-calculated total
                .bookingReference(bookingRef)         // Unique reference number
                .build();

        Booking savedBooking = bookingRepository.save(booking);  // Persist PENDING booking

        // Create BookingItems (one per seat)
        List<BookingItem> bookingItems = showSeats.stream()
                .map(ss -> BookingItem.builder()
                        .booking(savedBooking)           // Link to parent booking
                        .showSeat(ss)                    // The specific seat
                        .price(ss.getSeat().getBasePrice())  // Price snapshot at booking time
                        .build()
                )
                .collect(Collectors.toList());

        // Note: BookingItems saved via CascadeType.ALL when we set them on booking
        savedBooking.setBookingItems(bookingItems);
        bookingRepository.save(savedBooking);  // Save with items

        log.info("[Booking] PENDING booking created: id={} ref={} amount={}",
                savedBooking.getId(), bookingRef, totalAmount);

        // ── STEP 4: PROCESS PAYMENT VIA STRATEGY PATTERN ──────────────────
        // Build initial Payment entity
        Payment payment = Payment.builder()
                .booking(savedBooking)              // Link to booking
                .amount(totalAmount)                // Amount to charge
                .gateway(request.getPaymentGateway())  // Which gateway to use
                .build();

        Payment savedPayment = paymentRepository.save(payment);  // Persist INITIATED payment

        // Delegate to PaymentService which uses the Strategy+Factory pattern
        Payment processedPayment = paymentService.processPayment(savedBooking, savedPayment);

        // ── STEP 5a: PAYMENT SUCCESS ────────────────────────────────────────
        if (processedPayment.getStatus() == PaymentStatus.SUCCESS) {
            // Update ShowSeat statuses from LOCKED → BOOKED (optimistic lock via @Version)
            List<Long> seatIds = showSeats.stream().map(ShowSeat::getId).collect(Collectors.toList());
            showSeatRepository.updateStatusByIds(seatIds, SeatStatus.BOOKED);  // Bulk UPDATE

            // Confirm the booking
            savedBooking.setStatus(BookingStatus.CONFIRMED);  // Transition PENDING → CONFIRMED
            bookingRepository.save(savedBooking);              // Persist confirmed status

            log.info("[Booking] CONFIRMED: id={} ref={}", savedBooking.getId(), bookingRef);

            // OBSERVER PATTERN: Notify all registered observers (Email, SMS, Analytics)
            // Observers run @Async in their own thread pools
            eventPublisher.publishBookingConfirmed(savedBooking);

            // Publish to Kafka for downstream consumers (duplicate notification path for reliability)
            BookingConfirmedEvent kafkaEvent = BookingConfirmedEvent.builder()
                    .bookingId(savedBooking.getId())
                    .bookingReference(bookingRef)
                    .userId(user.getId())
                    .userEmail(user.getEmail())
                    .userPhone(user.getPhoneNumber())
                    .showId(show.getId())
                    .movieTitle(show.getMovie().getTitle())
                    .totalAmount(totalAmount)
                    .confirmedAt(LocalDateTime.now())
                    .build();

            kafkaProducer.publishBookingConfirmed(kafkaEvent);  // Async Kafka publish

            // Return success response DTO
            return buildBookingResponse(savedBooking, showSeats);

        } else {
            // ── STEP 5b: PAYMENT FAILED ─────────────────────────────────────
            log.error("[Booking] Payment failed for bookingId={}", savedBooking.getId());

            // Release Redis seat locks so other users can book these seats
            seatLockService.releaseAllLocks(
                showSeats.stream().map(ShowSeat::getId).collect(Collectors.toList()),
                userId
            );

            // Mark booking as CANCELLED (payment failed → auto-cancel)
            savedBooking.setStatus(BookingStatus.CANCELLED);
            bookingRepository.save(savedBooking);

            // Observer: notify user of failure
            eventPublisher.publishBookingCancelled(savedBooking);

            throw new PaymentFailedException("Payment processing failed. Please try again.");
        }
    }

    /**
     * Cancel a confirmed booking and initiate refund.
     *
     * @param bookingId Booking to cancel
     * @param userId    Must be the booking owner
     * @return Updated BookingResponse with CANCELLED status
     */
    @Transactional
    public BookingResponse cancelBooking(Long bookingId, Long userId) {
        // Load booking and validate ownership
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", bookingId));

        // Security: ensure user can only cancel their own bookings
        if (!booking.getUser().getId().equals(userId)) {
            throw new SecurityException("Not authorized to cancel booking: " + bookingId);
        }

        // Can only cancel CONFIRMED bookings
        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new IllegalStateException(
                "Cannot cancel booking with status: " + booking.getStatus()
            );
        }

        // Process refund via PaymentService (Strategy pattern)
        paymentService.processRefund(bookingId);

        // Release the seats back to AVAILABLE
        List<Long> seatIds = booking.getBookingItems().stream()
                .map(item -> item.getShowSeat().getId())
                .collect(Collectors.toList());
        showSeatRepository.updateStatusByIds(seatIds, SeatStatus.AVAILABLE);  // BOOKED → AVAILABLE

        // Update booking status
        booking.setStatus(BookingStatus.CANCELLED);
        Booking updated = bookingRepository.save(booking);

        // OBSERVER PATTERN: Notify about cancellation
        eventPublisher.publishBookingCancelled(updated);

        // Kafka: publish cancellation event
        BookingCancelledEvent kafkaEvent = BookingCancelledEvent.builder()
                .bookingId(updated.getId())
                .bookingReference(updated.getBookingReference())
                .userId(userId)
                .userEmail(updated.getUser().getEmail())
                .refundAmount(updated.getTotalAmount())
                .cancelledAt(LocalDateTime.now())
                .build();
        kafkaProducer.publishBookingCancelled(kafkaEvent);

        log.info("[Booking] CANCELLED: id={} ref={}", bookingId, booking.getBookingReference());

        return buildBookingResponse(updated, booking.getBookingItems().stream()
                .map(BookingItem::getShowSeat).collect(Collectors.toList()));
    }

    /**
     * Get all bookings for a user (booking history).
     */
    public List<BookingResponse> getUserBookings(Long userId) {
        return bookingRepository.findByUserId(userId).stream()
                .map(b -> buildBookingResponse(b,
                        b.getBookingItems().stream()
                                .map(BookingItem::getShowSeat)
                                .collect(Collectors.toList())))
                .collect(Collectors.toList());
    }

    /**
     * Get booking by ID (for ticket display / QR scan).
     */
    public BookingResponse getBookingById(Long bookingId, Long userId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", bookingId));
        // Security: only allow owner to view
        if (!booking.getUser().getId().equals(userId)) {
            throw new SecurityException("Not authorized to view booking: " + bookingId);
        }
        return buildBookingResponse(booking, booking.getBookingItems().stream()
                .map(BookingItem::getShowSeat).collect(Collectors.toList()));
    }

    /**
     * Helper: build BookingResponse DTO from entity.
     * Extracts and formats all relevant fields for the API response.
     */
    private BookingResponse buildBookingResponse(Booking booking, List<ShowSeat> showSeats) {
        // Build list of seat labels (e.g. "A1", "A2", "B5")
        List<String> seatLabels = showSeats.stream()
                .map(ss -> ss.getSeat().getRowNumber() + ss.getSeat().getSeatNumber())  // e.g. "A" + "3" = "A3"
                .collect(Collectors.toList());

        return BookingResponse.builder()
                .bookingId(booking.getId())
                .bookingReference(booking.getBookingReference())
                .status(booking.getStatus())
                .movieTitle(booking.getShow().getMovie().getTitle())
                .theatreName(booking.getShow().getScreen().getTheatre().getName())
                .screenName(booking.getShow().getScreen().getName())
                .showStartTime(booking.getShow().getStartTime())
                .seats(seatLabels)
                .totalAmount(booking.getTotalAmount())
                .bookedAt(booking.getCreatedAt())
                .build();
    }
}

