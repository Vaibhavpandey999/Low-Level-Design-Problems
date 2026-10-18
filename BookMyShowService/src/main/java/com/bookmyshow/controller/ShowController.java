package com.bookmyshow.controller;

import com.bookmyshow.dto.request.CreateShowRequest;
import com.bookmyshow.dto.response.SeatAvailabilityResponse;
import com.bookmyshow.model.Show;
import com.bookmyshow.service.ShowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * ShowController – Show Scheduling and Seat Availability REST API
 */
@RestController
@RequestMapping("/api/shows")
@RequiredArgsConstructor
@Tag(name = "Shows", description = "Show scheduling and seat availability")
public class ShowController {

    private final ShowService showService;

    /**
     * GET /api/shows?movieId={movieId}&city={city}
     * Get upcoming shows for a movie in a specific city.
     * Public endpoint – no auth required.
     */
    @GetMapping
    @Operation(summary = "Get upcoming shows for a movie in a city")
    public ResponseEntity<List<Show>> getShows(
            @RequestParam Long movieId,    // Movie filter
            @RequestParam String city      // City filter
    ) {
        return ResponseEntity.ok(showService.getUpcomingShows(movieId, city));
    }

    /**
     * GET /api/shows/{showId}/seats
     * Get the seat availability map for a show.
     * Public endpoint – users need this to select seats before login.
     */
    @GetMapping("/{showId}/seats")
    @Operation(summary = "Get seat availability for a show")
    public ResponseEntity<List<SeatAvailabilityResponse>> getSeatAvailability(
            @PathVariable Long showId
    ) {
        return ResponseEntity.ok(showService.getShowSeatAvailability(showId));
    }

    /**
     * POST /api/shows
     * Schedule a new show. Admin only.
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")  // Only admins can schedule shows
    @Operation(summary = "Create a new show (Admin only)")
    public ResponseEntity<Show> createShow(@Valid @RequestBody CreateShowRequest request) {
        Show show = showService.createShow(request);  // Validates, creates show + seats
        return ResponseEntity.status(HttpStatus.CREATED).body(show);
    }
}

