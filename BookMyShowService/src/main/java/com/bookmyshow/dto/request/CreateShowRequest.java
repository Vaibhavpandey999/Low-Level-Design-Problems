package com.bookmyshow.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

/** CreateShowRequest – DTO for admin scheduling a show. */
@Data
public class CreateShowRequest {
    @NotNull private Long movieId;       // Movie to screen
    @NotNull private Long screenId;      // Auditorium to use
    @NotNull private LocalDateTime startTime;  // Show start time
}

