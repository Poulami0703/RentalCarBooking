package com.rentalcar.booking.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class KycRequest {

    @NotBlank(message = "Driving license number is required")
    private String drivingLicenseNumber;

    @NotBlank(message = "Driving license document URL is required")
    private String drivingLicenseUrl;

    private String licenseExpiryDate;
}
