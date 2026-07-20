package com.rentalcar.booking.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rentalcar.booking.dto.request.*;
import com.rentalcar.booking.enums.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
class AdminControllerIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    // A real customer JWT for booking flows
    private String customerToken;

    @BeforeEach
    void setUp() throws Exception {
        String email = "admin.ctrl.test@test.com";
        RegisterRequest reg = new RegisterRequest();
        reg.setFirstName("Admin"); reg.setLastName("Test");
        reg.setEmail(email); reg.setPassword("Test@1234");
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reg)))
                .andReturn();
        if (result.getResponse().getStatus() == 409) {
            LoginRequest login = new LoginRequest();
            login.setEmail(email); login.setPassword("Test@1234");
            result = mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(login)))
                    .andReturn();
        }
        customerToken = objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("accessToken").asText();
    }

    // ── Dashboard ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /admin/dashboard: returns 200 with stats")
    void getDashboard_returns200() throws Exception {
        mockMvc.perform(get("/api/v1/admin/dashboard")
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalBookings").isNumber())
                .andExpect(jsonPath("$.data.totalVehicles").isNumber())
                .andExpect(jsonPath("$.data.totalCustomers").isNumber());
    }

    @Test
    @DisplayName("GET /admin/dashboard: unauthenticated returns 401")
    void getDashboard_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/admin/dashboard"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    @DisplayName("GET /admin/dashboard: CUSTOMER role returns 403")
    void getDashboard_customerRole_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/admin/dashboard"))
                .andExpect(status().isForbidden());
    }

    // ── Vehicle Management ──────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /admin/vehicles: returns paged vehicle list")
    void getAllVehicles_returns200() throws Exception {
        mockMvc.perform(get("/api/v1/admin/vehicles")
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray());
    }

    @Test
    @DisplayName("PUT /admin/vehicles/{id}: update vehicle returns 200")
    void updateVehicle_returns200() throws Exception {
        VehicleRequest vr = buildVehicleRequest("ADMIN-UPDATE-" + UUID.randomUUID().toString().substring(0, 6));
        MvcResult addResult = mockMvc.perform(post("/api/v1/admin/vehicles")
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(vr)))
                .andExpect(status().isCreated())
                .andReturn();
        Long id = objectMapper.readTree(addResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        vr.setBrand("Honda"); vr.setModel("City");
        mockMvc.perform(put("/api/v1/admin/vehicles/" + id)
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(vr)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.brand").value("Honda"))
                .andExpect(jsonPath("$.data.model").value("City"));
    }

    @Test
    @DisplayName("PATCH /admin/vehicles/{id}/status: update status to UNDER_MAINTENANCE returns 200")
    void updateVehicleStatus_returns200() throws Exception {
        VehicleRequest vr = buildVehicleRequest("ADMIN-STATUS-" + UUID.randomUUID().toString().substring(0, 6));
        MvcResult addResult = mockMvc.perform(post("/api/v1/admin/vehicles")
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(vr)))
                .andExpect(status().isCreated())
                .andReturn();
        Long id = objectMapper.readTree(addResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        mockMvc.perform(patch("/api/v1/admin/vehicles/" + id + "/status")
                        .with(user("admin").roles("ADMIN"))
                        .param("status", "UNDER_MAINTENANCE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("UNDER_MAINTENANCE"));
    }

    // ── User Management ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /admin/users: returns paged user list")
    void getAllUsers_returns200() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users")
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray());
    }

    @Test
    @DisplayName("GET /admin/users/{id}: unknown id returns 404")
    void getUserById_notFound_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users/99999")
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /admin/users/{id}: known id returns user")
    void getUserById_known_returns200() throws Exception {
        String email = "kyc.admin.test@test.com";
        RegisterRequest reg = new RegisterRequest();
        reg.setFirstName("KYC"); reg.setLastName("TestUser");
        reg.setEmail(email); reg.setPassword("Test@1234");
        MvcResult regResult = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reg)))
                .andReturn();
        if (regResult.getResponse().getStatus() == 409) {
            LoginRequest login = new LoginRequest();
            login.setEmail(email); login.setPassword("Test@1234");
            regResult = mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(login)))
                    .andReturn();
        }
        Long userId = objectMapper.readTree(regResult.getResponse().getContentAsString())
                .path("data").path("user").path("id").asLong();

        mockMvc.perform(get("/api/v1/admin/users/" + userId)
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value(email));
    }

    @Test
    @DisplayName("PATCH /admin/users/{id}/kyc/approve: approves KYC")
    void approveKyc_returns200() throws Exception {
        String email = "kyc.approve@test.com";
        RegisterRequest reg = new RegisterRequest();
        reg.setFirstName("KYCApprove"); reg.setLastName("User");
        reg.setEmail(email); reg.setPassword("Test@1234");
        MvcResult regResult = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reg)))
                .andReturn();
        if (regResult.getResponse().getStatus() == 409) {
            LoginRequest login = new LoginRequest();
            login.setEmail(email); login.setPassword("Test@1234");
            regResult = mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(login)))
                    .andReturn();
        }
        Long userId = objectMapper.readTree(regResult.getResponse().getContentAsString())
                .path("data").path("user").path("id").asLong();
        String token = objectMapper.readTree(regResult.getResponse().getContentAsString())
                .path("data").path("accessToken").asText();

        // Submit KYC first
        KycRequest kyc = new KycRequest();
        kyc.setDrivingLicenseNumber("MH0120230001"); kyc.setDrivingLicenseUrl("https://cdn.example.com/dl.pdf");
        mockMvc.perform(post("/api/v1/users/me/kyc")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(kyc)));

        // Admin approves KYC
        mockMvc.perform(patch("/api/v1/admin/users/" + userId + "/kyc/approve")
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.kycStatus").value("APPROVED"));
    }

    @Test
    @DisplayName("PATCH /admin/users/{id}/kyc/reject: rejects KYC")
    void rejectKyc_returns200() throws Exception {
        String email = "kyc.reject@test.com";
        RegisterRequest reg = new RegisterRequest();
        reg.setFirstName("KYCReject"); reg.setLastName("User");
        reg.setEmail(email); reg.setPassword("Test@1234");
        MvcResult regResult = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reg)))
                .andReturn();
        if (regResult.getResponse().getStatus() == 409) {
            LoginRequest login = new LoginRequest();
            login.setEmail(email); login.setPassword("Test@1234");
            regResult = mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(login)))
                    .andReturn();
        }
        Long userId = objectMapper.readTree(regResult.getResponse().getContentAsString())
                .path("data").path("user").path("id").asLong();

        mockMvc.perform(patch("/api/v1/admin/users/" + userId + "/kyc/reject")
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.kycStatus").value("REJECTED"));
    }

    @Test
    @DisplayName("PATCH /admin/users/{id}/disable: disables user account")
    void disableUser_returns200() throws Exception {
        String email = "disable.me@test.com";
        RegisterRequest reg = new RegisterRequest();
        reg.setFirstName("Disable"); reg.setLastName("Me");
        reg.setEmail(email); reg.setPassword("Test@1234");
        MvcResult regResult = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reg)))
                .andReturn();
        if (regResult.getResponse().getStatus() == 409) {
            LoginRequest login = new LoginRequest();
            login.setEmail(email); login.setPassword("Test@1234");
            regResult = mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(login)))
                    .andReturn();
        }
        Long userId = objectMapper.readTree(regResult.getResponse().getContentAsString())
                .path("data").path("user").path("id").asLong();

        mockMvc.perform(patch("/api/v1/admin/users/" + userId + "/disable")
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk());
    }

    // ── Booking Management ──────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /admin/bookings: returns paged bookings")
    void getAllBookings_returns200() throws Exception {
        mockMvc.perform(get("/api/v1/admin/bookings")
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray());
    }

    @Test
    @DisplayName("PATCH /admin/bookings/{id}/approve: unknown id returns 404")
    void approveBooking_notFound_returns404() throws Exception {
        mockMvc.perform(patch("/api/v1/admin/bookings/99999/approve")
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PATCH /admin/bookings/{id}/complete: approve then complete lifecycle")
    void approveAndCompleteBooking_lifecycle() throws Exception {
        // Create vehicle
        String plate = "LIFECYCLE-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        MvcResult vResult = mockMvc.perform(post("/api/v1/admin/vehicles")
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildVehicleRequest(plate))))
                .andExpect(status().isCreated())
                .andReturn();
        Long vehicleId = objectMapper.readTree(vResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        // Create booking as customer
        CreateBookingRequest bReq = buildBookingRequest(vehicleId, 5);
        MvcResult bResult = mockMvc.perform(post("/api/v1/bookings")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bReq)))
                .andExpect(status().isCreated())
                .andReturn();
        Long bId = objectMapper.readTree(bResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        // Admin approves
        mockMvc.perform(patch("/api/v1/admin/bookings/" + bId + "/approve")
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));

        // Admin completes
        mockMvc.perform(patch("/api/v1/admin/bookings/" + bId + "/complete")
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));
    }

    // ── Review Management ───────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /admin/reviews/pending: returns pending reviews")
    void getPendingReviews_returns200() throws Exception {
        mockMvc.perform(get("/api/v1/admin/reviews/pending")
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray());
    }

    @Test
    @DisplayName("PATCH /admin/reviews/{id}/approve: unknown id returns 404")
    void approveReview_notFound_returns404() throws Exception {
        mockMvc.perform(patch("/api/v1/admin/reviews/99999/approve")
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /admin/reviews/{id}: unknown id returns 404")
    void deleteReview_notFound_returns404() throws Exception {
        mockMvc.perform(delete("/api/v1/admin/reviews/99999")
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /admin/bookings/{id}/refund: no payment for booking returns 404")
    void processRefund_noPayment_returns404() throws Exception {
        String plate = "REFUND-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        MvcResult vResult = mockMvc.perform(post("/api/v1/admin/vehicles")
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildVehicleRequest(plate))))
                .andExpect(status().isCreated())
                .andReturn();
        Long vehicleId = objectMapper.readTree(vResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        // Create booking but don't pay
        MvcResult bResult = mockMvc.perform(post("/api/v1/bookings")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildBookingRequest(vehicleId, 8))))
                .andExpect(status().isCreated())
                .andReturn();
        Long bId = objectMapper.readTree(bResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        mockMvc.perform(post("/api/v1/admin/bookings/" + bId + "/refund")
                        .with(user("admin").roles("ADMIN"))
                        .param("amount", "500.00"))
                .andExpect(status().isNotFound());
    }

    // ── helpers ─────────────────────────────────────────────────────────────────

    private CreateBookingRequest buildBookingRequest(Long vehicleId, int daysFromNow) {
        CreateBookingRequest req = new CreateBookingRequest();
        req.setVehicleId(vehicleId);
        req.setPickupDateTime(LocalDateTime.now().plusDays(daysFromNow));
        req.setDropoffDateTime(LocalDateTime.now().plusDays(daysFromNow + 2));
        req.setPickupLocation("Delhi Airport");
        req.setDropoffLocation("Delhi Airport");
        req.setBookingType(BookingType.ROUND_TRIP);
        req.setChauffeurRequested(false);
        return req;
    }

    private VehicleRequest buildVehicleRequest(String plate) {
        VehicleRequest r = new VehicleRequest();
        r.setBrand("Tata"); r.setModel("Nexon"); r.setYear(2023);
        r.setLicensePlate(plate);
        r.setCarType(CarType.SUV); r.setFuelType(FuelType.ELECTRIC);
        r.setTransmissionType(TransmissionType.AUTOMATIC);
        r.setSeatingCapacity(5); r.setPricePerDay(new BigDecimal("2800.00"));
        r.setPricePerHour(new BigDecimal("180.00"));
        r.setCity("Delhi"); r.setPickupLocation("Airport");
        r.setChauffeurAvailable(false);
        return r;
    }
}
