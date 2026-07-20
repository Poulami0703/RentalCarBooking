package com.rentalcar.booking.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class NoVehiclesAvailableException extends RuntimeException {
    public NoVehiclesAvailableException() {
        super("No vehicles available for your selected period.");
    }

    public NoVehiclesAvailableException(String message) {
        super(message);
    }
}
