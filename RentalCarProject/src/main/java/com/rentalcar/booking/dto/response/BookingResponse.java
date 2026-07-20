package com.rentalcar.booking.dto.response;

import com.rentalcar.booking.enums.BookingStatus;
import com.rentalcar.booking.enums.BookingType;
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
public class BookingResponse {
    private Long id;
    private String bookingReference;
    private Long customerId;
    private String customerName;
    private VehicleResponse vehicle;
    private LocalDateTime pickupDateTime;
    private LocalDateTime dropoffDateTime;
    private String pickupLocation;
    private String dropoffLocation;
    private BookingType bookingType;
    private BookingStatus status;
    private BigDecimal baseAmount;
    private BigDecimal taxAmount;
    private BigDecimal discountAmount;
    private BigDecimal totalAmount;
    private BigDecimal refundAmount;
    private Boolean chauffeurRequested;
    private String specialRequests;
    private String cancellationReason;
    private LocalDateTime createdAt;
    private PaymentResponse payment;
}
