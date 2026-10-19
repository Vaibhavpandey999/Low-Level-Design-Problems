package com.carrental.dto.request;

import com.carrental.enums.PaymentMethod;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/**
 * BookCarRequest – DTO for the car booking endpoint.
 * Carries all parameters needed by RentalFacade.bookCar().
 */
@Getter
@Setter
public class BookCarRequest {

    @NotNull(message = "Car ID is required")
    private Long carId;                   // the car to rent

    @NotNull(message = "Pickup branch is required")
    private Long pickupBranchId;          // where the customer will pick up the car

    @NotNull(message = "Dropoff branch is required")
    private Long dropoffBranchId;         // where the customer will return the car

    @NotNull(message = "Start date is required")
    @Future(message = "Start date must be in the future")  // cannot book in the past
    private LocalDate startDate;          // rental start date

    @NotNull(message = "End date is required")
    @Future(message = "End date must be in the future")
    private LocalDate endDate;            // planned return date

    @NotNull(message = "Payment method is required")
    private PaymentMethod paymentMethod;  // CREDIT_CARD / UPI / WALLET
}

