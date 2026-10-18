package com.bookmyshow.controller;

import com.bookmyshow.dto.request.BookingRequest;
import com.bookmyshow.dto.response.BookingResponse;
import com.bookmyshow.model.User;
import com.bookmyshow.service.BookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * ============================================================
 * BookingController – Booking Lifecycle REST API
 * ============================================================
 *
 * All endpoints require JWT authentication.
 * Uses @AuthenticationPrincipal to extract the currently
 * authenticated user from the SecurityContext.
 *
 * DESIGN PATTERN: Facade – delegates all work to BookingService;
 *   controller is thin (validation + HTTP status only).
 */
@Slf4j
@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")  // All endpoints require JWT (shown in Swagger)
@Tag(name = "Bookings", description = "Movie ticket booking and cancellation")
public class BookingController {

    private final BookingService bookingService;  // Facade service

    /**
     * POST /api/bookings
     * Create a new booking (lock seats + process payment).
     *
     * @AuthenticationPrincipal: Spring Security injects the authenticated User
     *   from the SecurityContext. Our User implements UserDetails so it's
     *   returned directly without casting issues.
     *
     * Returns 201 CREATED with BookingResponse on success.
     */
    @PostMapping
    @Operation(summary = "Create a new booking", description =
        "Locks selected seats, processes payment, and confirms booking. " +
        "Uses Strategy pattern for payment gateway selection.")
    public ResponseEntity<BookingResponse> createBooking(
            @AuthenticationPrincipal User currentUser,          // Authenticated user from JWT
            @Valid @RequestBody BookingRequest request           // Validated booking request
    ) {
        log.info("Booking request from userId={} for showId={}", currentUser.getId(), request.getShowId());

        // Delegate entire booking flow to BookingService (Template Method + Facade)
        BookingResponse response = bookingService.book(currentUser.getId(), request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);  // 201 Created
    }

    /**
     * DELETE /api/bookings/{bookingId}
     * Cancel a confirmed booking and process refund.
     */
    @DeleteMapping("/{bookingId}")
    @Operation(summary = "Cancel a booking", description =
        "Cancels a confirmed booking, processes refund, and releases seats.")
    public ResponseEntity<BookingResponse> cancelBooking(
            @AuthenticationPrincipal User currentUser,  // Authenticated user
            @PathVariable Long bookingId                // Booking to cancel
    ) {
        log.info("Cancel request from userId={} for bookingId={}", currentUser.getId(), bookingId);
        BookingResponse response = bookingService.cancelBooking(bookingId, currentUser.getId());
        return ResponseEntity.ok(response);  // 200 OK
    }

    /**
     * GET /api/bookings
     * Get all bookings for the authenticated user (booking history).
     */
    @GetMapping
    @Operation(summary = "Get my booking history")
    public ResponseEntity<List<BookingResponse>> getMyBookings(
            @AuthenticationPrincipal User currentUser
    ) {
        return ResponseEntity.ok(bookingService.getUserBookings(currentUser.getId()));
    }

    /**
     * GET /api/bookings/{bookingId}
     * Get a specific booking by ID (for ticket display / QR scan).
     */
    @GetMapping("/{bookingId}")
    @Operation(summary = "Get booking details by ID")
    public ResponseEntity<BookingResponse> getBooking(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long bookingId
    ) {
        return ResponseEntity.ok(bookingService.getBookingById(bookingId, currentUser.getId()));
    }
}

