package com.carrental.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * RedisConfig – configures RedisTemplate beans for caching and distributed locking.
 *
 * Why Redis in car rental?
 * - Atomic SETNX operations for distributed locks (car booking lock per carId)
 * - TTL-based auto-expiry prevents deadlocks (no manual cleanup needed)
 * - O(1) availability checks (is car locked?)
 * - @Cacheable support for car listings (reduces PostgreSQL load)
 *
 * Two templates configured:
 * 1. StringRedisTemplate – for lock operations (key=String, value=String userId)
 * 2. RedisTemplate<String, Object> – for JSON-serialised object caching
 */
@Configuration
public class RedisConfig {

    /**
     * StringRedisTemplate bean – used by CarAvailabilityService for lock operations.
     * Both key and value are plain Strings (no serialisation overhead for lock values).
     *
     * Spring Boot auto-configures this if spring.data.redis.host is set,
     * but we define it explicitly for clarity and testability.
     */
    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
        return new StringRedisTemplate(connectionFactory);  // String key + String value serializer
    }

    /**
     * Generic RedisTemplate – used for @Cacheable object caching (e.g., car listings).
     * Keys serialised as Strings, values as JSON using Jackson.
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);  // connect to Redis server

        // Use String serialiser for keys (human-readable in Redis CLI)
        template.setKeySerializer(new StringRedisSerializer());

        // Use Jackson JSON serialiser for values (stores objects as JSON strings)
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());

        // Also configure hash key/value serialisers (used for Redis hashes)
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());

        template.afterPropertiesSet();  // initialise the template
        return template;
    }
}

