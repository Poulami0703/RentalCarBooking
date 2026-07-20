package com.rentalcar.booking.service;

import com.rentalcar.booking.dto.request.PaymentRequest;
import com.rentalcar.booking.dto.response.PaymentResponse;
import com.rentalcar.booking.entity.Booking;
import com.rentalcar.booking.entity.Payment;
import com.rentalcar.booking.enums.BookingStatus;
import com.rentalcar.booking.enums.PaymentStatus;
import com.rentalcar.booking.exception.BookingModificationException;
import com.rentalcar.booking.exception.PaymentException;
import com.rentalcar.booking.exception.ResourceNotFoundException;
import com.rentalcar.booking.repository.BookingRepository;
import com.rentalcar.booking.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;

    @Transactional
    public PaymentResponse initiatePayment(String email, PaymentRequest request) {
        Booking booking = bookingRepository.findById(request.getBookingId())
                .orElseThrow(() -> new ResourceNotFoundException("Booking", "id", request.getBookingId()));

        if (!booking.getCustomer().getEmail().equals(email)) {
            throw new com.rentalcar.booking.exception.AccessDeniedException("Access denied to this booking.");
        }

        if (booking.getStatus() == BookingStatus.CANCELLED || booking.getStatus() == BookingStatus.COMPLETED) {
            throw new BookingModificationException("Payment cannot be initiated for this booking status.");
        }

        if (paymentRepository.findByBookingId(booking.getId()).isPresent()) {
            throw new PaymentException("Payment already initiated for this booking.");
        }

        BigDecimal emiAmount = null;
        if (request.getEmiMonths() != null && request.getEmiMonths() > 0) {
            emiAmount = booking.getTotalAmount()
                    .divide(BigDecimal.valueOf(request.getEmiMonths()), 2, java.math.RoundingMode.HALF_UP);
        }

        Payment payment = Payment.builder()
                .booking(booking)
                .transactionId("TXN-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase())
                .paymentMethod(request.getPaymentMethod())
                .status(PaymentStatus.INITIATED)
                .amount(booking.getTotalAmount())
                .currency("INR")
                .upiId(request.getUpiId())
                .walletType(request.getWalletType())
                .emiMonths(request.getEmiMonths())
                .emiAmount(emiAmount)
                .build();

        payment = paymentRepository.save(payment);

        // Simulate payment success (in production, integrate with a real payment gateway)
        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setPaidAt(LocalDateTime.now());
        payment.setGatewayTransactionId("GW-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        payment = paymentRepository.save(payment);

        booking.setStatus(BookingStatus.CONFIRMED);
        bookingRepository.save(booking);

        log.info("Payment processed: txnId={}, bookingRef={}", payment.getTransactionId(), booking.getBookingReference());
        return mapToResponse(payment);
    }

    @Transactional
    public PaymentResponse processRefund(Long bookingId, BigDecimal refundAmount) {
        Payment payment = paymentRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "bookingId", bookingId));

        if (payment.getStatus() != PaymentStatus.SUCCESS) {
            throw new PaymentException("Refund can only be processed for successful payments.");
        }

        if (refundAmount.compareTo(payment.getAmount()) > 0) {
            throw new PaymentException("Refund amount cannot exceed original payment amount.");
        }

        payment.setRefundedAmount(refundAmount);
        payment.setStatus(refundAmount.compareTo(payment.getAmount()) == 0
                ? PaymentStatus.REFUNDED : PaymentStatus.PARTIALLY_REFUNDED);
        payment.setRefundedAt(LocalDateTime.now());
        payment = paymentRepository.save(payment);

        Booking booking = payment.getBooking();
        booking.setRefundAmount(refundAmount);
        booking.setStatus(BookingStatus.REFUNDED);
        bookingRepository.save(booking);

        log.info("Refund processed: txnId={}, amount={}", payment.getTransactionId(), refundAmount);
        return mapToResponse(payment);
    }

    @Transactional(readOnly = true)
    public PaymentResponse getPaymentByBooking(Long bookingId) {
        Payment payment = paymentRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "bookingId", bookingId));
        return mapToResponse(payment);
    }

    private PaymentResponse mapToResponse(Payment payment) {
        return PaymentResponse.builder()
                .id(payment.getId())
                .transactionId(payment.getTransactionId())
                .paymentMethod(payment.getPaymentMethod())
                .status(payment.getStatus())
                .amount(payment.getAmount())
                .refundedAmount(payment.getRefundedAmount())
                .currency(payment.getCurrency())
                .upiId(payment.getUpiId())
                .walletType(payment.getWalletType())
                .emiMonths(payment.getEmiMonths())
                .emiAmount(payment.getEmiAmount())
                .paidAt(payment.getPaidAt())
                .refundedAt(payment.getRefundedAt())
                .createdAt(payment.getCreatedAt())
                .build();
    }
}
