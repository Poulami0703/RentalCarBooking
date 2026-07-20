package com.rentalcar.booking.service;

import com.rentalcar.booking.dto.response.UserResponse;
import com.rentalcar.booking.entity.AuditLog;
import com.rentalcar.booking.entity.User;
import com.rentalcar.booking.enums.KycStatus;
import com.rentalcar.booking.exception.*;
import com.rentalcar.booking.dto.request.*;
import com.rentalcar.booking.dto.response.AuthResponse;
import com.rentalcar.booking.enums.AuthProvider;
import com.rentalcar.booking.repository.AuditLogRepository;
import com.rentalcar.booking.repository.UserRepository;
import com.rentalcar.booking.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final AuditLogRepository auditLogRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateEmailException(request.getEmail());
        }

        User user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .passwordHash(request.getPassword() != null
                        ? passwordEncoder.encode(request.getPassword()) : null)
                .phoneNumber(request.getPhoneNumber())
                .authProvider(request.getAuthProvider() != null ? request.getAuthProvider() : AuthProvider.LOCAL)
                .socialId(request.getSocialId())
                .emailVerified(request.getAuthProvider() != null && request.getAuthProvider() != AuthProvider.LOCAL)
                .build();

        user = userRepository.save(user);
        log.info("New user registered: {}", user.getEmail());
        saveAuditLog(user, "USER_REGISTERED", "New user registration via " + user.getAuthProvider());

        return buildAuthResponse(user);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );
        } catch (BadCredentialsException ex) {
            log.warn("Failed login attempt for email: [REDACTED]");
            throw new InvalidCredentialsException();
        }

        User user = userRepository.findActiveByEmail(request.getEmail())
                .orElseThrow(InvalidCredentialsException::new);

        log.info("User logged in: {}", user.getEmail());
        saveAuditLog(user, "USER_LOGIN", "User logged in via email/password");

        return buildAuthResponse(user);
    }

    @Transactional
    public void sendOtp(SendOtpRequest request) {
        User user = userRepository.findByPhoneNumber(request.getPhoneNumber())
                .orElseGet(() -> {
                    User newUser = User.builder()
                            .firstName("OTP")
                            .lastName("User")
                            .email(UUID.randomUUID() + "@otp.placeholder")
                            .phoneNumber(request.getPhoneNumber())
                            .authProvider(AuthProvider.OTP)
                            .build();
                    return userRepository.save(newUser);
                });

        String otp = generateOtp();
        user.setOtpCode(passwordEncoder.encode(otp));
        user.setOtpExpiresAt(LocalDateTime.now().plusMinutes(10));
        userRepository.save(user);

        // In production, integrate with an SMS gateway such as Twilio
        log.info("OTP generated for phone: [REDACTED] — would be sent via SMS gateway");
    }

    @Transactional
    public AuthResponse verifyOtp(OtpLoginRequest request) {
        User user = userRepository.findByPhoneNumber(request.getPhoneNumber())
                .orElseThrow(InvalidOtpException::new);

        if (user.getOtpExpiresAt() == null || LocalDateTime.now().isAfter(user.getOtpExpiresAt())) {
            throw new OtpExpiredException();
        }
        if (!passwordEncoder.matches(request.getOtp(), user.getOtpCode())) {
            throw new InvalidOtpException();
        }

        user.setOtpCode(null);
        user.setOtpExpiresAt(null);
        user.setPhoneVerified(true);
        user = userRepository.save(user);

        log.info("OTP verified for phone: [REDACTED]");
        saveAuditLog(user, "USER_LOGIN", "User logged in via OTP");

        return buildAuthResponse(user);
    }

    @Transactional
    public AuthResponse socialLogin(RegisterRequest request) {
        if (request.getSocialId() == null || request.getAuthProvider() == null) {
            throw new InvalidCredentialsException("Social login requires socialId and authProvider");
        }

        User user = userRepository
                .findBySocialIdAndAuthProvider(request.getSocialId(), request.getAuthProvider())
                .orElseGet(() -> {
                    if (userRepository.existsByEmail(request.getEmail())) {
                        // Link social account to existing email account
                        User existing = userRepository.findByEmail(request.getEmail())
                                .orElseThrow(InvalidCredentialsException::new);
                        existing.setSocialId(request.getSocialId());
                        existing.setAuthProvider(request.getAuthProvider());
                        existing.setEmailVerified(true);
                        return userRepository.save(existing);
                    }
                    // New social user — register and return the persisted entity
                    register(request);
                    return userRepository.findByEmail(request.getEmail()).orElseThrow();
                });

        log.info("Social login: {}", request.getAuthProvider());
        saveAuditLog(user, "USER_LOGIN", "User logged in via " + request.getAuthProvider());
        return buildAuthResponse(user);
    }

    private AuthResponse buildAuthResponse(User user) {
        String token = jwtTokenProvider.generateToken(user.getEmail(),
                Map.of("role", user.getRole().name(), "userId", user.getId()));

        return AuthResponse.builder()
                .accessToken(token)
                .expiresIn(jwtTokenProvider.getExpirationMs() / 1000)
                .user(mapToUserResponse(user))
                .build();
    }

    private String generateOtp() {
        return String.format("%06d", new Random().nextInt(1_000_000));
    }

    private void saveAuditLog(User user, String action, String description) {
        AuditLog auditLog = AuditLog.builder()
                .user(user)
                .action(action)
                .description(description)
                .entityType("User")
                .entityId(user.getId())
                .build();
        auditLogRepository.save(auditLog);
    }

    private UserResponse mapToUserResponse(User user) {
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
                .city(user.getCity())
                .state(user.getState())
                .country(user.getCountry())
                .kycStatus(user.getKycStatus())
                .drivingLicenseNumber(user.getDrivingLicenseNumber())
                .licenseExpiryDate(user.getLicenseExpiryDate())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
