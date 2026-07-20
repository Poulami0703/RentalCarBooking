package com.rentalcar.booking.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rentalcar.booking.dto.request.CreateReviewRequest;
import com.rentalcar.booking.dto.request.RegisterRequest;
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
class ReviewControllerIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    private String userToken;

    @BeforeEach
    void setUp() throws Exception {
        RegisterRequest reg = new RegisterRequest();
        reg.setFirstName("Ravi"); reg.setLastName("Kumar");
        reg.setEmail("ravi.review@test.com"); reg.setPassword("Test@1234");

        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reg)))
                .andReturn();

        if (result.getResponse().getStatus() == 409) {
            com.rentalcar.booking.dto.request.LoginRequest login = new com.rentalcar.booking.dto.request.LoginRequest();
            login.setEmail("ravi.review@test.com"); login.setPassword("Test@1234");
            result = mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(login)))
                    .andReturn();
        }
        userToken = objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("accessToken").asText();
    }

    @Test
    @DisplayName("GET /reviews/vehicle/{id}: returns 200 with empty page for unknown vehicle")
    void getVehicleReviews_unknownVehicle_returns200Empty() throws Exception {
        mockMvc.perform(get("/api/v1/reviews/vehicle/99999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray());
    }

    @Test
    @DisplayName("GET /reviews/vehicle/{id}: no auth required for public endpoint")
    void getVehicleReviews_noAuth_returns200() throws Exception {
        mockMvc.perform(get("/api/v1/reviews/vehicle/1"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /reviews/my: authenticated user gets own reviews")
    void getMyReviews_authenticated_returns200() throws Exception {
        mockMvc.perform(get("/api/v1/reviews/my")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray());
    }

    @Test
    @DisplayName("GET /reviews/my: unauthenticated returns 401")
    void getMyReviews_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/reviews/my"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /reviews: missing required fields returns 400")
    void createReview_missingFields_returns400() throws Exception {
        CreateReviewRequest req = new CreateReviewRequest(); // empty — missing vehicleId, bookingId, etc.

        mockMvc.perform(post("/api/v1/reviews")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /reviews: non-existent booking returns 404")
    void createReview_nonExistentBooking_returns404() throws Exception {
        CreateReviewRequest req = new CreateReviewRequest();
        req.setVehicleId(1L);
        req.setBookingId(99999L);
        req.setVehicleRating(5);
        req.setReviewText("Great car!");

        mockMvc.perform(post("/api/v1/reviews")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /reviews: unauthenticated returns 401")
    void createReview_unauthenticated_returns401() throws Exception {
        CreateReviewRequest req = new CreateReviewRequest();
        req.setVehicleId(1L);
        req.setBookingId(1L);
        req.setVehicleRating(4);
        req.setReviewText("Nice!");

        mockMvc.perform(post("/api/v1/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }
}
