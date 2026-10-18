package com.bookmyshow.controller;

import com.bookmyshow.model.Theatre;
import com.bookmyshow.repository.TheatreRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * TheatreController – Theatre information REST API.
 * Public endpoints – no auth required.
 */
@RestController
@RequestMapping("/api/theatres")
@RequiredArgsConstructor
@Tag(name = "Theatres", description = "Theatre and cinema information")
public class TheatreController {

    private final TheatreRepository theatreRepository;  // Direct repo access (thin controller)

    /**
     * GET /api/theatres?city={city}
     * List all active theatres in a city.
     * Used for the "Select City → Browse Theatres" feature.
     */
    @GetMapping
    @Operation(summary = "Get theatres by city")
    public ResponseEntity<List<Theatre>> getTheatresByCity(@RequestParam String city) {
        return ResponseEntity.ok(theatreRepository.findByCityAndIsActiveTrue(city));
    }

    /**
     * GET /api/theatres/{id}
     * Get theatre details by ID.
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get theatre by ID")
    public ResponseEntity<Theatre> getTheatreById(@PathVariable Long id) {
        return ResponseEntity.of(theatreRepository.findById(id));  // Returns 404 if not found
    }
}

