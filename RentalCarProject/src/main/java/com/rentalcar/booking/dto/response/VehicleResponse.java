package com.rentalcar.booking.dto.response;

import com.rentalcar.booking.enums.CarType;
import com.rentalcar.booking.enums.FuelType;
import com.rentalcar.booking.enums.TransmissionType;
import com.rentalcar.booking.enums.VehicleStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleResponse {
    private Long id;
    private String brand;
    private String model;
    private Integer year;
    private String licensePlate;
    private CarType carType;
    private FuelType fuelType;
    private TransmissionType transmissionType;
    private Integer seatingCapacity;
    private BigDecimal pricePerDay;
    private BigDecimal pricePerHour;
    private String color;
    private String description;
    private String features;
    private String city;
    private String pickupLocation;
    private Double latitude;
    private Double longitude;
    private VehicleStatus status;
    private Double averageRating;
    private Integer totalRatings;
    private Integer totalBookings;
    private String mileage;
    private String engineCapacity;
    private Boolean chauffeurAvailable;
    private List<VehicleImageResponse> images;
    private LocalDateTime createdAt;
}
