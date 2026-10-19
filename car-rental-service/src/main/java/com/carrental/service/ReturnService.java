package com.carrental.service;

import com.carrental.enums.CarStatus;
import com.carrental.enums.RentalStatus;
import com.carrental.exception.InvalidRentalStateException;
import com.carrental.exception.ResourceNotFoundException;
import com.carrental.model.Car;
import com.carrental.model.Rental;
import com.carrental.repository.jpa.CarRepository;
import com.carrental.repository.jpa.RentalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * ReturnService – handles the car return workflow.
 *
 * When a customer returns a car, this service:
 * 1. Validates the rental is in ACTIVE state
 * 2. Calculates overstay charges (if returned late)
 * 3. Records damage charges (if provided)
 * 4. Marks the rental as COMPLETED
 * 5. Marks the car as AVAILABLE again
 *
 * The car status update uses the @Version optimistic lock on Car entity –
 * preventing race conditions if two return requests arrive simultaneously.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReturnService {

    private final RentalRepository rentalRepository;  // REPOSITORY PATTERN
    private final CarRepository carRepository;         // REPOSITORY PATTERN

    /**
     * Process a car return.
     *
     * @param rentalId         rental to complete
     * @param actualReturnDate date the car was physically returned
     * @param damageCharge     additional damage fee assessed by branch staff (0 if no damage)
     * @return updated Rental entity with COMPLETED status and final charges
     */
    @Transactional  // all DB updates in one atomic transaction
    public Rental processReturn(Long rentalId, LocalDate actualReturnDate, BigDecimal damageCharge) {
        log.info("[ReturnService] Processing return. rentalId={}, returnDate={}", rentalId, actualReturnDate);

        // ── Step 1: Load rental and validate state ─────────────────────────────────────
        Rental rental = rentalRepository.findById(rentalId)
                .orElseThrow(() -> new ResourceNotFoundException("Rental not found: " + rentalId));

        // Only ACTIVE rentals can be returned
        if (rental.getStatus() != RentalStatus.ACTIVE) {
            throw new InvalidRentalStateException(
                    "Cannot return rental in state: " + rental.getStatus());
        }
        // ──────────────────────────────────────────────────────────────────────────────

        // ── Step 2: Calculate overstay charge ──────────────────────────────────────────
        BigDecimal overstayCharge = BigDecimal.ZERO;  // default: no overstay
        if (actualReturnDate.isAfter(rental.getEndDate())) {
            // customer returned the car after the agreed return date
            long overdueDays = ChronoUnit.DAYS.between(rental.getEndDate(), actualReturnDate);
            // overstay rate = 1.5x the daily rental rate per overdue day
            overstayCharge = rental.getCar().getDailyRate()
                    .multiply(BigDecimal.valueOf(1.5))                   // 1.5x penalty multiplier
                    .multiply(BigDecimal.valueOf(overdueDays));          // × number of overdue days
            log.info("[ReturnService] Overstay detected. days={}, charge={}", overdueDays, overstayCharge);
        }
        // ──────────────────────────────────────────────────────────────────────────────

        // ── Step 3: Update rental record ───────────────────────────────────────────────
        rental.setActualReturnDate(actualReturnDate);    // record when car was actually returned
        rental.setDamageCharge(damageCharge);            // damage fee assessed by branch staff
        rental.setOverstayCharge(overstayCharge);        // calculated overstay penalty
        rental.setStatus(RentalStatus.COMPLETED);        // advance state machine to terminal state
        rental = rentalRepository.save(rental);          // persist all changes
        // ──────────────────────────────────────────────────────────────────────────────

        // ── Step 4: Release car back to fleet ──────────────────────────────────────────
        // Load with optimistic lock to prevent race condition on status update
        Car car = carRepository.findByIdWithOptimisticLock(rental.getCar().getId())  // ← OPTIMISTIC LOCKING
                .orElseThrow(() -> new ResourceNotFoundException("Car not found"));
        car.setStatus(CarStatus.AVAILABLE);  // car is available for the next booking
        carRepository.save(car);             // Hibernate auto-increments @Version field
        log.info("[ReturnService] Car {} marked AVAILABLE after return.", car.getId());
        // ──────────────────────────────────────────────────────────────────────────────

        return rental;  // return completed rental to caller (RentalController)
    }
}

