package com.bookmyshow.dto.response;

import com.bookmyshow.enums.SeatStatus;
import com.bookmyshow.enums.SeatType;
import lombok.*;

/** SeatAvailabilityResponse – one seat's status in the show seat map. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeatAvailabilityResponse {
    private Long showSeatId;      // ShowSeat DB ID (sent to BookingRequest)
    private String rowNumber;     // e.g. "A"
    private Integer seatNumber;   // e.g. 3
    private SeatType seatType;    // NORMAL / PREMIUM / RECLINER
    private SeatStatus status;    // AVAILABLE / LOCKED / BOOKED
    private Double price;         // Price for this seat
}

