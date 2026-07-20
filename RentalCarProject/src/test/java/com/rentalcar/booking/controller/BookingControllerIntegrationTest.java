package com.rentalcar.booking.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rentalcar.booking.dto.request.*;
import com.rentalcar.booking.enums.BookingType;
import com.rentalcar.booking.enums.CarType;
import com.rentalcar.booking.enums.FuelType;
import com.rentalcar.booking.enums.TransmissionType;
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
class BookingControllerIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    private String userToken;
    private Long vehicleId;

    @BeforeEach
    void setUp() throws Exception {
        // Register customer (idempotent — login if already exists)
        String email = "anjali.booking@test.com";
        RegisterRequest reg = new RegisterRequest();
        reg.setFirstName("Anjali"); reg.setLastName("Verma");
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
        userToken = objectMapper.readTree(regResult.getResponse().getContentAsString())
                .path("data").path("accessToken").asText();

        // Add a vehicle with a unique plate per test run to avoid duplicates
        String uniquePlate = "BK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        VehicleRequest vr = buildVehicleRequest(uniquePlate);
        MvcResult vResult = mockMvc.perform(post("/api/v1/admin/vehicles")
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(vr)))
                .andExpect(status().isCreated())
                .andReturn();
        vehicleId = objectMapper.readTree(vResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();
    }

    @Test
    @DisplayName("POST /bookings: create booking returns 201")
    void createBooking_returns201() throws Exception {
        CreateBookingRequest req = buildBookingRequest(vehicleId, 2);

        mockMvc.perform(post("/api/v1/bookings")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.data.bookingReference").isNotEmpty());
    }

    @Test
    @DisplayName("POST /bookings: unauthenticated returns 401")
    void createBooking_unauthenticated_returns401() throws Exception {
        CreateBookingRequest req = buildBookingRequest(vehicleId, 5);

        mockMvc.perform(post("/api/v1/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /bookings: non-existent vehicle returns 404")
    void createBooking_unknownVehicle_returns404() throws Exception {
        CreateBookingRequest req = buildBookingRequest(999999L, 2);

        mockMvc.perform(post("/api/v1/bookings")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /bookings: returns paged booking history")
    void getMyBookings_returns200() throws Exception {
        mockMvc.perform(get("/api/v1/bookings")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray());
    }

    @Test
    @DisplayName("GET /bookings/{ref}: unknown reference returns 404")
    void getBookingByReference_notFound_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/bookings/BK-UNKNOWN-REF")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /bookings/{id}/cancel: cancel existing booking returns 200")
    void cancelBooking_returns200() throws Exception {
        // Use a fresh unique vehicle for this test
        String plate = "CANCEL-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        MvcResult vResult = mockMvc.perform(post("/api/v1/admin/vehicles")
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildVehicleRequest(plate))))
                .andExpect(status().isCreated())
                .andReturn();
        Long cancelVehicleId = objectMapper.readTree(vResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        CreateBookingRequest req = buildBookingRequest(cancelVehicleId, 10);
        MvcResult created = mockMvc.perform(post("/api/v1/bookings")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();

        Long bookingId = objectMapper.readTree(created.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        mockMvc.perform(delete("/api/v1/bookings/" + bookingId + "/cancel")
                        .header("Authorization", "Bearer " + userToken)
                        .param("reason", "Changed my plans"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));
    }

    @Test
    @DisplayName("PUT /bookings/{id}: modify confirmed booking returns 200")
    void modifyBooking_returns200() throws Exception {
        String plate = "MOD-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        MvcResult vResult = mockMvc.perform(post("/api/v1/admin/vehicles")
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildVehicleRequest(plate))))
                .andExpect(status().isCreated())
                .andReturn();
        Long modVehicleId = objectMapper.readTree(vResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        MvcResult created = mockMvc.perform(post("/api/v1/bookings")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildBookingRequest(modVehicleId, 20))))
                .andExpect(status().isCreated())
                .andReturn();
        Long bookingId = objectMapper.readTree(created.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        CreateBookingRequest modify = buildBookingRequest(modVehicleId, 25);
        mockMvc.perform(put("/api/v1/bookings/" + bookingId)
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(modify)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("GET /bookings/{ref}: get booking by reference after create")
    void getBookingByReference_afterCreate_returns200() throws Exception {
        String plate = "REF-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        MvcResult vResult = mockMvc.perform(post("/api/v1/admin/vehicles")
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildVehicleRequest(plate))))
                .andExpect(status().isCreated())
                .andReturn();
        Long refVehicleId = objectMapper.readTree(vResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        MvcResult created = mockMvc.perform(post("/api/v1/bookings")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildBookingRequest(refVehicleId, 30))))
                .andExpect(status().isCreated())
                .andReturn();

        String ref = objectMapper.readTree(created.getResponse().getContentAsString())
                .path("data").path("bookingReference").asText();

        mockMvc.perform(get("/api/v1/bookings/" + ref)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.bookingReference").value(ref));
    }

    // ── helpers ────────────────────────────────────────────────────────────────

    private CreateBookingRequest buildBookingRequest(Long vehicleId, int daysFromNow) {
        CreateBookingRequest req = new CreateBookingRequest();
        req.setVehicleId(vehicleId);
        req.setPickupDateTime(LocalDateTime.now().plusDays(daysFromNow));
        req.setDropoffDateTime(LocalDateTime.now().plusDays(daysFromNow + 2));
        req.setPickupLocation("Bangalore Airport");
        req.setDropoffLocation("Bangalore Airport");
        req.setBookingType(BookingType.ROUND_TRIP);
        req.setChauffeurRequested(false);
        return req;
    }

    private VehicleRequest buildVehicleRequest(String plate) {
        VehicleRequest r = new VehicleRequest();
        r.setBrand("Maruti"); r.setModel("Swift"); r.setYear(2023);
        r.setLicensePlate(plate);
        r.setCarType(CarType.HATCHBACK); r.setFuelType(FuelType.PETROL);
        r.setTransmissionType(TransmissionType.MANUAL);
        r.setSeatingCapacity(5); r.setPricePerDay(new BigDecimal("1500.00"));
        r.setPricePerHour(new BigDecimal("100.00"));
        r.setCity("Bangalore"); r.setPickupLocation("Airport");
        r.setChauffeurAvailable(false);
        return r;
    }
}
