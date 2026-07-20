package com.rentalcar.booking.service;

import com.rentalcar.booking.dto.request.LoginRequest;
import com.rentalcar.booking.dto.request.OtpLoginRequest;
import com.rentalcar.booking.dto.request.RegisterRequest;
import com.rentalcar.booking.dto.request.SendOtpRequest;
import com.rentalcar.booking.dto.response.AuthResponse;
import com.rentalcar.booking.entity.User;
import com.rentalcar.booking.enums.AuthProvider;
import com.rentalcar.booking.enums.KycStatus;
import com.rentalcar.booking.enums.UserRole;
import com.rentalcar.booking.exception.DuplicateEmailException;
import com.rentalcar.booking.exception.InvalidCredentialsException;
import com.rentalcar.booking.exception.InvalidOtpException;
import com.rentalcar.booking.exception.OtpExpiredException;
import com.rentalcar.booking.repository.AuditLogRepository;
import com.rentalcar.booking.repository.UserRepository;
import com.rentalcar.booking.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserRepository userRepository;
    @Mock AuditLogRepository auditLogRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtTokenProvider jwtTokenProvider;
    @Mock AuthenticationManager authenticationManager;

    @InjectMocks AuthService authService;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        sampleUser = User.builder()
                .id(1L)
                .firstName("John")
                .lastName("Doe")
                .email("john@example.com")
                .passwordHash("hashed")
                .role(UserRole.CUSTOMER)
                .authProvider(AuthProvider.LOCAL)
                .kycStatus(KycStatus.NOT_SUBMITTED)
                .build();
    }

    // ---- register ----

    @Test
    @DisplayName("register: success creates user and returns token")
    void register_success() {
        RegisterRequest req = new RegisterRequest();
        req.setFirstName("John"); req.setLastName("Doe");
        req.setEmail("john@example.com"); req.setPassword("pass1234");

        when(userRepository.existsByEmail(req.getEmail())).thenReturn(false);
        when(passwordEncoder.encode("pass1234")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);
        when(auditLogRepository.save(any())).thenReturn(null);
        when(jwtTokenProvider.generateToken(anyString(), anyMap())).thenReturn("jwt-token");
        when(jwtTokenProvider.getExpirationMs()).thenReturn(86400000L);

        AuthResponse resp = authService.register(req);

        assertThat(resp.getAccessToken()).isEqualTo("jwt-token");
        assertThat(resp.getUser().getEmail()).isEqualTo("john@example.com");
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("register: duplicate email throws DuplicateEmailException")
    void register_duplicateEmail_throws() {
        RegisterRequest req = new RegisterRequest();
        req.setEmail("john@example.com");

        when(userRepository.existsByEmail(req.getEmail())).thenReturn(true);

        assertThatThrownBy(() -> authService.register(req))
                .isInstanceOf(DuplicateEmailException.class)
                .hasMessageContaining("already registered");
    }

    // ---- login ----

    @Test
    @DisplayName("login: valid credentials return JWT")
    void login_success() {
        LoginRequest req = new LoginRequest();
        req.setEmail("john@example.com"); req.setPassword("pass1234");

        when(userRepository.findActiveByEmail(req.getEmail())).thenReturn(Optional.of(sampleUser));
        when(jwtTokenProvider.generateToken(anyString(), anyMap())).thenReturn("jwt-token");
        when(jwtTokenProvider.getExpirationMs()).thenReturn(86400000L);
        when(auditLogRepository.save(any())).thenReturn(null);

        AuthResponse resp = authService.login(req);

        assertThat(resp.getAccessToken()).isEqualTo("jwt-token");
    }

    @Test
    @DisplayName("login: bad credentials throws InvalidCredentialsException")
    void login_badCredentials_throws() {
        LoginRequest req = new LoginRequest();
        req.setEmail("john@example.com"); req.setPassword("wrong");

        doThrow(BadCredentialsException.class)
                .when(authenticationManager)
                .authenticate(any(UsernamePasswordAuthenticationToken.class));

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    // ---- sendOtp ----

    @Test
    @DisplayName("sendOtp: new phone creates placeholder user and sets OTP")
    void sendOtp_newPhone_createsUser() {
        SendOtpRequest req = new SendOtpRequest();
        req.setPhoneNumber("+919999999999");

        when(userRepository.findByPhoneNumber(req.getPhoneNumber())).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed-otp");

        authService.sendOtp(req);

        verify(userRepository, times(2)).save(any(User.class));
    }

    @Test
    @DisplayName("sendOtp: existing phone reuses user and refreshes OTP")
    void sendOtp_existingPhone_updatesOtp() {
        SendOtpRequest req = new SendOtpRequest();
        req.setPhoneNumber("+919999999999");

        when(userRepository.findByPhoneNumber(req.getPhoneNumber())).thenReturn(Optional.of(sampleUser));
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed-otp");

        authService.sendOtp(req);

        verify(userRepository, times(1)).save(sampleUser);
        assertThat(sampleUser.getOtpCode()).isEqualTo("hashed-otp");
    }

    // ---- verifyOtp ----

    @Test
    @DisplayName("verifyOtp: valid OTP returns JWT")
    void verifyOtp_success() {
        OtpLoginRequest req = new OtpLoginRequest();
        req.setPhoneNumber("+919999999999"); req.setOtp("123456");

        sampleUser.setOtpCode("hashed-otp");
        sampleUser.setOtpExpiresAt(LocalDateTime.now().plusMinutes(5));

        when(userRepository.findByPhoneNumber(req.getPhoneNumber())).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("123456", "hashed-otp")).thenReturn(true);
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);
        when(auditLogRepository.save(any())).thenReturn(null);
        when(jwtTokenProvider.generateToken(anyString(), anyMap())).thenReturn("jwt-otp");
        when(jwtTokenProvider.getExpirationMs()).thenReturn(86400000L);

        AuthResponse resp = authService.verifyOtp(req);

        assertThat(resp.getAccessToken()).isEqualTo("jwt-otp");
        assertThat(sampleUser.isPhoneVerified()).isTrue();
        assertThat(sampleUser.getOtpCode()).isNull();
    }

    @Test
    @DisplayName("verifyOtp: expired OTP throws OtpExpiredException")
    void verifyOtp_expired_throws() {
        OtpLoginRequest req = new OtpLoginRequest();
        req.setPhoneNumber("+919999999999"); req.setOtp("123456");

        sampleUser.setOtpCode("hashed-otp");
        sampleUser.setOtpExpiresAt(LocalDateTime.now().minusMinutes(1));

        when(userRepository.findByPhoneNumber(req.getPhoneNumber())).thenReturn(Optional.of(sampleUser));

        assertThatThrownBy(() -> authService.verifyOtp(req))
                .isInstanceOf(OtpExpiredException.class);
    }

    @Test
    @DisplayName("verifyOtp: wrong OTP throws InvalidOtpException")
    void verifyOtp_wrongOtp_throws() {
        OtpLoginRequest req = new OtpLoginRequest();
        req.setPhoneNumber("+919999999999"); req.setOtp("000000");

        sampleUser.setOtpCode("hashed-otp");
        sampleUser.setOtpExpiresAt(LocalDateTime.now().plusMinutes(5));

        when(userRepository.findByPhoneNumber(req.getPhoneNumber())).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("000000", "hashed-otp")).thenReturn(false);

        assertThatThrownBy(() -> authService.verifyOtp(req))
                .isInstanceOf(InvalidOtpException.class);
    }

    @Test
    @DisplayName("verifyOtp: user not found throws InvalidOtpException")
    void verifyOtp_userNotFound_throws() {
        OtpLoginRequest req = new OtpLoginRequest();
        req.setPhoneNumber("+910000000000"); req.setOtp("111111");

        when(userRepository.findByPhoneNumber(req.getPhoneNumber())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.verifyOtp(req))
                .isInstanceOf(InvalidOtpException.class);
    }

    // ---- socialLogin ----

    @Test
    @DisplayName("socialLogin: missing socialId throws InvalidCredentialsException")
    void socialLogin_missingSocialId_throws() {
        RegisterRequest req = new RegisterRequest();
        req.setAuthProvider(AuthProvider.GOOGLE);
        // socialId is null

        assertThatThrownBy(() -> authService.socialLogin(req))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessageContaining("socialId");
    }

    @Test
    @DisplayName("socialLogin: existing social account returns JWT")
    void socialLogin_existingAccount_returnsToken() {
        RegisterRequest req = new RegisterRequest();
        req.setEmail("john@example.com");
        req.setSocialId("google-123");
        req.setAuthProvider(AuthProvider.GOOGLE);

        when(userRepository.findBySocialIdAndAuthProvider("google-123", AuthProvider.GOOGLE))
                .thenReturn(Optional.of(sampleUser));
        when(auditLogRepository.save(any())).thenReturn(null);
        when(jwtTokenProvider.generateToken(anyString(), anyMap())).thenReturn("social-token");
        when(jwtTokenProvider.getExpirationMs()).thenReturn(86400000L);

        AuthResponse resp = authService.socialLogin(req);

        assertThat(resp.getAccessToken()).isEqualTo("social-token");
    }
}
