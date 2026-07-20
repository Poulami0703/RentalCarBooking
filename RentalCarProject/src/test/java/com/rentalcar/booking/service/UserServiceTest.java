package com.rentalcar.booking.service;

import com.rentalcar.booking.dto.request.KycRequest;
import com.rentalcar.booking.dto.request.UpdateProfileRequest;
import com.rentalcar.booking.dto.response.UserResponse;
import com.rentalcar.booking.entity.User;
import com.rentalcar.booking.enums.AuthProvider;
import com.rentalcar.booking.enums.KycStatus;
import com.rentalcar.booking.enums.UserRole;
import com.rentalcar.booking.exception.ResourceNotFoundException;
import com.rentalcar.booking.repository.AuditLogRepository;
import com.rentalcar.booking.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock UserRepository userRepository;
    @Mock AuditLogRepository auditLogRepository;
    @InjectMocks UserService userService;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        sampleUser = User.builder()
                .id(1L).firstName("John").lastName("Doe")
                .email("john@example.com").phoneNumber("+919876543210")
                .role(UserRole.CUSTOMER).authProvider(AuthProvider.LOCAL)
                .kycStatus(KycStatus.NOT_SUBMITTED)
                .emailVerified(true).phoneVerified(false).enabled(true)
                .build();
    }

    // ---- getUserProfile ----

    @Test
    @DisplayName("getUserProfile: returns profile for known email")
    void getUserProfile_success() {
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(sampleUser));

        UserResponse resp = userService.getUserProfile("john@example.com");

        assertThat(resp.getEmail()).isEqualTo("john@example.com");
        assertThat(resp.getFirstName()).isEqualTo("John");
    }

    @Test
    @DisplayName("getUserProfile: unknown email throws ResourceNotFoundException")
    void getUserProfile_notFound_throws() {
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserProfile("unknown@example.com"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ---- updateProfile ----

    @Test
    @DisplayName("updateProfile: partial update changes only provided fields")
    void updateProfile_success() {
        UpdateProfileRequest req = new UpdateProfileRequest();
        req.setFirstName("Jane");
        req.setCity("Delhi");

        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(sampleUser));
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);
        when(auditLogRepository.save(any())).thenReturn(null);

        UserResponse resp = userService.updateProfile("john@example.com", req);

        assertThat(sampleUser.getFirstName()).isEqualTo("Jane");
        assertThat(sampleUser.getCity()).isEqualTo("Delhi");
        assertThat(sampleUser.getLastName()).isEqualTo("Doe"); // unchanged
    }

    // ---- submitKyc ----

    @Test
    @DisplayName("submitKyc: sets KYC to PENDING_REVIEW and stores license")
    void submitKyc_success() {
        KycRequest req = new KycRequest();
        req.setDrivingLicenseNumber("MH0120230012345");
        req.setDrivingLicenseUrl("https://storage.example.com/license.jpg");
        req.setLicenseExpiryDate("2028-06-30");

        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(sampleUser));
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);

        UserResponse resp = userService.submitKyc("john@example.com", req);

        assertThat(sampleUser.getKycStatus()).isEqualTo(KycStatus.PENDING_REVIEW);
        assertThat(sampleUser.getDrivingLicenseNumber()).isEqualTo("MH0120230012345");
    }

    // ---- approveKyc / rejectKyc ----

    @Test
    @DisplayName("approveKyc: sets KYC status to APPROVED")
    void approveKyc_success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);

        UserResponse resp = userService.approveKyc(1L);

        assertThat(sampleUser.getKycStatus()).isEqualTo(KycStatus.APPROVED);
    }

    @Test
    @DisplayName("rejectKyc: sets KYC status to REJECTED")
    void rejectKyc_success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);

        userService.rejectKyc(1L);

        assertThat(sampleUser.getKycStatus()).isEqualTo(KycStatus.REJECTED);
    }

    @Test
    @DisplayName("approveKyc: unknown id throws ResourceNotFoundException")
    void approveKyc_notFound_throws() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.approveKyc(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ---- disableUser ----

    @Test
    @DisplayName("disableUser: sets enabled=false")
    void disableUser_success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);

        userService.disableUser(1L);

        assertThat(sampleUser.isEnabled()).isFalse();
    }

    // ---- getUserById ----

    @Test
    @DisplayName("getUserById: returns user for known id")
    void getUserById_success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));

        UserResponse resp = userService.getUserById(1L);

        assertThat(resp.getId()).isEqualTo(1L);
    }
}
