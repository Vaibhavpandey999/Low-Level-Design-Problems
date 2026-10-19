package com.carrental.service;

import com.carrental.exception.CarNotAvailableException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * CarAvailabilityService – Redis-backed distributed locking service for car booking.
 *
 * Why Redis for locking instead of DB-level locks?
 * - Redis SETNX (Set-if-Not-eXists) is atomic at the server level
 * - TTL auto-expires the lock after 30 seconds (no cron cleanup needed)
 * - Sub-millisecond latency vs database row locking overhead
 * - Distributed: works across multiple Spring Boot instances (Kubernetes pods)
 *
 * Lock key pattern: "car:lock:{carId}"
 * Lock value: userId (so we know which user holds the lock)
 *
 * This complements the @Version optimistic locking on Car entity –
 * Redis prevents unnecessary DB round-trips while @Version is the final
 * consistency guardrail at the persistence layer.
 */
@Service
@RequiredArgsConstructor  // Lombok – constructor injection for all final fields
@Slf4j
public class CarAvailabilityService {

    // Spring Data Redis template for raw String key/value operations
    private final StringRedisTemplate redisTemplate;

    /** Redis key prefix for car booking locks */
    private static final String LOCK_KEY_PREFIX = "car:lock:";

    /** Lock TTL – auto-expires after 30 seconds to prevent deadlocks */
    private static final Duration LOCK_TTL = Duration.ofSeconds(30);

    /**
     * Attempt to acquire an exclusive lock on a car for a specific user.
     *
     * Uses Redis SETNX (SET with NX=only-if-not-exists and EX=expiry).
     * Only ONE concurrent caller can acquire the lock for a given carId.
     *
     * @param carId  the car to lock
     * @param userId the user attempting the booking
     * @return true if lock was acquired, false if another user holds it
     */
    public boolean acquireLock(Long carId, Long userId) {
        String lockKey = LOCK_KEY_PREFIX + carId;  // e.g., "car:lock:42"
        String lockValue = userId.toString();       // value records who holds the lock

        // setIfAbsent = SETNX + EXPIRE in one atomic Redis command
        Boolean acquired = redisTemplate.opsForValue()
                .setIfAbsent(lockKey, lockValue, LOCK_TTL);  // atomic: set ONLY if key doesn't exist

        boolean success = Boolean.TRUE.equals(acquired);     // null-safe unboxing
        log.debug("[AvailabilityService] Lock acquire: carId={}, userId={}, success={}", carId, userId, success);
        return success;
    }

    /**
     * Release the lock held by a specific user.
     *
     * Only releases if the current lock value matches the userId –
     * prevents a user from accidentally releasing another user's lock.
     *
     * @param carId  the car whose lock should be released
     * @param userId the user releasing the lock
     */
    public void releaseLock(Long carId, Long userId) {
        String lockKey = LOCK_KEY_PREFIX + carId;   // same key as acquireLock
        String currentHolder = redisTemplate.opsForValue().get(lockKey);  // check who holds lock

        if (userId.toString().equals(currentHolder)) {
            // only delete the key if THIS user holds it
            redisTemplate.delete(lockKey);
            log.debug("[AvailabilityService] Lock released: carId={}, userId={}", carId, userId);
        } else {
            // another user's lock (or expired and re-acquired) – do not touch it
            log.warn("[AvailabilityService] Lock release skipped: carId={} held by different user", carId);
        }
    }

    /**
     * Checks whether a car is currently locked (has an active booking in progress).
     *
     * @param carId the car to check
     * @return true if car is currently being booked by someone
     */
    public boolean isLocked(Long carId) {
        String lockKey = LOCK_KEY_PREFIX + carId;
        return Boolean.TRUE.equals(redisTemplate.hasKey(lockKey));  // true if key exists in Redis
    }

    /**
     * Acquire lock or throw CarNotAvailableException.
     * Convenience method for use in RentalFacade.
     *
     * @param carId  car to lock
     * @param userId user attempting booking
     * @throws CarNotAvailableException if lock cannot be acquired (car is busy)
     */
    public void acquireLockOrThrow(Long carId, Long userId) {
        if (!acquireLock(carId, userId)) {
            // another booking is in progress for this car
            throw new CarNotAvailableException(
                    "Car " + carId + " is currently being booked by another user. Please try again.");
        }
    }
}

