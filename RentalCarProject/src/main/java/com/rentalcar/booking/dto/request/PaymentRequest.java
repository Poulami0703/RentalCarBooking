package com.rentalcar.booking.dto.request;

import com.rentalcar.booking.enums.PaymentMethod;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class PaymentRequest {

    @NotNull(message = "Booking ID is required")
    private Long bookingId;

    @NotNull(message = "Payment method is required")
    private PaymentMethod paymentMethod;

    private String upiId;

    private String walletType;

    // Card details (in production these would be tokenized via a PCI-DSS gateway)
    private String cardToken;

    private Integer emiMonths;

    @Positive
    private BigDecimal amount;
}
