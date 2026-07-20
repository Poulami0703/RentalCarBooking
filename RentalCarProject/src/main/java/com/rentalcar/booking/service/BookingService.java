package com.rentalcar.booking.service;

import com.rentalcar.booking.dto.request.CreateBookingRequest;
import com.rentalcar.booking.dto.response.BookingResponse;
import com.rentalcar.booking.dto.response.PaymentResponse;
import com.rentalcar.booking.entity.AuditLog;
import com.rentalcar.booking.entity.Booking;
import com.rentalcar.booking.entity.User;
import com.rentalcar.booking.entity.Vehicle;
import com.rentalcar.booking.enums.BookingStatus;
import com.rentalcar.booking.exception.*;
import com.rentalcar.booking.repository.AuditLogRepository;
import com.rentalcar.booking.repository.BookingRepository;
import com.rentalcar.booking.repository.UserRepository;
import com.rentalcar.booking.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BookingService {

    private static final Logger log = LoggerFactory.getLogger(BookingService.class);
    private static final BigDecimal TAX_RATE = new BigDecimal("0.18");

    private final BookingRepository bookingRepository;
    private final VehicleRepository vehicleRepository;
    private final UserRepository userRepository;
    private final AuditLogRepository auditLogRepository;

    @Transactional
    public BookingResponse createBooking(String customerEmail, CreateBookingRequest request) {
        User customer = userRepository.findByEmail(customerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", customerEmail));

        Vehicle vehicle = vehicleRepository.findById(request.getVehicleId())
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle", "id", request.getVehicleId()));

        if (vehicle.getStatus() != com.rentalcar.booking.enums.VehicleStatus.AVAILABLE) {
            throw new VehicleNotAvailableException();
        }

        // Check for conflicting bookings
        List<Booking> conflicts = bookingRepository.findConflictingBookings(
                vehicle.getId(), request.getPickupDateTime(), request.getDropoffDateTime()
        );
        if (!conflicts.isEmpty()) {
            throw new VehicleNotAvailableException();
        }

        if (request.getPickupDateTime().isAfter(request.getDropoffDateTime())) {
            throw new BookingModificationException("Pickup date must be before drop-off date.");
        }

        BigDecimal baseAmount = calculateBaseAmount(vehicle, request.getPickupDateTime(), request.getDropoffDateTime());
        BigDecimal taxAmount = baseAmount.multiply(TAX_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalAmount = baseAmount.add(taxAmount);

        Booking booking = Booking.builder()
                .bookingReference(generateBookingReference())
                .customer(customer)
                .vehicle(vehicle)
                .pickupDateTime(request.getPickupDateTime())
                .dropoffDateTime(request.getDropoffDateTime())
                .pickupLocation(request.getPickupLocation())
                .dropoffLocation(request.getDropoffLocation() != null
                        ? request.getDropoffLocation() : request.getPickupLocation())
                .bookingType(request.getBookingType())
                .status(BookingStatus.CONFIRMED)
                .baseAmount(baseAmount)
                .taxAmount(taxAmount)
                .discountAmount(BigDecimal.ZERO)
                .totalAmount(totalAmount)
                .chauffeurRequested(request.getChauffeurRequested())
                .specialRequests(request.getSpecialRequests())
                .confirmedAt(LocalDateTime.now())
                .build();

        booking = bookingRepository.save(booking);
        vehicle.setTotalBookings(vehicle.getTotalBookings() + 1);
        vehicleRepository.save(vehicle);

        log.info("Booking created: ref={}, customer={}", booking.getBookingReference(), customerEmail);
        saveAuditLog(customer, "BOOKING_CREATED", "Booking created: " + booking.getBookingReference(), booking.getId());

        return mapToResponse(booking);
    }

    @Transactional(readOnly = true)
    public Page<BookingResponse> getCustomerBookings(String email, Pageable pageable) {
        User customer = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
        return bookingRepository.findByCustomerIdOrderByCreatedAtDesc(customer.getId(), pageable)
                .map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public BookingResponse getBookingByReference(String ref, String email) {
        User customer = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
        Booking booking = bookingRepository.findByBookingReference(ref)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", "reference", ref));
        if (!booking.getCustomer().getId().equals(customer.getId())) {
            throw new AccessDeniedException("You do not have access to this booking.");
        }
        return mapToResponse(booking);
    }

    @Transactional
    public BookingResponse cancelBooking(String email, Long bookingId, String reason) {
        User customer = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
        Booking booking = bookingRepository.findByIdAndCustomerId(bookingId, customer.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Booking", "id", bookingId));

        if (booking.getStatus() == BookingStatus.CANCELLED
                || booking.getStatus() == BookingStatus.COMPLETED) {
            throw new BookingModificationException("Booking cannot be cancelled in its current state: " + booking.getStatus());
        }

        booking.setStatus(BookingStatus.CANCELLED);
        booking.setCancellationReason(reason);
        booking.setCancelledAt(LocalDateTime.now());
        booking = bookingRepository.save(booking);

        log.info("Booking cancelled: ref={}", booking.getBookingReference());
        saveAuditLog(customer, "BOOKING_CANCELLED", "Booking cancelled: " + booking.getBookingReference(), bookingId);

        return mapToResponse(booking);
    }

    @Transactional
    public BookingResponse modifyBooking(String email, Long bookingId, CreateBookingRequest request) {
        User customer = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
        Booking booking = bookingRepository.findByIdAndCustomerId(bookingId, customer.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Booking", "id", bookingId));

        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new BookingModificationException("Only confirmed bookings can be modified.");
        }

        // Check new time slot
        List<Booking> conflicts = bookingRepository.findConflictingBookings(
                booking.getVehicle().getId(), request.getPickupDateTime(), request.getDropoffDateTime()
        ).stream().filter(b -> !b.getId().equals(bookingId)).toList();

        if (!conflicts.isEmpty()) {
            throw new VehicleNotAvailableException();
        }

        BigDecimal baseAmount = calculateBaseAmount(booking.getVehicle(), request.getPickupDateTime(), request.getDropoffDateTime());
        BigDecimal taxAmount = baseAmount.multiply(TAX_RATE).setScale(2, RoundingMode.HALF_UP);

        booking.setPickupDateTime(request.getPickupDateTime());
        booking.setDropoffDateTime(request.getDropoffDateTime());
        booking.setPickupLocation(request.getPickupLocation());
        booking.setDropoffLocation(request.getDropoffLocation());
        booking.setBaseAmount(baseAmount);
        booking.setTaxAmount(taxAmount);
        booking.setTotalAmount(baseAmount.add(taxAmount));

        booking = bookingRepository.save(booking);
        log.info("Booking modified: ref={}", booking.getBookingReference());
        saveAuditLog(customer, "BOOKING_MODIFIED", "Booking modified: " + booking.getBookingReference(), bookingId);
        return mapToResponse(booking);
    }

    // Admin methods
    @Transactional(readOnly = true)
    public Page<BookingResponse> getAllBookings(Pageable pageable) {
        return bookingRepository.findAll(pageable).map(this::mapToResponse);
    }

    @Transactional
    public BookingResponse approveBooking(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", "id", bookingId));
        booking.setStatus(BookingStatus.ACTIVE);
        booking.setConfirmedAt(LocalDateTime.now());
        booking = bookingRepository.save(booking);
        log.info("Booking approved: id={}", bookingId);
        return mapToResponse(booking);
    }

    @Transactional
    public BookingResponse completeBooking(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", "id", bookingId));
        booking.setStatus(BookingStatus.COMPLETED);
        booking.setCompletedAt(LocalDateTime.now());
        booking = bookingRepository.save(booking);
        log.info("Booking completed: id={}", bookingId);
        return mapToResponse(booking);
    }

    private BigDecimal calculateBaseAmount(Vehicle vehicle, LocalDateTime from, LocalDateTime to) {
        long hours = Duration.between(from, to).toHours();
        long days = hours / 24;
        long remainingHours = hours % 24;

        BigDecimal amount = vehicle.getPricePerDay().multiply(BigDecimal.valueOf(days));
        if (remainingHours > 0 && vehicle.getPricePerHour() != null) {
            amount = amount.add(vehicle.getPricePerHour().multiply(BigDecimal.valueOf(remainingHours)));
        } else if (remainingHours > 0) {
            // Charge partial day as full day
            amount = amount.add(vehicle.getPricePerDay());
        }
        return amount.setScale(2, RoundingMode.HALF_UP);
    }

    private String generateBookingReference() {
        return "BK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private void saveAuditLog(User user, String action, String description, Long entityId) {
        auditLogRepository.save(AuditLog.builder()
                .user(user)
                .action(action)
                .description(description)
                .entityType("Booking")
                .entityId(entityId)
                .build());
    }

    public BookingResponse mapToResponse(Booking booking) {
        PaymentResponse paymentResponse = null;
        if (booking.getPayment() != null) {
            var p = booking.getPayment();
            paymentResponse = PaymentResponse.builder()
                    .id(p.getId())
                    .transactionId(p.getTransactionId())
                    .paymentMethod(p.getPaymentMethod())
                    .status(p.getStatus())
                    .amount(p.getAmount())
                    .refundedAmount(p.getRefundedAmount())
                    .currency(p.getCurrency())
                    .paidAt(p.getPaidAt())
                    .createdAt(p.getCreatedAt())
                    .build();
        }

        return BookingResponse.builder()
                .id(booking.getId())
                .bookingReference(booking.getBookingReference())
                .customerId(booking.getCustomer().getId())
                .customerName(booking.getCustomer().getFullName())
                .vehicle(null) // vehicle details returned separately for performance
                .pickupDateTime(booking.getPickupDateTime())
                .dropoffDateTime(booking.getDropoffDateTime())
                .pickupLocation(booking.getPickupLocation())
                .dropoffLocation(booking.getDropoffLocation())
                .bookingType(booking.getBookingType())
                .status(booking.getStatus())
                .baseAmount(booking.getBaseAmount())
                .taxAmount(booking.getTaxAmount())
                .discountAmount(booking.getDiscountAmount())
                .totalAmount(booking.getTotalAmount())
                .refundAmount(booking.getRefundAmount())
                .chauffeurRequested(booking.getChauffeurRequested())
                .specialRequests(booking.getSpecialRequests())
                .cancellationReason(booking.getCancellationReason())
                .createdAt(booking.getCreatedAt())
                .payment(paymentResponse)
                .build();
    }
}
