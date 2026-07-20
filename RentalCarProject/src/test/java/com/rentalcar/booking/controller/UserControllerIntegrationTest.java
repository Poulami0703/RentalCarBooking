package com.rentalcar.booking.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rentalcar.booking.dto.request.KycRequest;
import com.rentalcar.booking.dto.request.RegisterRequest;
import com.rentalcar.booking.dto.request.UpdateProfileRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class UserControllerIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    private String userToken;

    @BeforeEach
    void setUp() throws Exception {
        RegisterRequest reg = new RegisterRequest();
        reg.setFirstName("Priya"); reg.setLastName("Sharma");
        reg.setEmail("priya.user@test.com"); reg.setPassword("Test@1234");

        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reg)))
                .andReturn();

        // If already registered (test re-run), login instead
        if (result.getResponse().getStatus() == 409) {
            com.rentalcar.booking.dto.request.LoginRequest login = new com.rentalcar.booking.dto.request.LoginRequest();
            login.setEmail("priya.user@test.com"); login.setPassword("Test@1234");
            result = mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(login)))
                    .andReturn();
        }
        userToken = objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("accessToken").asText();
    }

    @Test
    @DisplayName("GET /users/me: authenticated user gets own profile")
    void getMyProfile_returns200() throws Exception {
        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("priya.user@test.com"));
    }

    @Test
    @DisplayName("GET /users/me: unauthenticated returns 401")
    void getMyProfile_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("PUT /users/me: updates profile fields")
    void updateProfile_returns200() throws Exception {
        UpdateProfileRequest req = new UpdateProfileRequest();
        req.setFirstName("Priyanka");
        req.setLastName("Sharma");
        req.setCity("Mumbai");
        req.setState("Maharashtra");
        req.setCountry("India");

        mockMvc.perform(put("/api/v1/users/me")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.firstName").value("Priyanka"))
                .andExpect(jsonPath("$.data.city").value("Mumbai"));
    }

    @Test
    @DisplayName("PUT /users/me: invalid phone number returns 400")
    void updateProfile_invalidPhone_returns400() throws Exception {
        UpdateProfileRequest req = new UpdateProfileRequest();
        req.setPhoneNumber("123"); // too short

        mockMvc.perform(put("/api/v1/users/me")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /users/me/kyc: valid KYC submission returns 200")
    void submitKyc_returns200() throws Exception {
        KycRequest req = new KycRequest();
        req.setDrivingLicenseNumber("KA0520230012345");
        req.setDrivingLicenseUrl("https://storage.example.com/dl/abc123.pdf");
        req.setLicenseExpiryDate("2030-01-01");

        mockMvc.perform(post("/api/v1/users/me/kyc")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.kycStatus").value("PENDING_REVIEW"));
    }

    @Test
    @DisplayName("POST /users/me/kyc: missing license number returns 400")
    void submitKyc_missingFields_returns400() throws Exception {
        KycRequest req = new KycRequest(); // empty fields

        mockMvc.perform(post("/api/v1/users/me/kyc")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }
}
