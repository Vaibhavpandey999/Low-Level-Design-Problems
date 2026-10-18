package com.bookmyshow.service;

import com.bookmyshow.dto.request.CreateMovieRequest;
import com.bookmyshow.exception.ResourceNotFoundException;
import com.bookmyshow.model.Movie;
import com.bookmyshow.model.elastic.MovieDocument;
import com.bookmyshow.repository.MovieRepository;
import com.bookmyshow.repository.elastic.MovieSearchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * ============================================================
 * MovieService – Movie Catalog Management
 * ============================================================
 *
 * Handles CRUD for movies and keeps Elasticsearch in sync.
 *
 * DESIGN PATTERN: Facade – wraps JPA repo + ES repo operations.
 * DESIGN PATTERN: Proxy (via @Cacheable) – Spring caches results
 *   in Redis to avoid repeated PostgreSQL queries.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MovieService {

    private final MovieRepository movieRepository;          // PostgreSQL
    private final MovieSearchRepository movieSearchRepository;  // Elasticsearch

    /**
     * Create a new movie.
     * Persists to PostgreSQL (source of truth) AND indexes in Elasticsearch (search).
     *
     * @Transactional: ensures both DB saves are in one transaction
     *   (actually ES is not transactional, but JPA write is atomic)
     *
     * @param request Movie creation details
     * @return Created Movie entity
     */
//    Configure Redis in application.properties
//    spring.data.redis.host=localhost
//    spring.data.redis.port=6379
//
//    spring.cache.type=redis
//
//
//    These settings tell Spring Boot to use Redis as the cache provider and connect to Redis on port 6379.
//            4. Enable caching
//    @SpringBootApplication
//    @EnableCaching
//    public class MovieApplication {
//
//        public static void main(String[] args) {
//            SpringApplication.run(
//                    MovieApplication.class, args
//            );
//        }
//    }
//
//
//    @EnableCaching activates Spring's caching annotations. Spring Boot auto-configuration can provide a Redis-backed cache manager when Redis and the cache starter are configured.
//            5. The main Redis cache annotations
//    These annotations are provided by Spring's caching abstraction; they work with Redis when Redis is configured as the cache provider.
//    @Cacheable — store and retrieve cached data
//    @Cacheable(value = "movies", key = "#id")
//    public Movie getMovie(Long id) {
//        return movieRepository.findById(id)
//                .orElseThrow();
//    }
//
//
//- First request: fetches the movie from the database and caches the result.
//            - Later requests for the same ID: return the cached result if it exists.
//    @CachePut — update the cached value
//    @CachePut(value = "movies", key = "#result.id")
//    public Movie updateMovie(Movie movie) {
//        return movieRepository.save(movie);
//    }
//
//
//    This always executes the method and caches the returned movie under its ID. The example assumes the saved movie has a non-null ID.
//    @CacheEvict — remove cached data
//    Remove one movie:
//    @CacheEvict(value = "movies", key = "#id")
//    public void deleteMovie(Long id) {
//        movieRepository.deleteById(id);
//    }
//
//
//    Clear every entry in the movies cache:
//    @CacheEvict(value = "movies", allEntries = true)
//    public void clearMoviesCache() {
//    }
//
//
//    When the second method is invoked through Spring's caching proxy, Spring clears the entire named cache in Redis. The database is unaffected by the cache eviction itself.
//    @CacheConfig — share cache configuration
//    @Service
//    @CacheConfig(cacheNames = "movies")
//    public class MovieService {
//
//        @Cacheable(key = "#id")
//        public Movie getMovie(Long id) {
//            return movieRepository.findById(id)
//                    .orElseThrow();
//        }
//    }
//
//
//    You can specify the cache name once at the class level instead of repeating it on every caching annotation.
//            6. Configure a cache expiration time (TTL)
//    Redis can automatically expire cached entries after a specified duration.
//    For example, set the default cache TTL to 10 minutes:
//    @Configuration
//    public class RedisCacheConfig {
//
//        @Bean
//        RedisCacheConfiguration redisCacheConfiguration() {
//            return RedisCacheConfiguration
//                    .defaultCacheConfig()
//                    .entryTtl(Duration.ofMinutes(10));
//        }
//    }
//
//
//    Required imports include:
//            import java.time.Duration;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.data.redis.cache.RedisCacheConfiguration;
//
//
//    This configures a default TTL of 10 minutes for Spring's Redis caches. Individual cache configurations can use different TTLs if needed.
//            7. What does Redis store?
//    Spring's default Redis cache naming commonly produces keys such as:
//    movies::10
//    movies::20
//    movies::30
//
//
    @Transactional
    @CacheEvict(value = "movies", allEntries = true)  // Invalidate movie list cache on write
    public Movie createMovie(CreateMovieRequest request) {
        // STEP 1: Build Movie entity using Builder pattern
        Movie movie = Movie.builder()
                .title(request.getTitle())                         // Set title
                .description(request.getDescription())             // Set synopsis
                .durationMinutes(request.getDurationMinutes())     // Set runtime
                .genre(request.getGenre())                         // Set genre enum
                .language(request.getLanguage())                   // Set language
                .certification(request.getCertification())         // Set rating certification
                .posterUrl(request.getPosterUrl())                 // Set poster CDN URL
                .trailerUrl(request.getTrailerUrl())               // Set trailer URL
                .cast(request.getCast())                           // Set cast list
                .build();

        // STEP 2: Save to PostgreSQL (primary data store)
        Movie savedMovie = movieRepository.save(movie);
        log.info("Movie created: id={} title={}", savedMovie.getId(), savedMovie.getTitle());

        // STEP 3: Index in Elasticsearch for search
        // Build denormalized MovieDocument (ES doesn't support JOINs like SQL)
        MovieDocument doc = MovieDocument.builder()
                .id(String.valueOf(savedMovie.getId()))            // ES ID = Movie DB ID as string
                .title(savedMovie.getTitle())                      // Searchable title
                .description(savedMovie.getDescription())          // Searchable description
                .genre(savedMovie.getGenre().name())               // Genre keyword for filtering
                .language(savedMovie.getLanguage())                // Language keyword
                .certification(savedMovie.getCertification())      // Certification filter
                .cast(savedMovie.getCast())                        // Cast names for search
                .rating(savedMovie.getRating())                    // Rating for range filter
                .posterUrl(savedMovie.getPosterUrl())              // URL in search results
                .durationMinutes(savedMovie.getDurationMinutes())  // Duration filter
                .build();

        movieSearchRepository.save(doc);  // Index in Elasticsearch
        log.info("Movie indexed in Elasticsearch: id={}", savedMovie.getId());

        return savedMovie;
    }

    /**
     * Get a movie by ID.
     *
     * @Cacheable: Cache result in Redis under key "movies::movieId".
     * On cache hit: return cached Movie without DB query (fast!).
     * On cache miss: hit PostgreSQL, store result in Redis, return result.
     *
     * DESIGN PATTERN: Proxy (caching proxy via Spring AOP)
     *
     * @param id Movie DB ID
     * @return Movie entity
     */
    @Cacheable(value = "movies", key = "#id")   // Cache in Redis: "movies::42"
    public Movie getMovieById(Long id) {
        log.debug("Cache miss for movie id={}, querying DB", id);
        return movieRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Movie", id));
    }

    /**
     * Get all active movies.
     *
     * @Cacheable: Cache the full list for 60s to avoid hammering DB.
     * Cache is invalidated on createMovie() / updateMovie() via @CacheEvict.
     */
    @Cacheable(value = "movies", key = "'all-active'")  // Cache key: "movies::all-active"
    public List<Movie> getAllActiveMovies() {
        return movieRepository.findByIsActiveTrue();  // SELECT * WHERE is_active = true
    }

    /**
     * Soft-delete a movie (set isActive = false).
     * Does NOT delete from DB – preserves historical booking data.
     *
     * @CacheEvict: clear cached data so next read hits DB.
     */
    @Transactional
    @CacheEvict(value = "movies", allEntries = true)  // Bust the entire movies cache
    public Movie deactivateMovie(Long id) {
        Movie movie = getMovieById(id);    // Fetch (may come from cache)
        movie.setIsActive(false);          // Mark as inactive (soft delete)
        Movie updated = movieRepository.save(movie);  // Persist the change
        log.info("Movie deactivated: id={}", id);

        // Also remove from Elasticsearch so it doesn't appear in search results
        movieSearchRepository.deleteById(String.valueOf(id));

        return updated;
    }
}

