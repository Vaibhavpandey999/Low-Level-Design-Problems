package com.bookmyshow.controller;

import com.bookmyshow.dto.request.CreateMovieRequest;
import com.bookmyshow.model.Movie;
import com.bookmyshow.service.MovieService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * ============================================================
 * MovieController – Movie Catalog REST API
 * ============================================================
 *
 * Public endpoints (GET): no JWT required.
 * Admin endpoints (POST, DELETE): requires ROLE_ADMIN.
 */
@RestController
@RequestMapping("/api/movies")
@RequiredArgsConstructor
@Tag(name = "Movies", description = "Movie catalog management")
public class MovieController {

    private final MovieService movieService;

    /**
     * GET /api/movies
     * List all active movies. Public endpoint.
     */
    @GetMapping
    @Operation(summary = "Get all active movies")
    public ResponseEntity<List<Movie>> getAllMovies() {
        return ResponseEntity.ok(movieService.getAllActiveMovies());  // From cache or DB
    }

    /**
     * GET /api/movies/{id}
     * Get a specific movie by ID. Public endpoint.
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get movie by ID")
    public ResponseEntity<Movie> getMovieById(@PathVariable Long id) {
        return ResponseEntity.ok(movieService.getMovieById(id));  // Cached in Redis
    }

    /**
     * POST /api/movies
     * Create a new movie. Admin only.
     *
     * @PreAuthorize: Spring Security method-level auth check.
     *   Only users with ROLE_ADMIN can call this.
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")  // DESIGN PATTERN: Method-level security
    @SecurityRequirement(name = "bearerAuth")  // Swagger: show lock icon on this endpoint
    @Operation(summary = "Create a new movie (Admin only)")
    public ResponseEntity<Movie> createMovie(@Valid @RequestBody CreateMovieRequest request) {
        Movie movie = movieService.createMovie(request);  // Saves to PostgreSQL + Elasticsearch
        return ResponseEntity.status(HttpStatus.CREATED).body(movie);
    }

    /**
     * DELETE /api/movies/{id}
     * Soft-delete a movie (set isActive = false). Admin only.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Deactivate a movie (Admin only)")
    public ResponseEntity<Movie> deactivateMovie(@PathVariable Long id) {
        return ResponseEntity.ok(movieService.deactivateMovie(id));
    }
}

