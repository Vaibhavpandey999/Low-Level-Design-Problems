package com.bookmyshow.model;

import com.bookmyshow.enums.Genre;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;

/**
 * ============================================================
 * Movie Entity – maps to 'movies' table in PostgreSQL
 * ============================================================
 *
 * A Movie represents the content catalog. It is linked to
 * Show instances which schedule it in specific screens at
 * specific times.
 *
 * DESIGN PATTERN: Builder (Lombok @Builder) – construct Movie
 *   objects fluently in tests and service layer.
 */
@Entity                                  // JPA entity
@Table(name = "movies")                  // PostgreSQL table name
@Data                                    // Lombok: getters/setters/equals/hashCode/toString
@Builder                                 // DESIGN PATTERN: Builder
@NoArgsConstructor                       // JPA requires no-arg constructor
@AllArgsConstructor                      // Required by @Builder
public class Movie {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)  // Auto-increment PK
    private Long id;

    @Column(nullable = false)
    private String title;                // Movie title (e.g. "Avengers: Endgame")

    @Column(columnDefinition = "TEXT")   // Large text field for synopsis
    private String description;          // Plot synopsis / short description

    @Column(nullable = false)
    private Integer durationMinutes;     // Running time in minutes (e.g. 180)

    @Enumerated(EnumType.STRING)         // Store genre name as string (not ordinal) for readability
    @Column(nullable = false)
    private Genre genre;                 // Movie genre (ACTION, COMEDY, etc.)

    @Column(nullable = false)
    private String language;             // Original language (e.g. "English", "Hindi")

    @Column(nullable = false)
    private String certification;        // Rating: "U", "UA", "A", "S"

    private String posterUrl;            // CDN URL for movie poster image

    private String trailerUrl;           // YouTube / CDN URL for trailer

    @ElementCollection                   // Maps to a separate 'movie_cast' join table
    @CollectionTable(name = "movie_cast", joinColumns = @JoinColumn(name = "movie_id"))
    @Column(name = "actor_name")
    private List<String> cast;           // List of actor/actress names

    @Column(nullable = false)
    @Builder.Default
    private Double rating = 0.0;         // Average user rating (0.0 – 10.0)

    @Column(nullable = false)
    @Builder.Default
    private Boolean isActive = true;     // Soft-delete flag: false = not listed

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;     // Auto-set on first INSERT

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;     // Auto-updated on every UPDATE
}

