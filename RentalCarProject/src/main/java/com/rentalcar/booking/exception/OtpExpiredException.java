package com.rentalcar.booking.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class OtpExpiredException extends RuntimeException {
    public OtpExpiredException() {
        super("OTP has expired. Please request a new one.");
    }
}
