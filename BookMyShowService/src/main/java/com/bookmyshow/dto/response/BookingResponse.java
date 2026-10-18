package com.bookmyshow.dto.response;

import com.bookmyshow.enums.BookingStatus;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

/** BookingResponse – returned after booking creation or status query. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingResponse {
    private Long bookingId;                 // DB booking ID
    private String bookingReference;        // Human-readable reference (BMS-xxx)
    private BookingStatus status;           // Current booking status
    private String movieTitle;             // Movie name
    private String theatreName;            // Theatre name
    private String screenName;             // Screen/auditorium name
    private LocalDateTime showStartTime;   // Show timing
    private List<String> seats;            // List of seat labels (e.g. ["A1", "A2"])
    private Double totalAmount;            // Total amount paid
    private LocalDateTime bookedAt;        // When booking was created
}

