package com.bookmyshow.controller;

import com.bookmyshow.model.elastic.MovieDocument;
import com.bookmyshow.repository.elastic.MovieSearchRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * ============================================================
 * SearchController – Elasticsearch-Powered Movie Search
 * ============================================================
 *
 * Public endpoint – no auth required.
 * Routes search queries to Elasticsearch for fast full-text search.
 *
 * DESIGN PATTERN: CQRS – this controller handles the "Query" side
 *   of movies (reads from Elasticsearch), while MovieController
 *   handles the "Command" side (writes to PostgreSQL).
 */
@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
@Tag(name = "Search", description = "Elasticsearch-powered movie search")
public class SearchController {

    private final MovieSearchRepository movieSearchRepository;  // ES repository

    /**
     * GET /api/search/movies?q={query}
     * Full-text search movies by title keyword.
     * Returns results ranked by relevance (ES scores).
     *
     * Example: GET /api/search/movies?q=avengers
     */
    @GetMapping("/movies")
    @Operation(summary = "Search movies by title (Elasticsearch full-text)")
    public ResponseEntity<List<MovieDocument>> searchMovies(
            @RequestParam String q  // Search query parameter
    ) {
        // Elasticsearch MATCH query on 'title' field
        return ResponseEntity.ok(movieSearchRepository.findByTitleContaining(q));
    }

    /**
     * GET /api/search/movies/genre/{genre}
     * Filter movies by genre (exact match on keyword field).
     *
     * Example: GET /api/search/movies/genre/ACTION
     */
    @GetMapping("/movies/genre/{genre}")
    @Operation(summary = "Filter movies by genre")
    public ResponseEntity<List<MovieDocument>> searchByGenre(@PathVariable String genre) {
        return ResponseEntity.ok(movieSearchRepository.findByGenre(genre.toUpperCase()));
    }

    /**
     * GET /api/search/movies/language/{language}
     * Filter movies by language.
     *
     * Example: GET /api/search/movies/language/Hindi
     */
    @GetMapping("/movies/language/{language}")
    @Operation(summary = "Filter movies by language")
    public ResponseEntity<List<MovieDocument>> searchByLanguage(@PathVariable String language) {
        return ResponseEntity.ok(movieSearchRepository.findByLanguage(language));
    }

    /**
     * GET /api/search/movies/top-rated?minRating={rating}
     * Find highly-rated movies (range query).
     *
     * Example: GET /api/search/movies/top-rated?minRating=8.0
     */
    @GetMapping("/movies/top-rated")
    @Operation(summary = "Get movies with rating above threshold")
    public ResponseEntity<List<MovieDocument>> getTopRated(
            @RequestParam(defaultValue = "7.0") Double minRating  // Default: 7.0+
    ) {
        return ResponseEntity.ok(movieSearchRepository.findByRatingGreaterThanEqual(minRating));
    }
}

