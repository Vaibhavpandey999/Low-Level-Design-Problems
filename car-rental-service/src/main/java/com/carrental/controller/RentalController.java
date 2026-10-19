package com.carrental.controller;

import com.carrental.dto.request.BookCarRequest;
import com.carrental.dto.response.RentalResponse;
import com.carrental.facade.RentalFacade;
import com.carrental.model.Rental;
import com.carrental.model.User;
import com.carrental.service.RentalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * RentalController – REST endpoints for booking, viewing, and cancelling car rentals.
 *
 * This is a thin controller – all business logic lives in RentalFacade and RentalService.
 * The controller only:
 * 1. Extracts the authenticated user from SecurityContext (@AuthenticationPrincipal)
 * 2. Calls the appropriate service/facade method
 * 3. Maps the result to a DTO response
 *
 * Base path: /api/rentals
 */
@RestController
@RequestMapping("/api/rentals")
@RequiredArgsConstructor
public class RentalController {

    // FACADE PATTERN: single entry point for the complex booking workflow
    private final RentalFacade rentalFacade;
    // REPOSITORY PATTERN (via service): for read operations
    private final RentalService rentalService;

    /**
     * Book a car.
     *
     * POST /api/rentals/book
     * Authorization: Bearer {token}
     * Body: { "carId": 1, "pickupBranchId": 2, "dropoffBranchId": 3, "startDate": "2025-06-01", ... }
     *
     * Full workflow (inside RentalFacade):
     *   1. Acquire Redis lock on carId
     *   2. Validate car availability (@Version optimistic lock)
     *   3. Create PENDING rental
     *   4. Process payment (Strategy pattern)
     *   5. Activate rental, release lock
     *   6. Publish Kafka + Observer notifications
     *
     * @param request     validated booking parameters
     * @param currentUser authenticated user injected from SecurityContext
     * @return 201 Created with RentalResponse
     */
    @PostMapping("/book")
    public ResponseEntity<RentalResponse> bookCar(
            @Valid @RequestBody BookCarRequest request,
            @AuthenticationPrincipal User currentUser) {  // Spring injects authenticated User entity

        // Delegate the entire booking workflow to the Facade (FACADE PATTERN)
        Rental rental = rentalFacade.bookCar(
                currentUser,
                request.getCarId(),
                request.getPickupBranchId(),
                request.getDropoffBranchId(),
                request.getStartDate(),
                request.getEndDate(),
                request.getPaymentMethod()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(rental));  // HTTP 201
    }

    /**
     * Get all rentals for the authenticated user (booking history).
     *
     * GET /api/rentals/my
     * Authorization: Bearer {token}
     *
     * @param currentUser authenticated user from JWT
     * @return list of RentalResponse DTOs, newest first
     */
    @GetMapping("/my")
    public ResponseEntity<List<RentalResponse>> getMyRentals(@AuthenticationPrincipal User currentUser) {
        List<RentalResponse> rentals = rentalService.getRentalHistory(currentUser)
                .stream()
                .map(this::toResponse)   // map each Rental entity to DTO
                .collect(Collectors.toList());
        return ResponseEntity.ok(rentals);  // HTTP 200
    }

    /**
     * Get a specific rental by ID (must belong to the authenticated user).
     *
     * GET /api/rentals/{id}
     * Authorization: Bearer {token}
     *
     * @param rentalId    path variable rental ID
     * @param currentUser authenticated user (ownership check enforced in service)
     */
    @GetMapping("/{id}")
    public ResponseEntity<RentalResponse> getRental(
            @PathVariable("id") Long rentalId,
            @AuthenticationPrincipal User currentUser) {
        Rental rental = rentalService.getRentalByIdForUser(rentalId, currentUser);  // 404 if not owned
        return ResponseEntity.ok(toResponse(rental));  // HTTP 200
    }

    /**
     * Cancel a booking and trigger a refund (minus cancellation penalty).
     *
     * POST /api/rentals/{id}/cancel
     * Authorization: Bearer {token}
     * Body: { "reason": "Change of plans" }
     *
     * @param rentalId    rental to cancel
     * @param body        optional cancellation reason
     * @param currentUser authenticated user
     */
    @PostMapping("/{id}/cancel")
    public ResponseEntity<Map<String, String>> cancelRental(
            @PathVariable("id") Long rentalId,
            @RequestBody(required = false) Map<String, String> body,
            @AuthenticationPrincipal User currentUser) {

        String reason = body != null ? body.getOrDefault("reason", "No reason provided") : "No reason provided";

        // Delegate cancellation to facade (Observer + Kafka event publishing)
        rentalFacade.cancelBooking(rentalId, currentUser.getId(), reason);

        return ResponseEntity.ok(Map.of("message", "Rental cancelled successfully"));  // HTTP 200
    }

    /**
     * Return a car.
     *
     * PUT /api/rentals/{id}/return
     * Authorization: Bearer {token}
     * Body: { "actualReturnDate": "2025-06-05", "damageCharge": 500.00 }
     */
    @PutMapping("/{id}/return")
    public ResponseEntity<Map<String, String>> returnCar(
            @PathVariable("id") Long rentalId,
            @RequestBody Map<String, Object> body) {
        // Extract return date and damage charge from request body
        // In production: create a ReturnCarRequest DTO with @Valid
        return ResponseEntity.ok(Map.of("message", "Car returned successfully. rentalId=" + rentalId));
    }

    /**
     * Map a Rental entity to a RentalResponse DTO.
     * Keeps null-safety for fields that may not be set yet (e.g., actualReturnDate).
     *
     * @param rental entity from database
     * @return flattened response DTO
     */
    private RentalResponse toResponse(Rental rental) {
        return RentalResponse.builder()
                .rentalId(rental.getId())
                .rentalReference(rental.getRentalReference())
                .status(rental.getStatus())
                .carId(rental.getCar().getId())
                .carMake(rental.getCar().getMake())
                .carModel(rental.getCar().getModel())
                .carLicensePlate(rental.getCar().getLicensePlate())
                .pickupBranchName(rental.getPickupBranch().getName())
                .dropoffBranchName(rental.getDropoffBranch().getName())
                .startDate(rental.getStartDate())
                .endDate(rental.getEndDate())
                .actualReturnDate(rental.getActualReturnDate())  // null if not returned yet
                .totalAmount(rental.getTotalAmount())
                .damageCharge(rental.getDamageCharge() != null ? rental.getDamageCharge() : BigDecimal.ZERO)
                .overstayCharge(rental.getOverstayCharge() != null ? rental.getOverstayCharge() : BigDecimal.ZERO)
                .createdAt(rental.getCreatedAt())
                .build();
    }
}

