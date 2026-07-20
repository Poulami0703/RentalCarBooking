package com.rentalcar.booking.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;
    private WebRequest webRequest;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
        webRequest = new ServletWebRequest(new MockHttpServletRequest("GET", "/api/v1/test"));
    }

    @Test
    @DisplayName("DuplicateEmailException returns 409 CONFLICT")
    void handleDuplicateEmail() {
        var ex = new DuplicateEmailException("test@example.com");
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> resp = handler.handleDuplicateEmail(ex, webRequest);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(resp.getBody().message()).contains("already registered");
    }

    @Test
    @DisplayName("InvalidCredentialsException returns 401 UNAUTHORIZED")
    void handleInvalidCredentials() {
        var ex = new InvalidCredentialsException();
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> resp = handler.handleInvalidCredentials(ex, webRequest);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(resp.getBody().message()).contains("Invalid credentials");
    }

    @Test
    @DisplayName("BadCredentialsException returns 401 UNAUTHORIZED")
    void handleBadCredentials() {
        var ex = new BadCredentialsException("bad");
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> resp = handler.handleBadCredentials(ex, webRequest);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(resp.getBody().message()).contains("Invalid credentials");
    }

    @Test
    @DisplayName("ResourceNotFoundException returns 404 NOT_FOUND")
    void handleNotFound() {
        var ex = new ResourceNotFoundException("Vehicle", "id", 99L);
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> resp = handler.handleNotFound(ex, webRequest);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(resp.getBody().message()).contains("Vehicle");
    }

    @Test
    @DisplayName("NoVehiclesAvailableException returns 404 NOT_FOUND")
    void handleNoVehicles() {
        var ex = new NoVehiclesAvailableException();
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> resp = handler.handleNoVehicles(ex, webRequest);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(resp.getBody().message()).contains("No vehicles available");
    }

    @Test
    @DisplayName("VehicleNotAvailableException returns 409 CONFLICT")
    void handleVehicleUnavailable() {
        var ex = new VehicleNotAvailableException();
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> resp = handler.handleVehicleUnavailable(ex, webRequest);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(resp.getBody().message()).contains("reserved by another customer");
    }

    @Test
    @DisplayName("InvalidOtpException returns 400 BAD_REQUEST")
    void handleInvalidOtp() {
        var ex = new InvalidOtpException();
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> resp = handler.handleInvalidOtp(ex, webRequest);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resp.getBody().message()).contains("Invalid OTP");
    }

    @Test
    @DisplayName("OtpExpiredException returns 400 BAD_REQUEST")
    void handleOtpExpired() {
        var ex = new OtpExpiredException();
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> resp = handler.handleOtpExpired(ex, webRequest);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resp.getBody().message()).contains("expired");
    }

    @Test
    @DisplayName("BookingModificationException returns 400 BAD_REQUEST")
    void handleBookingModification() {
        var ex = new BookingModificationException("Cannot cancel completed booking");
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> resp = handler.handleBookingModification(ex, webRequest);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resp.getBody().message()).contains("Cannot cancel");
    }

    @Test
    @DisplayName("AccessDeniedException returns 403 FORBIDDEN")
    void handleAccessDenied() {
        var ex = new AccessDeniedException("Not your booking");
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> resp = handler.handleAccessDenied(ex, webRequest);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("PaymentException returns 400 BAD_REQUEST")
    void handlePayment() {
        var ex = new PaymentException("Payment failed");
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> resp = handler.handlePayment(ex, webRequest);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resp.getBody().message()).contains("Payment failed");
    }

    @Test
    @DisplayName("Generic Exception returns 500 INTERNAL_SERVER_ERROR")
    void handleGeneric() {
        var ex = new RuntimeException("Unexpected");
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> resp = handler.handleGeneric(ex, webRequest);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(resp.getBody().message()).contains("unexpected error");
    }
}
