package com.rentalcar.booking.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rentalcar.booking.dto.request.RegisterRequest;
import com.rentalcar.booking.dto.request.VehicleRequest;
import com.rentalcar.booking.enums.CarType;
import com.rentalcar.booking.enums.FuelType;
import com.rentalcar.booking.enums.TransmissionType;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class VehicleControllerIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    // ── Public endpoints ──────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /vehicles/search: returns 200 with paged results (or 404 if none available)")
    void searchVehicles_returns2xx() throws Exception {
        mockMvc.perform(get("/api/v1/vehicles/search"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    if (status != 200 && status != 404) {
                        throw new AssertionError("Expected 200 or 404 but got " + status);
                    }
                });
    }

    @Test
    @DisplayName("GET /vehicles/{id}: unknown id returns 404")
    void getVehicleById_notFound_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/vehicles/99999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /vehicles/cities: returns 200 with list")
    void getCities_returns200() throws Exception {
        mockMvc.perform(get("/api/v1/vehicles/cities"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("GET /vehicles/brands: returns 200 with list")
    void getBrands_returns200() throws Exception {
        mockMvc.perform(get("/api/v1/vehicles/brands"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    // ── Admin vehicle CRUD (using @WithMockUser ADMIN role) ───────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /admin/vehicles: admin adds new vehicle returns 201")
    void addVehicle_admin_returns201() throws Exception {
        VehicleRequest req = buildVehicleRequest("KA05IT1001");

        mockMvc.perform(post("/api/v1/admin/vehicles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.brand").value("Hyundai"))
                .andExpect(jsonPath("$.data.model").value("Creta"))
                .andExpect(jsonPath("$.data.status").value("AVAILABLE"))
                .andExpect(jsonPath("$.data.city").value("Bangalore"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /admin/vehicles then GET: vehicle is retrievable by id")
    void addVehicle_thenGet_returnsVehicle() throws Exception {
        VehicleRequest req = buildVehicleRequest("KA05IT2002");

        MvcResult result = mockMvc.perform(post("/api/v1/admin/vehicles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();

        int id = objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("id").asInt();

        mockMvc.perform(get("/api/v1/vehicles/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.licensePlate").value("KA05IT2002"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /admin/vehicles: duplicate plate returns 409")
    void addVehicle_duplicatePlate_returns409() throws Exception {
        VehicleRequest req = buildVehicleRequest("DUP-PLATE-01");

        // First add
        mockMvc.perform(post("/api/v1/admin/vehicles")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)));

        // Duplicate
        mockMvc.perform(post("/api/v1/admin/vehicles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("DELETE /admin/vehicles/{id}: retires vehicle")
    void deleteVehicle_setsRetired() throws Exception {
        VehicleRequest req = buildVehicleRequest("RET-PLATE-01");

        MvcResult result = mockMvc.perform(post("/api/v1/admin/vehicles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andReturn();

        int id = objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("id").asInt();

        mockMvc.perform(delete("/api/v1/admin/vehicles/" + id))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/vehicles/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("RETIRED"));
    }

    @Test
    @DisplayName("POST /admin/vehicles: unauthenticated returns 401")
    void addVehicle_unauthenticated_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/admin/vehicles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildVehicleRequest("UNAUTH-01"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    @DisplayName("POST /admin/vehicles: CUSTOMER role returns 403")
    void addVehicle_customerRole_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/admin/vehicles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildVehicleRequest("CUST-01"))))
                .andExpect(status().isForbidden());
    }

    // ── helper ────────────────────────────────────────────────────────────────

    private VehicleRequest buildVehicleRequest(String plate) {
        VehicleRequest r = new VehicleRequest();
        r.setBrand("Hyundai"); r.setModel("Creta"); r.setYear(2024);
        r.setLicensePlate(plate);
        r.setCarType(CarType.SUV); r.setFuelType(FuelType.PETROL);
        r.setTransmissionType(TransmissionType.AUTOMATIC);
        r.setSeatingCapacity(5); r.setPricePerDay(new BigDecimal("3200.00"));
        r.setPricePerHour(new BigDecimal("220.00"));
        r.setCity("Bangalore"); r.setPickupLocation("Whitefield");
        r.setChauffeurAvailable(false);
        return r;
    }
}
