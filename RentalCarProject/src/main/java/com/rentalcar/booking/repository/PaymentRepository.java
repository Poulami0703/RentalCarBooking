package com.rentalcar.booking.repository;

import com.rentalcar.booking.entity.Payment;
import com.rentalcar.booking.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByTransactionId(String transactionId);

    Optional<Payment> findByBookingId(Long bookingId);

    boolean existsByTransactionId(String transactionId);

    java.util.List<Payment> findByStatus(PaymentStatus status);
}
