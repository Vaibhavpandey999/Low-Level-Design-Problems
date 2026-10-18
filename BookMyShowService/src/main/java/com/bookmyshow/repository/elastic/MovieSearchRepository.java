package com.bookmyshow.repository.elastic;

import com.bookmyshow.model.elastic.MovieDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * MovieSearchRepository – Elasticsearch repository for MovieDocument.
 *
 * Extends ElasticsearchRepository which provides:
 *   save(), findById(), findAll(), delete(), search(), etc.
 *
 * Spring Data ES auto-generates query implementations from method names,
 * just like Spring Data JPA does for SQL.
 *
 * DESIGN PATTERN: Repository Pattern (same abstraction, different backing store)
 * DESIGN PATTERN: CQRS – this is the "Query" side for movie search
 */
@Repository
public interface MovieSearchRepository extends ElasticsearchRepository<MovieDocument, String> {

    /**
     * Full-text search on title field in Elasticsearch.
     * ES derived query: { "match": { "title": "query" } }
     */
    List<MovieDocument> findByTitleContaining(String title);

    /**
     * Filter movies by genre (exact keyword match).
     * ES derived query: { "term": { "genre": "ACTION" } }
     */
    List<MovieDocument> findByGenre(String genre);

    /**
     * Filter movies by language (exact keyword match).
     * ES derived query: { "term": { "language": "Hindi" } }
     */
    List<MovieDocument> findByLanguage(String language);

    /**
     * Find high-rated movies (rating above threshold).
     * ES derived query: { "range": { "rating": { "gte": minRating } } }
     */
    List<MovieDocument> findByRatingGreaterThanEqual(Double minRating);
}

