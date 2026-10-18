package com.bookmyshow.dto.request;

import com.bookmyshow.enums.PaymentGateway;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * BookingRequest – DTO for creating a new booking.
 * User selects a show and one or more show-seat IDs.
 */
@Data
public class BookingRequest {

    @NotNull(message = "Show ID is required")
    private Long showId;                         // Which show to book

    @NotEmpty(message = "At least one seat must be selected")
    private List<Long> showSeatIds;              // List of ShowSeat IDs the user selected

    @NotNull(message = "Payment gateway is required")
    private PaymentGateway paymentGateway;       // Which gateway to charge (RAZORPAY/STRIPE/PAYTM)
}

