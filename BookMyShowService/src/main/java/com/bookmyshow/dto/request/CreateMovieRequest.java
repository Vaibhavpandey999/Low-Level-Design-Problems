package com.bookmyshow.dto.request;

import com.bookmyshow.enums.Genre;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.util.List;

/** CreateMovieRequest – DTO for admin movie creation. */
@Data
public class CreateMovieRequest {
    @NotBlank private String title;
    private String description;
    @NotNull @Positive private Integer durationMinutes;
    @NotNull private Genre genre;
    @NotBlank private String language;
    @NotBlank private String certification;
    private String posterUrl;
    private String trailerUrl;
    private List<String> cast;
}

