package com.rentalcar.booking.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewResponse {
    private Long id;
    private Long vehicleId;
    private String vehicleName;
    private Long reviewerId;
    private String reviewerName;
    private Integer vehicleRating;
    private Integer driverRating;
    private String reviewText;
    private boolean approved;
    private String adminResponse;
    private LocalDateTime createdAt;
}
