package com.bookmyshow.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * ============================================================
 * RedisConfig – Redis Template Configuration
 * ============================================================
 *
 * Configures RedisTemplate with:
 * - String keys (human-readable keys in Redis)
 * - JSON values (objects serialized via Jackson)
 *
 * WHY custom RedisTemplate?
 * The default Spring Boot RedisTemplate uses Java serialization
 * for values, which is not human-readable in Redis CLI and
 * creates coupling to Java. Using JSON serialization:
 * - Keys/values are readable via redis-cli
 * - Works with Redis clients in any language
 * - No ClassCastException risk on deserialization
 *
 * DESIGN PATTERN: Singleton (Spring @Bean) – one RedisTemplate
 *   shared across the application.
 */
@Configuration
public class RedisConfig {

    /**
     * Custom RedisTemplate bean:
     * - Keys:   serialized as plain Strings
     * - Values: serialized as JSON via Jackson
     * - Hash keys: Strings
     * - Hash values: JSON
     *
     * @param connectionFactory Auto-configured by Spring Boot from application.yml (redis.host/port)
     * @return Configured RedisTemplate
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();

        // Connect template to the Redis server via Lettuce connection factory
        template.setConnectionFactory(connectionFactory);

        // Key serializer: plain String (e.g. "seatlock:42", "session:user:5")
        template.setKeySerializer(new StringRedisSerializer());

        // Value serializer: JSON (e.g. SeatLock object → {"userId":1,"ttl":600,...})
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());

        // Hash key serializer: String (for Redis Hash field names)
        template.setHashKeySerializer(new StringRedisSerializer());

        // Hash value serializer: JSON (for Redis Hash field values)
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());

        template.afterPropertiesSet();  // Initialize template with set serializers
        return template;
    }
}

