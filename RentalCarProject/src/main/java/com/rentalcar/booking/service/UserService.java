package com.rentalcar.booking.service;

import com.rentalcar.booking.dto.request.UpdateProfileRequest;
import com.rentalcar.booking.dto.request.KycRequest;
import com.rentalcar.booking.dto.response.UserResponse;
import com.rentalcar.booking.entity.AuditLog;
import com.rentalcar.booking.entity.User;
import com.rentalcar.booking.enums.KycStatus;
import com.rentalcar.booking.exception.AccessDeniedException;
import com.rentalcar.booking.exception.ResourceNotFoundException;
import com.rentalcar.booking.repository.AuditLogRepository;
import com.rentalcar.booking.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final AuditLogRepository auditLogRepository;

    @Transactional(readOnly = true)
    public UserResponse getUserProfile(String email) {
        User user = findByEmail(email);
        return mapToResponse(user);
    }

    @Transactional
    public UserResponse updateProfile(String email, UpdateProfileRequest request) {
        User user = findByEmail(email);

        if (request.getFirstName() != null) user.setFirstName(request.getFirstName());
        if (request.getLastName() != null) user.setLastName(request.getLastName());
        if (request.getPhoneNumber() != null) user.setPhoneNumber(request.getPhoneNumber());
        if (request.getDateOfBirth() != null) user.setDateOfBirth(request.getDateOfBirth());
        if (request.getProfileImageUrl() != null) user.setProfileImageUrl(request.getProfileImageUrl());
        if (request.getAddressLine() != null) user.setAddressLine(request.getAddressLine());
        if (request.getCity() != null) user.setCity(request.getCity());
        if (request.getState() != null) user.setState(request.getState());
        if (request.getCountry() != null) user.setCountry(request.getCountry());
        if (request.getPinCode() != null) user.setPinCode(request.getPinCode());

        user = userRepository.save(user);
        log.info("Profile updated for user: {}", email);

        auditLogRepository.save(AuditLog.builder()
                .user(user)
                .action("PROFILE_UPDATED")
                .description("User profile updated")
                .entityType("User")
                .entityId(user.getId())
                .build());

        return mapToResponse(user);
    }

    @Transactional
    public UserResponse submitKyc(String email, KycRequest request) {
        User user = findByEmail(email);
        user.setDrivingLicenseNumber(request.getDrivingLicenseNumber());
        user.setDrivingLicenseUrl(request.getDrivingLicenseUrl());
        if (request.getLicenseExpiryDate() != null) {
            user.setLicenseExpiryDate(LocalDate.parse(request.getLicenseExpiryDate()));
        }
        user.setKycStatus(KycStatus.PENDING_REVIEW);
        user = userRepository.save(user);
        log.info("KYC submitted for user: {}", email);
        return mapToResponse(user);
    }

    // Admin methods
    @Transactional(readOnly = true)
    public Page<UserResponse> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable).map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        return mapToResponse(user);
    }

    @Transactional
    public UserResponse approveKyc(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        user.setKycStatus(KycStatus.APPROVED);
        user = userRepository.save(user);
        log.info("KYC approved for user id: {}", userId);
        return mapToResponse(user);
    }

    @Transactional
    public UserResponse rejectKyc(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        user.setKycStatus(KycStatus.REJECTED);
        user = userRepository.save(user);
        log.info("KYC rejected for user id: {}", userId);
        return mapToResponse(user);
    }

    @Transactional
    public void disableUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        user.setEnabled(false);
        userRepository.save(user);
        log.info("User disabled: id={}", userId);
    }

    private User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
    }

    public UserResponse mapToResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .dateOfBirth(user.getDateOfBirth())
                .role(user.getRole())
                .authProvider(user.getAuthProvider())
                .emailVerified(user.isEmailVerified())
                .phoneVerified(user.isPhoneVerified())
                .profileImageUrl(user.getProfileImageUrl())
                .addressLine(user.getAddressLine())
                .city(user.getCity())
                .state(user.getState())
                .country(user.getCountry())
                .pinCode(user.getPinCode())
                .kycStatus(user.getKycStatus())
                .drivingLicenseNumber(user.getDrivingLicenseNumber())
                .licenseExpiryDate(user.getLicenseExpiryDate())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
