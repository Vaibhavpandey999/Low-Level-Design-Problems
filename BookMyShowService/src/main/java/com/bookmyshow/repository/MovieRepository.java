package com.bookmyshow.repository;

import com.bookmyshow.enums.Genre;
import com.bookmyshow.model.Movie;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * MovieRepository – JPA repository for Movie entity.
 *
 * DESIGN PATTERN: Repository Pattern – data access abstraction.
 */
@Repository
public interface MovieRepository extends JpaRepository<Movie, Long> {

    /**
     * Find all active movies by genre.
     * Derived query: SELECT * FROM movies WHERE genre = ? AND is_active = true
     */
    List<Movie> findByGenreAndIsActiveTrue(Genre genre);

    /**
     * Find all active movies by language.
     * Derived query: SELECT * FROM movies WHERE language = ? AND is_active = true
     */
    List<Movie> findByLanguageAndIsActiveTrue(String language);

    /**
     * Full-text LIKE search on title in PostgreSQL.
     * @Query lets us write JPQL (JPA Query Language) directly.
     * LOWER() + LIKE makes it case-insensitive.
     *
     * NOTE: For production-scale search, use MovieSearchRepository (Elasticsearch).
     * This query is used as a fallback or admin utility.
     */
    @Query("SELECT m FROM Movie m WHERE LOWER(m.title) LIKE LOWER(CONCAT('%', :keyword, '%')) AND m.isActive = true")
    List<Movie> searchByTitleContaining(@Param("keyword") String keyword);

    /**
     * Find all active (non-deleted) movies.
     * Derived: SELECT * FROM movies WHERE is_active = true
     */
    List<Movie> findByIsActiveTrue();
}

