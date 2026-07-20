package com.rentalcar.booking.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class VehicleNotAvailableException extends RuntimeException {
    public VehicleNotAvailableException() {
        super("The selected vehicle has already been reserved by another customer.");
    }

    public VehicleNotAvailableException(String message) {
        super(message);
    }
}
