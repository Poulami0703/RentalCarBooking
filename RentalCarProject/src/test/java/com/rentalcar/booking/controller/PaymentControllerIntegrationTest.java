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
class PaymentControllerIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    private String userToken;
    private Long bookingId;

    @BeforeEach
    void setUp() throws Exception {
        // Register customer and get JWT
        String email = "suresh.pay@test.com";
        RegisterRequest reg = new RegisterRequest();
        reg.setFirstName("Suresh"); reg.setLastName("Patel");
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

        // Add a fresh vehicle with unique plate
        String plate = "PAY-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        MvcResult vResult = mockMvc.perform(post("/api/v1/admin/vehicles")
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildVehicleRequest(plate))))
                .andExpect(status().isCreated())
                .andReturn();
        Long vehicleId = objectMapper.readTree(vResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        // Create a booking
        CreateBookingRequest bReq = buildBookingRequest(vehicleId, 5);
        MvcResult bResult = mockMvc.perform(post("/api/v1/bookings")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bReq)))
                .andExpect(status().isCreated())
                .andReturn();
        bookingId = objectMapper.readTree(bResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();
    }

    @Test
    @DisplayName("POST /payments: initiate payment returns 201")
    void initiatePayment_returns201() throws Exception {
        PaymentRequest req = new PaymentRequest();
        req.setBookingId(bookingId);
        req.setPaymentMethod(PaymentMethod.UPI);
        req.setUpiId("suresh@ybl");

        mockMvc.perform(post("/api/v1/payments")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.transactionId").isNotEmpty());
    }

    @Test
    @DisplayName("POST /payments: unauthenticated returns 401")
    void initiatePayment_unauthenticated_returns401() throws Exception {
        PaymentRequest req = new PaymentRequest();
        req.setBookingId(bookingId);
        req.setPaymentMethod(PaymentMethod.CREDIT_CARD);

        mockMvc.perform(post("/api/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /payments: missing required fields returns 400")
    void initiatePayment_missingFields_returns400() throws Exception {
        PaymentRequest req = new PaymentRequest(); // missing bookingId and paymentMethod

        mockMvc.perform(post("/api/v1/payments")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /payments/booking/{id}: unknown booking returns 404")
    void getPaymentByBooking_notFound_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/payments/booking/99999")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /payments/booking/{id}: returns payment after initiation")
    void getPaymentByBooking_afterInitiate_returns200() throws Exception {
        // Create a fresh vehicle + booking for this isolated test
        String plate = "PAYQ-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        MvcResult vResult = mockMvc.perform(post("/api/v1/admin/vehicles")
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildVehicleRequest(plate))))
                .andExpect(status().isCreated())
                .andReturn();
        Long newVehicleId = objectMapper.readTree(vResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        MvcResult bResult = mockMvc.perform(post("/api/v1/bookings")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildBookingRequest(newVehicleId, 15))))
                .andExpect(status().isCreated())
                .andReturn();
        Long newBookingId = objectMapper.readTree(bResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        // Pay
        PaymentRequest payReq = new PaymentRequest();
        payReq.setBookingId(newBookingId);
        payReq.setPaymentMethod(PaymentMethod.DEBIT_CARD);
        mockMvc.perform(post("/api/v1/payments")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payReq)))
                .andExpect(status().isCreated());

        // Retrieve payment
        mockMvc.perform(get("/api/v1/payments/booking/" + newBookingId)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.paymentMethod").value("DEBIT_CARD"));
    }

    // ── helpers ────────────────────────────────────────────────────────────────

    private CreateBookingRequest buildBookingRequest(Long vehicleId, int daysFromNow) {
        CreateBookingRequest req = new CreateBookingRequest();
        req.setVehicleId(vehicleId);
        req.setPickupDateTime(LocalDateTime.now().plusDays(daysFromNow));
        req.setDropoffDateTime(LocalDateTime.now().plusDays(daysFromNow + 3));
        req.setPickupLocation("Chennai Airport");
        req.setBookingType(BookingType.ONE_WAY);
        req.setChauffeurRequested(false);
        return req;
    }

    private VehicleRequest buildVehicleRequest(String plate) {
        VehicleRequest r = new VehicleRequest();
        r.setBrand("Toyota"); r.setModel("Innova"); r.setYear(2022);
        r.setLicensePlate(plate);
        r.setCarType(CarType.SUV); r.setFuelType(FuelType.DIESEL);
        r.setTransmissionType(TransmissionType.AUTOMATIC);
        r.setSeatingCapacity(7); r.setPricePerDay(new BigDecimal("4500.00"));
        r.setPricePerHour(new BigDecimal("300.00"));
        r.setCity("Chennai"); r.setPickupLocation("Airport");
        r.setChauffeurAvailable(false);
        return r;
    }
}
