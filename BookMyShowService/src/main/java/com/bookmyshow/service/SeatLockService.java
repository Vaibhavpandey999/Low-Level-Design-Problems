package com.bookmyshow.service;

import com.bookmyshow.exception.ResourceNotFoundException;
import com.bookmyshow.exception.SeatAlreadyLockedException;
import com.bookmyshow.model.ShowSeat;
import com.bookmyshow.model.redis.SeatLock;
import com.bookmyshow.repository.ShowSeatRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * ============================================================
 * SeatLockService – Redis-Backed Seat Locking
 * ============================================================
 *
 * Manages the temporary (10-minute) hold on seats while a user
 * is in the payment flow.
 *
 * WHY Redis for seat locking?
 * - SETNX (SET if Not eXists) is ATOMIC at the Redis level.
 *   Two concurrent threads cannot both get a "success" on the
 *   same key – one will always win, one will always fail.
 * - TTL auto-expires the lock, no cron job needed.
 * - Extremely fast: O(1) operations vs. DB row locking.
 *
 * Redis key format: "seat-lock:{showSeatId}"
 *
 * DESIGN PATTERN: Strategy (Redis as locking strategy) +
 *   atomic SETNX aligns with the Mutex / Semaphore patterns.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SeatLockService {

    private final RedisTemplate<String, Object> redisTemplate;  // Redis client
    private final ShowSeatRepository showSeatRepository;         // To validate seat exists

    @Value("${seat.lock.ttl-minutes:10}")  // Read TTL from config (default 10 min)
    private long lockTtlMinutes;

    /** Redis key prefix for seat locks */
    private static final String SEAT_LOCK_PREFIX = "seat-lock:";

    /**
     * Attempt to lock a ShowSeat for a specific user.
     *
     * Uses Redis SETNX (atomically set if key doesn't exist).
     * If the key already exists → another user has the lock → throw exception.
     *
     * @param showSeatId  ID of the ShowSeat to lock
     * @param userId      ID of the user requesting the lock
     * @throws SeatAlreadyLockedException if seat is already held
     */
    public void lockSeat(Long showSeatId, Long userId) {
        // Build Redis key for this specific show seat
        String lockKey = SEAT_LOCK_PREFIX + showSeatId;  // e.g. "seat-lock:42"

        // Verify seat exists in DB before locking
        ShowSeat showSeat = showSeatRepository.findById(showSeatId)
                .orElseThrow(() -> new ResourceNotFoundException("ShowSeat", showSeatId));

        // Reject lock attempt if seat is already BOOKED (permanent)
        if (showSeat.getStatus().name().equals("BOOKED")) {
            throw new SeatAlreadyLockedException(
                "Seat " + showSeatId + " is already booked and cannot be selected."
            );
        }

        // ATOMIC Redis SETNX: setIfAbsent returns true ONLY if the key was newly created
        // If key already exists (another user locked it), returns false
        Boolean lockAcquired = redisTemplate.opsForValue().setIfAbsent(
                lockKey,                          // Key: "seat-lock:{showSeatId}"
                userId.toString(),                // Value: user ID who holds the lock
                lockTtlMinutes,                   // TTL value
                TimeUnit.MINUTES                  // TTL unit (minutes)
        );

        // Check if lock was acquired
        if (Boolean.FALSE.equals(lockAcquired)) {
            // Lock exists → seat is held by another user
            String currentHolder = (String) redisTemplate.opsForValue().get(lockKey);
            log.warn("Seat {} is already locked by userId={}", showSeatId, currentHolder);
            throw new SeatAlreadyLockedException(
                "Seat " + showSeatId + " is currently held by another user. Please select a different seat."
            );
        }

        log.info("Seat {} locked by userId={} for {} minutes", showSeatId, userId, lockTtlMinutes);
    }

    /**
     * Release a seat lock (called after booking confirmed or user abandons).
     * Explicitly delete the Redis key (don't wait for TTL expiry).
     *
     * @param showSeatId  ShowSeat to unlock
     * @param userId      Must be the same user who acquired the lock (security check)
     */
    public void unlockSeat(Long showSeatId, Long userId) {
        String lockKey = SEAT_LOCK_PREFIX + showSeatId;  // Same key format as lock

        // Verify the requesting user is the one who holds the lock
        String currentHolder = (String) redisTemplate.opsForValue().get(lockKey);
        if (currentHolder != null && currentHolder.equals(userId.toString())) {
            redisTemplate.delete(lockKey);  // Delete Redis key → seat immediately available
            log.info("Seat {} unlocked by userId={}", showSeatId, userId);
        } else {
            log.warn("Unlock denied: seat {} not locked by userId={}", showSeatId, userId);
        }
    }

    /**
     * Check if a seat is currently locked.
     * Used for informational purposes (seat map display).
     *
     * @param showSeatId ShowSeat ID to check
     * @return true if locked, false if available
     */
    public boolean isSeatLocked(Long showSeatId) {
        String lockKey = SEAT_LOCK_PREFIX + showSeatId;
        return redisTemplate.hasKey(lockKey);  // O(1) Redis key existence check
    }

    /**
     * Force-release all seat locks for a booking (called on payment failure).
     * Allows other users to immediately select these seats.
     *
     * @param showSeatIds List of ShowSeat IDs to release
     * @param userId      User whose locks to release
     */
    public void releaseAllLocks(java.util.List<Long> showSeatIds, Long userId) {
        for (Long seatId : showSeatIds) {
            unlockSeat(seatId, userId);  // Release each lock individually
        }
        log.info("Released {} seat locks for userId={}", showSeatIds.size(), userId);
    }
}

