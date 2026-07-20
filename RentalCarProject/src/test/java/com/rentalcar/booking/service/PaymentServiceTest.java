package com.rentalcar.booking.service;

import com.rentalcar.booking.dto.request.PaymentRequest;
import com.rentalcar.booking.dto.response.PaymentResponse;
import com.rentalcar.booking.entity.Booking;
import com.rentalcar.booking.entity.Payment;
import com.rentalcar.booking.entity.User;
import com.rentalcar.booking.entity.Vehicle;
import com.rentalcar.booking.enums.*;
import com.rentalcar.booking.exception.AccessDeniedException;
import com.rentalcar.booking.exception.BookingModificationException;
import com.rentalcar.booking.exception.PaymentException;
import com.rentalcar.booking.exception.ResourceNotFoundException;
import com.rentalcar.booking.repository.BookingRepository;
import com.rentalcar.booking.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock PaymentRepository paymentRepository;
    @Mock BookingRepository bookingRepository;
    @InjectMocks PaymentService paymentService;

    private User customer;
    private Booking booking;
    private Payment payment;

    @BeforeEach
    void setUp() {
        customer = User.builder()
                .id(1L).email("john@example.com")
                .role(UserRole.CUSTOMER).authProvider(AuthProvider.LOCAL)
                .kycStatus(KycStatus.NOT_SUBMITTED).build();

        Vehicle vehicle = Vehicle.builder()
                .id(10L).brand("Toyota").model("Fortuner")
                .pricePerDay(new BigDecimal("4500.00"))
                .status(VehicleStatus.AVAILABLE)
                .averageRating(0.0).totalRatings(0).totalBookings(0)
                .chauffeurAvailable(false)
                .images(new ArrayList<>()).bookings(new ArrayList<>())
                .reviews(new ArrayList<>()).availabilitySlots(new ArrayList<>())
                .build();

        booking = Booking.builder()
                .id(100L).bookingReference("BK-TEST1234")
                .customer(customer).vehicle(vehicle)
                .status(BookingStatus.CONFIRMED)
                .totalAmount(new BigDecimal("10620.00"))
                .baseAmount(new BigDecimal("9000.00"))
                .taxAmount(new BigDecimal("1620.00"))
                .discountAmount(BigDecimal.ZERO)
                .chauffeurRequested(false)
                .pickupDateTime(LocalDateTime.now().plusDays(1))
                .dropoffDateTime(LocalDateTime.now().plusDays(3))
                .bookingType(BookingType.INSTANT)
                .build();

        payment = Payment.builder()
                .id(200L).booking(booking)
                .transactionId("TXN-ABCDE12345")
                .paymentMethod(PaymentMethod.UPI)
                .status(PaymentStatus.SUCCESS)
                .amount(new BigDecimal("10620.00"))
                .currency("INR")
                .build();
    }

    // ---- initiatePayment ----

    @Test
    @DisplayName("initiatePayment: valid request processes payment successfully")
    void initiatePayment_success() {
        PaymentRequest req = new PaymentRequest();
        req.setBookingId(100L);
        req.setPaymentMethod(PaymentMethod.UPI);
        req.setUpiId("john@upi");
        req.setAmount(new BigDecimal("10620.00"));

        when(bookingRepository.findById(100L)).thenReturn(Optional.of(booking));
        when(paymentRepository.findByBookingId(100L)).thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);
        when(bookingRepository.save(any(Booking.class))).thenReturn(booking);

        PaymentResponse resp = paymentService.initiatePayment("john@example.com", req);

        assertThat(resp.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(resp.getTransactionId()).isEqualTo("TXN-ABCDE12345");
    }

    @Test
    @DisplayName("initiatePayment: wrong user throws AccessDeniedException")
    void initiatePayment_wrongUser_throws() {
        PaymentRequest req = new PaymentRequest();
        req.setBookingId(100L);
        req.setPaymentMethod(PaymentMethod.CREDIT_CARD);

        when(bookingRepository.findById(100L)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> paymentService.initiatePayment("other@example.com", req))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("initiatePayment: cancelled booking throws BookingModificationException")
    void initiatePayment_cancelledBooking_throws() {
        booking.setStatus(BookingStatus.CANCELLED);
        PaymentRequest req = new PaymentRequest();
        req.setBookingId(100L);
        req.setPaymentMethod(PaymentMethod.CREDIT_CARD);

        when(bookingRepository.findById(100L)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> paymentService.initiatePayment("john@example.com", req))
                .isInstanceOf(BookingModificationException.class);
    }

    @Test
    @DisplayName("initiatePayment: duplicate payment throws PaymentException")
    void initiatePayment_duplicate_throws() {
        PaymentRequest req = new PaymentRequest();
        req.setBookingId(100L);
        req.setPaymentMethod(PaymentMethod.NET_BANKING);

        when(bookingRepository.findById(100L)).thenReturn(Optional.of(booking));
        when(paymentRepository.findByBookingId(100L)).thenReturn(Optional.of(payment));

        assertThatThrownBy(() -> paymentService.initiatePayment("john@example.com", req))
                .isInstanceOf(PaymentException.class)
                .hasMessageContaining("already initiated");
    }

    // ---- processRefund ----

    @Test
    @DisplayName("processRefund: full refund sets status REFUNDED")
    void processRefund_full_success() {
        when(paymentRepository.findByBookingId(100L)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);
        when(bookingRepository.save(any(Booking.class))).thenReturn(booking);

        PaymentResponse resp = paymentService.processRefund(100L, new BigDecimal("10620.00"));

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
        assertThat(resp.getRefundedAmount()).isEqualByComparingTo("10620.00");
    }

    @Test
    @DisplayName("processRefund: partial refund sets status PARTIALLY_REFUNDED")
    void processRefund_partial_success() {
        when(paymentRepository.findByBookingId(100L)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);
        when(bookingRepository.save(any(Booking.class))).thenReturn(booking);

        paymentService.processRefund(100L, new BigDecimal("5000.00"));

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PARTIALLY_REFUNDED);
    }

    @Test
    @DisplayName("processRefund: refund exceeds payment amount throws PaymentException")
    void processRefund_exceeds_throws() {
        when(paymentRepository.findByBookingId(100L)).thenReturn(Optional.of(payment));

        assertThatThrownBy(() -> paymentService.processRefund(100L, new BigDecimal("99999.00")))
                .isInstanceOf(PaymentException.class)
                .hasMessageContaining("cannot exceed");
    }

    @Test
    @DisplayName("processRefund: payment not found throws ResourceNotFoundException")
    void processRefund_notFound_throws() {
        when(paymentRepository.findByBookingId(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.processRefund(999L, BigDecimal.TEN))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
