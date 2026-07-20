package com.rentalcar.booking.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;

@Data
public class UpdateProfileRequest {

    @Size(max = 100)
    private String firstName;

    @Size(max = 100)
    private String lastName;

    @Pattern(regexp = "^[+]?[0-9]{10,15}$", message = "Please provide a valid phone number")
    private String phoneNumber;

    private LocalDate dateOfBirth;

    private String profileImageUrl;

    private String addressLine;

    private String city;

    private String state;

    private String country;

    @Size(max = 10)
    private String pinCode;
}
