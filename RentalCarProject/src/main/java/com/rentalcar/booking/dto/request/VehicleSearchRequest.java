package com.rentalcar.booking.dto.request;

import com.rentalcar.booking.enums.CarType;
import com.rentalcar.booking.enums.FuelType;
import com.rentalcar.booking.enums.TransmissionType;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class VehicleSearchRequest {

    private String city;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime pickupDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime dropoffDate;

    private CarType carType;

    private FuelType fuelType;

    private TransmissionType transmissionType;

    private String brand;

    private BigDecimal minPrice;

    private BigDecimal maxPrice;

    private Integer minSeats;

    private String sortBy = "pricePerDay";

    private String sortDir = "asc";

    private int page = 0;

    private int size = 10;
}
