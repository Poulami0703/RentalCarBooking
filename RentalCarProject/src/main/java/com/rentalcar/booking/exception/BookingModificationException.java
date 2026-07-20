package com.rentalcar.booking.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class BookingModificationException extends RuntimeException {
    public BookingModificationException(String message) {
        super(message);
    }
}
