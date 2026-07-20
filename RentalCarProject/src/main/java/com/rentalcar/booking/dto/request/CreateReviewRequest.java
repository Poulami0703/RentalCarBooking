package com.rentalcar.booking.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateReviewRequest {

    @NotNull(message = "Vehicle ID is required")
    private Long vehicleId;

    @NotNull(message = "Booking ID is required")
    private Long bookingId;

    @NotNull(message = "Vehicle rating is required")
    @Min(value = 1, message = "Rating must be at least 1")
    @Max(value = 5, message = "Rating cannot exceed 5")
    private Integer vehicleRating;

    @Min(value = 1, message = "Driver rating must be at least 1")
    @Max(value = 5, message = "Driver rating cannot exceed 5")
    private Integer driverRating;

    @NotBlank(message = "Review text is required")
    private String reviewText;
}
