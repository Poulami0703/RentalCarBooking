package com.rentalcar.booking.service;

import com.rentalcar.booking.dto.request.VehicleRequest;
import com.rentalcar.booking.dto.request.VehicleSearchRequest;
import com.rentalcar.booking.dto.response.VehicleImageResponse;
import com.rentalcar.booking.dto.response.VehicleResponse;
import com.rentalcar.booking.entity.Vehicle;
import com.rentalcar.booking.entity.VehicleImage;
import com.rentalcar.booking.enums.VehicleStatus;
import com.rentalcar.booking.exception.ResourceNotFoundException;
import com.rentalcar.booking.exception.NoVehiclesAvailableException;
import com.rentalcar.booking.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class VehicleService {

    private static final Logger log = LoggerFactory.getLogger(VehicleService.class);

    private final VehicleRepository vehicleRepository;

    @Transactional(readOnly = true)
    public Page<VehicleResponse> searchVehicles(VehicleSearchRequest request) {
        Sort sort = request.getSortDir().equalsIgnoreCase("desc")
                ? Sort.by(request.getSortBy()).descending()
                : Sort.by(request.getSortBy()).ascending();
        Pageable pageable = PageRequest.of(request.getPage(), request.getSize(), sort);

        Page<Vehicle> vehicles = vehicleRepository.searchAvailableVehicles(
                request.getCity(),
                request.getCarType(),
                request.getFuelType(),
                request.getTransmissionType(),
                request.getBrand(),
                request.getMinPrice(),
                request.getMaxPrice(),
                request.getMinSeats(),
                request.getPickupDate(),
                request.getDropoffDate(),
                pageable
        );

        if (vehicles.isEmpty()) {
            throw new NoVehiclesAvailableException();
        }

        return vehicles.map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public VehicleResponse getVehicleById(Long id) {
        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle", "id", id));
        return mapToResponse(vehicle);
    }

    @Transactional
    public VehicleResponse createVehicle(VehicleRequest request) {
        if (vehicleRepository.existsByLicensePlate(request.getLicensePlate())) {
            throw new com.rentalcar.booking.exception.VehicleNotAvailableException(
                    "A vehicle with license plate " + request.getLicensePlate() + " already exists.");
        }

        Vehicle vehicle = buildVehicle(request);
        vehicle = vehicleRepository.save(vehicle);
        log.info("New vehicle created: {} {}", vehicle.getBrand(), vehicle.getModel());
        return mapToResponse(vehicle);
    }

    @Transactional
    public VehicleResponse updateVehicle(Long id, VehicleRequest request) {
        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle", "id", id));

        vehicle.setBrand(request.getBrand());
        vehicle.setModel(request.getModel());
        vehicle.setYear(request.getYear());
        vehicle.setCarType(request.getCarType());
        vehicle.setFuelType(request.getFuelType());
        vehicle.setTransmissionType(request.getTransmissionType());
        vehicle.setSeatingCapacity(request.getSeatingCapacity());
        vehicle.setPricePerDay(request.getPricePerDay());
        vehicle.setPricePerHour(request.getPricePerHour());
        vehicle.setColor(request.getColor());
        vehicle.setDescription(request.getDescription());
        vehicle.setFeatures(request.getFeatures());
        vehicle.setCity(request.getCity());
        vehicle.setPickupLocation(request.getPickupLocation());
        vehicle.setLatitude(request.getLatitude());
        vehicle.setLongitude(request.getLongitude());
        vehicle.setMileage(request.getMileage());
        vehicle.setEngineCapacity(request.getEngineCapacity());
        vehicle.setChauffeurAvailable(request.getChauffeurAvailable());

        vehicle = vehicleRepository.save(vehicle);
        log.info("Vehicle updated: id={}", id);
        return mapToResponse(vehicle);
    }

    @Transactional
    public void deleteVehicle(Long id) {
        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle", "id", id));
        vehicle.setStatus(VehicleStatus.RETIRED);
        vehicleRepository.save(vehicle);
        log.info("Vehicle retired: id={}", id);
    }

    @Transactional
    public VehicleResponse updateVehicleStatus(Long id, VehicleStatus status) {
        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle", "id", id));
        vehicle.setStatus(status);
        vehicle = vehicleRepository.save(vehicle);
        log.info("Vehicle status updated to {} for id={}", status, id);
        return mapToResponse(vehicle);
    }

    @Transactional(readOnly = true)
    public Page<VehicleResponse> getAllVehicles(Pageable pageable) {
        return vehicleRepository.findAll(pageable).map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public List<String> getAllCities() {
        return vehicleRepository.findAllCities();
    }

    @Transactional(readOnly = true)
    public List<String> getAllBrands() {
        return vehicleRepository.findAllBrands();
    }

    private Vehicle buildVehicle(VehicleRequest request) {
        return Vehicle.builder()
                .brand(request.getBrand())
                .model(request.getModel())
                .year(request.getYear())
                .licensePlate(request.getLicensePlate())
                .carType(request.getCarType())
                .fuelType(request.getFuelType())
                .transmissionType(request.getTransmissionType())
                .seatingCapacity(request.getSeatingCapacity())
                .pricePerDay(request.getPricePerDay())
                .pricePerHour(request.getPricePerHour())
                .color(request.getColor())
                .description(request.getDescription())
                .features(request.getFeatures())
                .city(request.getCity())
                .pickupLocation(request.getPickupLocation())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .mileage(request.getMileage())
                .engineCapacity(request.getEngineCapacity())
                .chauffeurAvailable(request.getChauffeurAvailable() != null && request.getChauffeurAvailable())
                .status(VehicleStatus.AVAILABLE)
                .build();
    }

    public VehicleResponse mapToResponse(Vehicle vehicle) {
        List<VehicleImageResponse> imageResponses = vehicle.getImages().stream()
                .map(img -> VehicleImageResponse.builder()
                        .id(img.getId())
                        .imageUrl(img.getImageUrl())
                        .caption(img.getCaption())
                        .primary(img.isPrimary())
                        .displayOrder(img.getDisplayOrder())
                        .build())
                .toList();

        return VehicleResponse.builder()
                .id(vehicle.getId())
                .brand(vehicle.getBrand())
                .model(vehicle.getModel())
                .year(vehicle.getYear())
                .licensePlate(vehicle.getLicensePlate())
                .carType(vehicle.getCarType())
                .fuelType(vehicle.getFuelType())
                .transmissionType(vehicle.getTransmissionType())
                .seatingCapacity(vehicle.getSeatingCapacity())
                .pricePerDay(vehicle.getPricePerDay())
                .pricePerHour(vehicle.getPricePerHour())
                .color(vehicle.getColor())
                .description(vehicle.getDescription())
                .features(vehicle.getFeatures())
                .city(vehicle.getCity())
                .pickupLocation(vehicle.getPickupLocation())
                .latitude(vehicle.getLatitude())
                .longitude(vehicle.getLongitude())
                .status(vehicle.getStatus())
                .averageRating(vehicle.getAverageRating())
                .totalRatings(vehicle.getTotalRatings())
                .totalBookings(vehicle.getTotalBookings())
                .mileage(vehicle.getMileage())
                .engineCapacity(vehicle.getEngineCapacity())
                .chauffeurAvailable(vehicle.getChauffeurAvailable())
                .images(imageResponses)
                .createdAt(vehicle.getCreatedAt())
                .build();
    }
}
