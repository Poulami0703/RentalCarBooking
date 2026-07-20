package com.rentalcar.booking.service;

import com.rentalcar.booking.dto.request.VehicleRequest;
import com.rentalcar.booking.dto.request.VehicleSearchRequest;
import com.rentalcar.booking.dto.response.VehicleResponse;
import com.rentalcar.booking.entity.Vehicle;
import com.rentalcar.booking.enums.CarType;
import com.rentalcar.booking.enums.FuelType;
import com.rentalcar.booking.enums.TransmissionType;
import com.rentalcar.booking.enums.VehicleStatus;
import com.rentalcar.booking.exception.NoVehiclesAvailableException;
import com.rentalcar.booking.exception.ResourceNotFoundException;
import com.rentalcar.booking.exception.VehicleNotAvailableException;
import com.rentalcar.booking.repository.VehicleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VehicleServiceTest {

    @Mock VehicleRepository vehicleRepository;
    @InjectMocks VehicleService vehicleService;

    private Vehicle sampleVehicle;

    @BeforeEach
    void setUp() {
        sampleVehicle = Vehicle.builder()
                .id(1L)
                .brand("Toyota").model("Fortuner").year(2023)
                .licensePlate("MH01AB1234")
                .carType(CarType.SUV).fuelType(FuelType.DIESEL)
                .transmissionType(TransmissionType.AUTOMATIC)
                .seatingCapacity(7)
                .pricePerDay(new BigDecimal("4500.00"))
                .pricePerHour(new BigDecimal("300.00"))
                .city("Mumbai").pickupLocation("Andheri West")
                .status(VehicleStatus.AVAILABLE)
                .averageRating(0.0).totalRatings(0).totalBookings(0)
                .chauffeurAvailable(false)
                .images(new ArrayList<>())
                .bookings(new ArrayList<>())
                .reviews(new ArrayList<>())
                .availabilitySlots(new ArrayList<>())
                .build();
    }

    // ---- getVehicleById ----

    @Test
    @DisplayName("getVehicleById: existing id returns VehicleResponse")
    void getVehicleById_success() {
        when(vehicleRepository.findById(1L)).thenReturn(Optional.of(sampleVehicle));

        VehicleResponse resp = vehicleService.getVehicleById(1L);

        assertThat(resp.getBrand()).isEqualTo("Toyota");
        assertThat(resp.getModel()).isEqualTo("Fortuner");
        assertThat(resp.getStatus()).isEqualTo(VehicleStatus.AVAILABLE);
    }

    @Test
    @DisplayName("getVehicleById: unknown id throws ResourceNotFoundException")
    void getVehicleById_notFound() {
        when(vehicleRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> vehicleService.getVehicleById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Vehicle");
    }

    // ---- searchVehicles ----

    @Test
    @DisplayName("searchVehicles: results found returns page")
    void searchVehicles_found() {
        VehicleSearchRequest req = new VehicleSearchRequest();
        req.setCity("Mumbai");
        req.setPickupDate(LocalDateTime.now().plusDays(1));
        req.setDropoffDate(LocalDateTime.now().plusDays(3));

        Page<Vehicle> page = new PageImpl<>(List.of(sampleVehicle));
        when(vehicleRepository.searchAvailableVehicles(
                anyString(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(page);

        Page<VehicleResponse> result = vehicleService.searchVehicles(req);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getBrand()).isEqualTo("Toyota");
    }

    @Test
    @DisplayName("searchVehicles: no results throws NoVehiclesAvailableException")
    void searchVehicles_noResults() {
        VehicleSearchRequest req = new VehicleSearchRequest();
        Page<Vehicle> empty = Page.empty();
        when(vehicleRepository.searchAvailableVehicles(
                any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(empty);

        assertThatThrownBy(() -> vehicleService.searchVehicles(req))
                .isInstanceOf(NoVehiclesAvailableException.class);
    }

    // ---- createVehicle ----

    @Test
    @DisplayName("createVehicle: new license plate saves and returns response")
    void createVehicle_success() {
        VehicleRequest req = buildVehicleRequest("MH01AB9999");

        when(vehicleRepository.existsByLicensePlate("MH01AB9999")).thenReturn(false);
        when(vehicleRepository.save(any(Vehicle.class))).thenReturn(sampleVehicle);

        VehicleResponse resp = vehicleService.createVehicle(req);

        assertThat(resp.getBrand()).isEqualTo("Toyota");
        verify(vehicleRepository).save(any(Vehicle.class));
    }

    @Test
    @DisplayName("createVehicle: duplicate license plate throws VehicleNotAvailableException")
    void createVehicle_duplicatePlate_throws() {
        VehicleRequest req = buildVehicleRequest("MH01AB1234");
        when(vehicleRepository.existsByLicensePlate("MH01AB1234")).thenReturn(true);

        assertThatThrownBy(() -> vehicleService.createVehicle(req))
                .isInstanceOf(VehicleNotAvailableException.class);
    }

    // ---- updateVehicle ----

    @Test
    @DisplayName("updateVehicle: valid id updates fields")
    void updateVehicle_success() {
        VehicleRequest req = buildVehicleRequest("MH01AB1234");
        req.setPricePerDay(new BigDecimal("5000.00"));

        when(vehicleRepository.findById(1L)).thenReturn(Optional.of(sampleVehicle));
        when(vehicleRepository.save(any(Vehicle.class))).thenReturn(sampleVehicle);

        VehicleResponse resp = vehicleService.updateVehicle(1L, req);

        assertThat(resp).isNotNull();
        verify(vehicleRepository).save(sampleVehicle);
    }

    @Test
    @DisplayName("updateVehicle: unknown id throws ResourceNotFoundException")
    void updateVehicle_notFound() {
        when(vehicleRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> vehicleService.updateVehicle(99L, buildVehicleRequest("X")))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ---- deleteVehicle (soft retire) ----

    @Test
    @DisplayName("deleteVehicle: sets status to RETIRED")
    void deleteVehicle_success() {
        when(vehicleRepository.findById(1L)).thenReturn(Optional.of(sampleVehicle));
        when(vehicleRepository.save(any(Vehicle.class))).thenReturn(sampleVehicle);

        vehicleService.deleteVehicle(1L);

        assertThat(sampleVehicle.getStatus()).isEqualTo(VehicleStatus.RETIRED);
    }

    // ---- updateVehicleStatus ----

    @Test
    @DisplayName("updateVehicleStatus: changes status correctly")
    void updateVehicleStatus_success() {
        when(vehicleRepository.findById(1L)).thenReturn(Optional.of(sampleVehicle));
        when(vehicleRepository.save(any(Vehicle.class))).thenReturn(sampleVehicle);

        VehicleResponse resp = vehicleService.updateVehicleStatus(1L, VehicleStatus.UNDER_MAINTENANCE);

        assertThat(sampleVehicle.getStatus()).isEqualTo(VehicleStatus.UNDER_MAINTENANCE);
    }

    // ---- getAllCities / getAllBrands ----

    @Test
    @DisplayName("getAllCities: returns list from repository")
    void getAllCities_success() {
        when(vehicleRepository.findAllCities()).thenReturn(List.of("Mumbai", "Delhi"));

        List<String> cities = vehicleService.getAllCities();

        assertThat(cities).containsExactly("Mumbai", "Delhi");
    }

    @Test
    @DisplayName("getAllBrands: returns list from repository")
    void getAllBrands_success() {
        when(vehicleRepository.findAllBrands()).thenReturn(List.of("Toyota", "Honda"));

        List<String> brands = vehicleService.getAllBrands();

        assertThat(brands).containsExactly("Toyota", "Honda");
    }

    // ---- helper ----

    private VehicleRequest buildVehicleRequest(String plate) {
        VehicleRequest r = new VehicleRequest();
        r.setBrand("Toyota"); r.setModel("Fortuner"); r.setYear(2023);
        r.setLicensePlate(plate);
        r.setCarType(CarType.SUV); r.setFuelType(FuelType.DIESEL);
        r.setTransmissionType(TransmissionType.AUTOMATIC);
        r.setSeatingCapacity(7); r.setPricePerDay(new BigDecimal("4500.00"));
        r.setCity("Mumbai"); r.setPickupLocation("Andheri");
        r.setChauffeurAvailable(false);
        return r;
    }
}
