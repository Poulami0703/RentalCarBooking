package com.rentalcar.booking.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidOtpException extends RuntimeException {
    public InvalidOtpException() {
        super("Invalid OTP. Please check and try again.");
    }
}
