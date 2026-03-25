package com.movieplatform.booking.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

/**
 * Seat Lock Service — Redis SETNX distributed lock.
 *
 * Design Decision: Redis TTL = 10 minutes.
 * If payment never arrives within 10 min, Redis auto-expires the lock
 * and seats become available again — zero manual cleanup required.
 *
 * Key format: seat:lock:{seatLayoutId}:{showId}
 * Value: bookingId (so we know which booking holds the lock)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SeatLockService {

    private final StringRedisTemplate redisTemplate;

    private static final Duration LOCK_TTL = Duration.ofMinutes(10);
    private static final String KEY_PREFIX  = "seat:lock:";

    /**
     * Attempt to lock all requested seats atomically.
     * Returns true only if ALL seats were locked successfully.
     * If any seat fails, rolls back all acquired locks.
     */
    public boolean lockSeats(List<UUID> seatLayoutIds, UUID showId, UUID bookingId) {
        List<String> keys = buildKeys(seatLayoutIds, showId);
        List<String> locked = new java.util.ArrayList<>();

        for (String key : keys) {
            Boolean acquired = redisTemplate.opsForValue()
                .setIfAbsent(key, bookingId.toString(), LOCK_TTL);

            if (Boolean.TRUE.equals(acquired)) {
                locked.add(key);
                log.debug("Seat lock acquired: key={} bookingId={}", key, bookingId);
            } else {
                // Rollback — release all acquired locks
                log.warn("Seat lock conflict on key={} — rolling back {} acquired locks", key, locked.size());
                locked.forEach(k -> redisTemplate.delete(k));
                return false;
            }
        }
        log.info("All {} seats locked for bookingId={}", seatLayoutIds.size(), bookingId);
        return true;
    }

    /**
     * Release seat locks after booking confirmed or failed.
     */
    public void releaseSeats(List<UUID> seatLayoutIds, UUID showId) {
        List<String> keys = buildKeys(seatLayoutIds, showId);
        keys.forEach(key -> {
            redisTemplate.delete(key);
            log.debug("Seat lock released: key={}", key);
        });
        log.info("Released {} seat locks for showId={}", seatLayoutIds.size(), showId);
    }

    /**
     * Check if a specific seat is currently locked.
     */
    public boolean isSeatLocked(UUID seatLayoutId, UUID showId) {
        String key = KEY_PREFIX + seatLayoutId + ":" + showId;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    private List<String> buildKeys(List<UUID> seatLayoutIds, UUID showId) {
        return seatLayoutIds.stream()
            .map(id -> KEY_PREFIX + id + ":" + showId)
            .toList();
    }
}
