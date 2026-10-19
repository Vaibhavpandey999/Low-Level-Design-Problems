package com.carrental.service;

import com.carrental.enums.CarStatus;
import com.carrental.model.Car;
import com.carrental.model.CarCategory;
import com.carrental.repository.jpa.CarRepository;
import com.carrental.repository.jpa.CarCategoryRepository;
import com.carrental.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * CarService – CRUD operations for the car fleet.
 *
 * Uses Spring's @Cacheable / @CacheEvict (PROXY PATTERN via Redis) to cache
 * frequently read car listings, reducing PostgreSQL load under peak traffic.
 *
 * Caching strategy:
 * - Cache car listings by status on first read (TTL set in Redis)
 * - Evict cache when a car is updated (stale cache must not mislead customers)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CarService {

    private final CarRepository carRepository;              // REPOSITORY PATTERN
    private final CarCategoryRepository carCategoryRepository; // REPOSITORY PATTERN

    /**
     * Get all cars with AVAILABLE status.
     * Result is cached in Redis under key "cars::available".
     * Subsequent calls within TTL return cached result without hitting PostgreSQL.
     *
     * PROXY PATTERN: Spring AOP intercepts the call and checks Redis before DB.
     */
    @Cacheable(value = "cars", key = "'available'")  // ← PROXY/CACHING PATTERN
    public List<Car> getAvailableCars() {
        log.debug("[CarService] Cache miss – loading available cars from DB");
        return carRepository.findByStatus(CarStatus.AVAILABLE);  // REPOSITORY PATTERN
    }

    /**
     * Get a specific car by ID.
     * Cached individually per carId.
     *
     * @param id car primary key
     * @return Car entity
     * @throws ResourceNotFoundException if car doesn't exist
     */
    @Cacheable(value = "cars", key = "#id")  // cache individual car by ID
    public Car getCarById(Long id) {
        return carRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Car not found: " + id));
    }

    /**
     * Add a new car to the fleet (ADMIN only – enforced by @PreAuthorize in controller).
     * Evicts the "available" cars cache so the new car appears in listings.
     *
     * @param car car entity to persist
     * @return saved Car with generated ID
     */
    @Transactional
    @CacheEvict(value = "cars", key = "'available'")  // invalidate cache after new car added
    public Car addCar(Car car) {
        log.info("[CarService] Adding car: {} {}", car.getMake(), car.getModel());
        car.setStatus(CarStatus.AVAILABLE);  // new cars start as available
        return carRepository.save(car);      // persist to DB
    }

    /**
     * Update car details (ADMIN only).
     * Evicts both the specific car cache and the list cache.
     *
     * @param id      car to update
     * @param updated updated fields
     * @return updated Car entity
     */
    @Transactional
    @CacheEvict(value = "cars", allEntries = true)  // clear all car cache entries on update
    public Car updateCar(Long id, Car updated) {
        Car existing = carRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Car not found: " + id));
        // Update only non-null fields (partial update pattern)
        if (updated.getDailyRate() != null) existing.setDailyRate(updated.getDailyRate());
        if (updated.getStatus() != null)    existing.setStatus(updated.getStatus());
        if (updated.getMileage() != null)   existing.setMileage(updated.getMileage());
        return carRepository.save(existing);  // Hibernate detects changes and generates UPDATE SQL
    }
}

