package com.rentalcar.booking.dto.request;

import com.rentalcar.booking.enums.BookingType;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CreateBookingRequest {

    @NotNull(message = "Vehicle ID is required")
    private Long vehicleId;

    @NotNull(message = "Pickup date and time is required")
    @FutureOrPresent(message = "Pickup date must be in the present or future")
    private LocalDateTime pickupDateTime;

    @NotNull(message = "Drop-off date and time is required")
    @Future(message = "Drop-off date must be in the future")
    private LocalDateTime dropoffDateTime;

    @NotBlank(message = "Pickup location is required")
    private String pickupLocation;

    private String dropoffLocation;

    @NotNull(message = "Booking type is required")
    private BookingType bookingType;

    private Boolean chauffeurRequested = false;

    private String specialRequests;
}
