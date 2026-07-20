package com.rentalcar.booking;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class RentalCarApplication {

    public static void main(String[] args) {
        SpringApplication.run(RentalCarApplication.class, args);
    }
}
