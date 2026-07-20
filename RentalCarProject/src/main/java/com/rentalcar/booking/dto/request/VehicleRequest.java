package com.rentalcar.booking.dto.request;

import com.rentalcar.booking.enums.CarType;
import com.rentalcar.booking.enums.FuelType;
import com.rentalcar.booking.enums.TransmissionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class VehicleRequest {

    @NotBlank(message = "Brand is required")
    private String brand;

    @NotBlank(message = "Model is required")
    private String model;

    @NotNull(message = "Year is required")
    private Integer year;

    @NotBlank(message = "License plate is required")
    private String licensePlate;

    @NotNull(message = "Car type is required")
    private CarType carType;

    @NotNull(message = "Fuel type is required")
    private FuelType fuelType;

    @NotNull(message = "Transmission type is required")
    private TransmissionType transmissionType;

    @NotNull(message = "Seating capacity is required")
    @Positive(message = "Seating capacity must be positive")
    private Integer seatingCapacity;

    @NotNull(message = "Price per day is required")
    @Positive(message = "Price per day must be positive")
    private BigDecimal pricePerDay;

    @Positive(message = "Price per hour must be positive")
    private BigDecimal pricePerHour;

    private String color;

    private String description;

    private String features;

    @NotBlank(message = "City is required")
    private String city;

    @NotBlank(message = "Pickup location is required")
    private String pickupLocation;

    private Double latitude;

    private Double longitude;

    private String mileage;

    private String engineCapacity;

    private Boolean chauffeurAvailable = false;
}
