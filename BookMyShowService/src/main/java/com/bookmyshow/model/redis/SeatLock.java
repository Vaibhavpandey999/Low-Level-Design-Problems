package com.bookmyshow.model.redis;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.TimeToLive;
import org.springframework.data.redis.core.index.Indexed;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;

/**
 * ============================================================
 * SeatLock – Redis Hash stored with TTL
 * ============================================================
 *
 * Represents a temporary hold placed on a ShowSeat by a User.
 * Stored in Redis (not PostgreSQL) because:
 * 1. It has a short life (10 min) → Redis TTL auto-cleans it.
 * 2. O(1) SETNX (set-if-not-exists) atomic operation prevents
 *    two users locking the same seat simultaneously.
 * 3. Avoids expensive DB writes for ephemeral lock data.
 *
 * Redis key format: "SeatLock:{showSeatId}"
 * (set by @RedisHash + @Id → "SeatLock:{id}")
 *
 * DESIGN PATTERN: None specific, but part of the
 *   Optimistic Concurrency strategy: Redis handles
 *   the short-lived lock while PostgreSQL @Version
 *   handles final write-conflict detection.
 */
@RedisHash("SeatLock")           // Store as Redis Hash with key prefix "SeatLock:"
                              //RedisHash tells Spring Data Redis:
//
//“Treat this Java class as a Redis-backed entity and store its objects as Redis hashes.”
//
//The "SeatLock" part is the keyspace/prefix.
//
//For example, if:
//
//SeatLock lock = new SeatLock();
//lock.setId("A1");
@Data                            // Lombok: getters/setters/etc.
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeatLock implements Serializable {

    @Id                          // Redis @Id – used as the hash key suffix
    private String id;           // Key = "showSeatId" (e.g. "42")

    @Indexed                     // Allows secondary index lookup by userId
    private Long userId;         // Which user holds this lock

    private Long showSeatId;     // Redundant reference to the locked ShowSeat

    private Long showId;         // Show reference (for quick validation)

    /**
     * TTL annotation tells Spring Data Redis to set this hash's
     * expiry in Redis automatically.
     * Default 10 minutes = 600 seconds.
     * When TTL expires, Redis deletes the key → seat becomes AVAILABLE.
     */
    @TimeToLive(unit = TimeUnit.SECONDS)
    @Builder.Default
    private Long ttl = 600L;     // 600 seconds = 10 minutes seat hold duration
}

