package com.bookmyshow.service;

import com.bookmyshow.dto.request.CreateShowRequest;
import com.bookmyshow.dto.response.SeatAvailabilityResponse;
import com.bookmyshow.enums.SeatStatus;
import com.bookmyshow.exception.ResourceNotFoundException;
import com.bookmyshow.model.*;
import com.bookmyshow.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * ============================================================
 * ShowService – Show Scheduling and Seat Availability
 * ============================================================
 *
 * Handles:
 * 1. Creating new shows (admin)
 * 2. Querying upcoming shows by movie + city
 * 3. Returning seat availability map for a show
 *
 * DESIGN PATTERN: Facade – abstracts Show + ShowSeat creation.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ShowService {

    private final ShowRepository showRepository;
    private final ScreenRepository screenRepository;
    private final MovieRepository movieRepository;
    private final SeatRepository seatRepository;
    private final ShowSeatRepository showSeatRepository;

    /**
     * Create a new Show and auto-generate ShowSeat rows.
     *
     * Steps:
     * 1. Validate Movie and Screen exist
     * 2. Calculate end time (startTime + movie duration)
     * 3. Check for scheduling conflicts on the same screen
     * 4. Save Show
     * 5. Auto-create ShowSeat rows for every seat in the screen
     *
     * DESIGN PATTERN: Template Method – fixed steps that must
     *   occur in sequence.
     *
     * @param request Show creation details
     * @return Created Show
     */
    @Transactional  // All DB writes atomic: if ShowSeat creation fails, Show is also rolled back
    public Show createShow(CreateShowRequest request) {
        // STEP 1a: Validate movie exists and is active
        Movie movie = movieRepository.findById(request.getMovieId())
                .orElseThrow(() -> new ResourceNotFoundException("Movie", request.getMovieId()));

        // STEP 1b: Validate screen exists and is active
        Screen screen = screenRepository.findById(request.getScreenId())
                .orElseThrow(() -> new ResourceNotFoundException("Screen", request.getScreenId()));

        // STEP 2: Calculate end time = startTime + durationMinutes
        LocalDateTime endTime = request.getStartTime()
                .plusMinutes(movie.getDurationMinutes());  // Add movie duration

        // STEP 3: Check for scheduling conflicts
        // A screen cannot host two overlapping shows
        List<Show> existingShows = showRepository
                .findByScreenIdAndStartTimeGreaterThanEqualAndIsActiveTrue(
                        screen.getId(),
                        request.getStartTime().minusHours(4)  // Check 4h window before
                );
        for (Show existing : existingShows) {
            // Check if time ranges overlap: new show starts before existing ends
            // AND new show ends after existing starts
            boolean overlaps = request.getStartTime().isBefore(existing.getEndTime())
                    && endTime.isAfter(existing.getStartTime());
            if (overlaps) {
                throw new IllegalArgumentException(
                    "Scheduling conflict: Screen " + screen.getName() +
                    " is already booked from " + existing.getStartTime() +
                    " to " + existing.getEndTime()
                );
            }
        }

        // STEP 4: Build and save the Show
        Show show = Show.builder()
                .movie(movie)                      // Link to movie
                .screen(screen)                    // Link to screen
                .startTime(request.getStartTime()) // Set show start
                .endTime(endTime)                  // Set calculated end time
                .isActive(true)                    // Show is active
                .build();

        Show savedShow = showRepository.save(show);  // Persist show to DB
        log.info("Show created: id={} movie={} screen={} start={}",
                savedShow.getId(), movie.getTitle(), screen.getName(), savedShow.getStartTime());

        // STEP 5: Auto-create ShowSeat for EVERY seat in the screen
        // This creates the per-show seat inventory
        List<Seat> screenSeats = seatRepository.findByScreenId(screen.getId());  // All physical seats

        // Build ShowSeat objects for each physical seat
        List<ShowSeat> showSeats = screenSeats.stream()
                .map(seat -> ShowSeat.builder()
                        .show(savedShow)               // Link to this show
                        .seat(seat)                    // Link to physical seat
                        .status(SeatStatus.AVAILABLE)  // All seats start available
                        .build()
                )
                .collect(Collectors.toList());

        showSeatRepository.saveAll(showSeats);  // Bulk insert all ShowSeat rows
        log.info("Created {} ShowSeat rows for showId={}", showSeats.size(), savedShow.getId());

        return savedShow;
    }

    /**
     * Get upcoming shows for a movie in a city.
     * Used for the "show timings" page after a user selects a movie.
     *
     * @param movieId  Movie DB ID
     * @param city     City name (e.g. "Mumbai")
     * @return List of upcoming Shows
     */
    public List<Show> getUpcomingShows(Long movieId, String city) {
        LocalDateTime now = LocalDateTime.now();  // Filter out past shows
        return showRepository.findUpcomingShowsByMovieAndCity(movieId, city, now);
    }

    /**
     * Get seat availability map for a specific show.
     * Returns the status of every seat (AVAILABLE / LOCKED / BOOKED).
     * This data powers the interactive seat selection UI.
     *
     * @param showId Show DB ID
     * @return List of seat availability info
     */
    public List<SeatAvailabilityResponse> getShowSeatAvailability(Long showId) {
        // Validate show exists
        if (!showRepository.existsById(showId)) {
            throw new ResourceNotFoundException("Show", showId);
        }

        // Fetch all ShowSeats for this show
        List<ShowSeat> showSeats = showSeatRepository.findByShowId(showId);

        // Map each ShowSeat → SeatAvailabilityResponse DTO
        return showSeats.stream()
                .map(ss -> SeatAvailabilityResponse.builder()
                        .showSeatId(ss.getId())                        // ID to send in BookingRequest
                        .rowNumber(ss.getSeat().getRowNumber())         // e.g. "A"
                        .seatNumber(ss.getSeat().getSeatNumber())       // e.g. 5
                        .seatType(ss.getSeat().getSeatType())           // NORMAL/PREMIUM/RECLINER
                        .status(ss.getStatus())                         // AVAILABLE/LOCKED/BOOKED
                        .price(ss.getSeat().getBasePrice())             // Base price for this seat
                        .build()
                )
                .collect(Collectors.toList());
    }
}

