package com.rentalcar.booking.dto.response;

import com.rentalcar.booking.enums.AuthProvider;
import com.rentalcar.booking.enums.KycStatus;
import com.rentalcar.booking.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;
    private LocalDate dateOfBirth;
    private UserRole role;
    private AuthProvider authProvider;
    private boolean emailVerified;
    private boolean phoneVerified;
    private String profileImageUrl;
    private String addressLine;
    private String city;
    private String state;
    private String country;
    private String pinCode;
    private KycStatus kycStatus;
    private String drivingLicenseNumber;
    private LocalDate licenseExpiryDate;
    private LocalDateTime createdAt;
}
