package com.rentalcar.booking.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rentalcar.booking.dto.request.LoginRequest;
import com.rentalcar.booking.dto.request.OtpLoginRequest;
import com.rentalcar.booking.dto.request.RegisterRequest;
import com.rentalcar.booking.dto.request.SendOtpRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class AuthControllerIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    // ---- register ----

    @Test
    @DisplayName("POST /auth/register: valid payload returns 201 with JWT")
    void register_validPayload_returns201() throws Exception {
        RegisterRequest req = new RegisterRequest();
        req.setFirstName("Alice"); req.setLastName("Smith");
        req.setEmail("alice.smith@test.com"); req.setPassword("Test@1234");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.user.email").value("alice.smith@test.com"));
    }

    @Test
    @DisplayName("POST /auth/register: duplicate email returns 409")
    void register_duplicateEmail_returns409() throws Exception {
        RegisterRequest req = new RegisterRequest();
        req.setFirstName("Bob"); req.setLastName("Jones");
        req.setEmail("duplicate@test.com"); req.setPassword("Test@1234");

        // Register once
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)));

        // Try again
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("already registered")));
    }

    @Test
    @DisplayName("POST /auth/register: missing email returns 400")
    void register_missingEmail_returns400() throws Exception {
        RegisterRequest req = new RegisterRequest();
        req.setFirstName("No"); req.setLastName("Email");
        req.setPassword("Test@1234");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    // ---- login ----

    @Test
    @DisplayName("POST /auth/login: valid credentials return 200 with JWT")
    void login_valid_returns200() throws Exception {
        // First register
        RegisterRequest reg = new RegisterRequest();
        reg.setFirstName("Charlie"); reg.setLastName("Brown");
        reg.setEmail("charlie@test.com"); reg.setPassword("Test@1234");
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(reg)));

        // Then login
        LoginRequest login = new LoginRequest();
        login.setEmail("charlie@test.com"); login.setPassword("Test@1234");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty());
    }

    @Test
    @DisplayName("POST /auth/login: wrong password returns 401")
    void login_wrongPassword_returns401() throws Exception {
        // Register
        RegisterRequest reg = new RegisterRequest();
        reg.setFirstName("Dave"); reg.setLastName("Test");
        reg.setEmail("dave@test.com"); reg.setPassword("Test@1234");
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(reg)));

        LoginRequest login = new LoginRequest();
        login.setEmail("dave@test.com"); login.setPassword("WrongPass");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /auth/login: missing fields returns 400")
    void login_missingFields_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    // ---- OTP ----

    @Test
    @DisplayName("POST /auth/otp/send: valid phone returns 200")
    void sendOtp_valid_returns200() throws Exception {
        SendOtpRequest req = new SendOtpRequest();
        req.setPhoneNumber("+919876543210");

        mockMvc.perform(post("/api/v1/auth/otp/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("POST /auth/otp/verify: wrong OTP returns 400")
    void verifyOtp_wrongOtp_returns400() throws Exception {
        // Send OTP first
        SendOtpRequest send = new SendOtpRequest();
        send.setPhoneNumber("+911234567890");
        mockMvc.perform(post("/api/v1/auth/otp/send")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(send)));

        // Verify with wrong OTP
        OtpLoginRequest verify = new OtpLoginRequest();
        verify.setPhoneNumber("+911234567890"); verify.setOtp("000000");

        mockMvc.perform(post("/api/v1/auth/otp/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(verify)))
                .andExpect(status().isBadRequest());
    }
}
