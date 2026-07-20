package com.rentalcar.booking.dto.response;

import com.rentalcar.booking.enums.PaymentMethod;
import com.rentalcar.booking.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {
    private Long id;
    private String transactionId;
    private PaymentMethod paymentMethod;
    private PaymentStatus status;
    private BigDecimal amount;
    private BigDecimal refundedAmount;
    private String currency;
    private String upiId;
    private String walletType;
    private Integer emiMonths;
    private BigDecimal emiAmount;
    private LocalDateTime paidAt;
    private LocalDateTime refundedAt;
    private LocalDateTime createdAt;
}
