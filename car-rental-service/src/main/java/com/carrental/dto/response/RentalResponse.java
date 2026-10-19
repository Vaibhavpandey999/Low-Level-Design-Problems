package com.carrental.dto.response;

import com.carrental.enums.RentalStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * RentalResponse – DTO returned from booking and rental history endpoints.
 * Flattens the Rental entity + nested Car, Branch objects into a flat response.
 */
@Getter
@Builder  // BUILDER PATTERN for construction in RentalController
public class RentalResponse {

    private Long rentalId;              // rental primary key
    private String rentalReference;     // human-readable booking reference e.g. "CR-AB12CD34"
    private RentalStatus status;        // PENDING / ACTIVE / COMPLETED / CANCELLED

    // Car details (denormalised for frontend convenience)
    private Long carId;
    private String carMake;             // e.g., "Toyota"
    private String carModel;            // e.g., "Camry"
    private String carLicensePlate;     // vehicle registration number

    // Branch details
    private String pickupBranchName;    // pickup location name
    private String dropoffBranchName;   // return location name

    // Date range
    private LocalDate startDate;        // rental start
    private LocalDate endDate;          // planned return
    private LocalDate actualReturnDate; // actual return (null while active)

    // Financial
    private BigDecimal totalAmount;     // base rental charge
    private BigDecimal damageCharge;    // assessed on return
    private BigDecimal overstayCharge;  // if returned late

    private LocalDateTime createdAt;    // booking creation timestamp
}

