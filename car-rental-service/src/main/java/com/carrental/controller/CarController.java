package com.carrental.controller;

import com.carrental.model.Car;
import com.carrental.service.CarService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * CarController – REST endpoints for browsing and managing cars.
 *
 * Public endpoints (no JWT): GET /api/cars, GET /api/cars/{id}
 * Admin endpoints (JWT + ADMIN role): POST, PUT, DELETE /api/cars
 *
 * Base path: /api/cars
 */
@RestController
@RequestMapping("/api/cars")
@RequiredArgsConstructor
public class CarController {

    private final CarService carService;  // car CRUD + caching (PROXY PATTERN via @Cacheable)

    /**
     * Browse all available cars.
     *
     * GET /api/cars
     * Public – no JWT required.
     * Returns cached result from Redis if available (PROXY/CACHING PATTERN).
     *
     * @return list of AVAILABLE cars
     */
    @GetMapping
    public ResponseEntity<List<Car>> getAvailableCars() {
        return ResponseEntity.ok(carService.getAvailableCars());  // @Cacheable in service
    }

    /**
     * Get detailed information for a specific car.
     *
     * GET /api/cars/{id}
     * Public – no JWT required.
     *
     * @param id car primary key
     * @return Car entity (404 if not found)
     */
    @GetMapping("/{id}")
    public ResponseEntity<Car> getCarById(@PathVariable Long id) {
        return ResponseEntity.ok(carService.getCarById(id));
    }

    /**
     * Add a new car to the fleet.
     *
     * POST /api/cars
     * Requires: JWT + ADMIN role (@PreAuthorize)
     * Body: Car JSON (make, model, year, licensePlate, dailyRate, fuelType, categoryId)
     *
     * @param car car data from request body
     * @return 200 OK with saved Car entity
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")  // blocks non-ADMIN requests with 403 Forbidden
    public ResponseEntity<Car> addCar(@RequestBody Car car) {
        return ResponseEntity.ok(carService.addCar(car));  // evicts Redis cache on success
    }

    /**
     * Update an existing car (e.g., change status, daily rate, mileage).
     *
     * PUT /api/cars/{id}
     * Requires: JWT + ADMIN role
     *
     * @param id      car to update
     * @param updated fields to update (null fields are ignored)
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Car> updateCar(@PathVariable Long id, @RequestBody Car updated) {
        return ResponseEntity.ok(carService.updateCar(id, updated));
    }
}

