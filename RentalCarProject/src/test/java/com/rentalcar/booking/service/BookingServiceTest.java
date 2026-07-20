package com.rentalcar.booking.service;

import com.rentalcar.booking.dto.request.CreateBookingRequest;
import com.rentalcar.booking.dto.response.BookingResponse;
import com.rentalcar.booking.entity.Booking;
import com.rentalcar.booking.entity.User;
import com.rentalcar.booking.entity.Vehicle;
import com.rentalcar.booking.enums.*;
import com.rentalcar.booking.exception.*;
import com.rentalcar.booking.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock BookingRepository bookingRepository;
    @Mock VehicleRepository vehicleRepository;
    @Mock UserRepository userRepository;
    @Mock AuditLogRepository auditLogRepository;

    @InjectMocks BookingService bookingService;

    private User customer;
    private Vehicle vehicle;
    private Booking booking;

    @BeforeEach
    void setUp() {
        customer = User.builder()
                .id(1L).firstName("John").lastName("Doe")
                .email("john@example.com")
                .role(UserRole.CUSTOMER).authProvider(AuthProvider.LOCAL)
                .kycStatus(KycStatus.NOT_SUBMITTED).build();

        vehicle = Vehicle.builder()
                .id(10L).brand("Toyota").model("Fortuner")
                .pricePerDay(new BigDecimal("4500.00"))
                .pricePerHour(new BigDecimal("300.00"))
                .status(VehicleStatus.AVAILABLE)
                .totalBookings(0)
                .averageRating(0.0).totalRatings(0)
                .chauffeurAvailable(false)
                .images(new ArrayList<>()).bookings(new ArrayList<>())
                .reviews(new ArrayList<>()).availabilitySlots(new ArrayList<>())
                .build();

        booking = Booking.builder()
                .id(100L).bookingReference("BK-ABCD1234")
                .customer(customer).vehicle(vehicle)
                .pickupDateTime(LocalDateTime.now().plusDays(1))
                .dropoffDateTime(LocalDateTime.now().plusDays(3))
                .pickupLocation("Mumbai").dropoffLocation("Mumbai")
                .bookingType(BookingType.INSTANT)
                .status(BookingStatus.CONFIRMED)
                .baseAmount(new BigDecimal("9000.00"))
                .taxAmount(new BigDecimal("1620.00"))
                .discountAmount(BigDecimal.ZERO)
                .totalAmount(new BigDecimal("10620.00"))
                .chauffeurRequested(false)
                .build();
    }

    // ---- createBooking ----

    @Test
    @DisplayName("createBooking: valid request creates booking")
    void createBooking_success() {
        CreateBookingRequest req = buildRequest(
                LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(3));

        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(customer));
        when(vehicleRepository.findById(10L)).thenReturn(Optional.of(vehicle));
        when(bookingRepository.findConflictingBookings(anyLong(), any(), any())).thenReturn(List.of());
        when(bookingRepository.save(any(Booking.class))).thenReturn(booking);
        when(vehicleRepository.save(any(Vehicle.class))).thenReturn(vehicle);
        when(auditLogRepository.save(any())).thenReturn(null);

        BookingResponse resp = bookingService.createBooking("john@example.com", req);

        assertThat(resp.getBookingReference()).isEqualTo("BK-ABCD1234");
        assertThat(resp.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        verify(bookingRepository).save(any(Booking.class));
    }

    @Test
    @DisplayName("createBooking: vehicle not available throws VehicleNotAvailableException")
    void createBooking_vehicleUnavailable_throws() {
        CreateBookingRequest req = buildRequest(
                LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(3));

        vehicle.setStatus(VehicleStatus.BOOKED);
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(customer));
        when(vehicleRepository.findById(10L)).thenReturn(Optional.of(vehicle));

        assertThatThrownBy(() -> bookingService.createBooking("john@example.com", req))
                .isInstanceOf(VehicleNotAvailableException.class);
    }

    @Test
    @DisplayName("createBooking: conflicting booking throws VehicleNotAvailableException")
    void createBooking_conflict_throws() {
        CreateBookingRequest req = buildRequest(
                LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(3));

        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(customer));
        when(vehicleRepository.findById(10L)).thenReturn(Optional.of(vehicle));
        when(bookingRepository.findConflictingBookings(anyLong(), any(), any()))
                .thenReturn(List.of(booking));

        assertThatThrownBy(() -> bookingService.createBooking("john@example.com", req))
                .isInstanceOf(VehicleNotAvailableException.class);
    }

    @Test
    @DisplayName("createBooking: pickup after dropoff throws BookingModificationException")
    void createBooking_invalidDates_throws() {
        CreateBookingRequest req = buildRequest(
                LocalDateTime.now().plusDays(5), LocalDateTime.now().plusDays(2));

        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(customer));
        when(vehicleRepository.findById(10L)).thenReturn(Optional.of(vehicle));
        when(bookingRepository.findConflictingBookings(anyLong(), any(), any())).thenReturn(List.of());

        assertThatThrownBy(() -> bookingService.createBooking("john@example.com", req))
                .isInstanceOf(BookingModificationException.class)
                .hasMessageContaining("before drop-off");
    }

    // ---- cancelBooking ----

    @Test
    @DisplayName("cancelBooking: confirmed booking gets cancelled")
    void cancelBooking_success() {
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(customer));
        when(bookingRepository.findByIdAndCustomerId(100L, 1L)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenReturn(booking);
        when(auditLogRepository.save(any())).thenReturn(null);

        BookingResponse resp = bookingService.cancelBooking("john@example.com", 100L, "Changed plans");

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(booking.getCancellationReason()).isEqualTo("Changed plans");
    }

    @Test
    @DisplayName("cancelBooking: already cancelled throws BookingModificationException")
    void cancelBooking_alreadyCancelled_throws() {
        booking.setStatus(BookingStatus.CANCELLED);
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(customer));
        when(bookingRepository.findByIdAndCustomerId(100L, 1L)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> bookingService.cancelBooking("john@example.com", 100L, "reason"))
                .isInstanceOf(BookingModificationException.class);
    }

    @Test
    @DisplayName("cancelBooking: completed booking throws BookingModificationException")
    void cancelBooking_completed_throws() {
        booking.setStatus(BookingStatus.COMPLETED);
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(customer));
        when(bookingRepository.findByIdAndCustomerId(100L, 1L)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> bookingService.cancelBooking("john@example.com", 100L, "reason"))
                .isInstanceOf(BookingModificationException.class);
    }

    // ---- getCustomerBookings ----

    @Test
    @DisplayName("getCustomerBookings: returns paged bookings for customer")
    void getCustomerBookings_success() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Booking> page = new PageImpl<>(List.of(booking));

        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(customer));
        when(bookingRepository.findByCustomerIdOrderByCreatedAtDesc(1L, pageable)).thenReturn(page);

        Page<BookingResponse> result = bookingService.getCustomerBookings("john@example.com", pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getBookingReference()).isEqualTo("BK-ABCD1234");
    }

    // ---- getBookingByReference ----

    @Test
    @DisplayName("getBookingByReference: owner can retrieve booking")
    void getBookingByReference_success() {
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(customer));
        when(bookingRepository.findByBookingReference("BK-ABCD1234")).thenReturn(Optional.of(booking));

        BookingResponse resp = bookingService.getBookingByReference("BK-ABCD1234", "john@example.com");

        assertThat(resp.getBookingReference()).isEqualTo("BK-ABCD1234");
    }

    @Test
    @DisplayName("getBookingByReference: wrong user throws AccessDeniedException")
    void getBookingByReference_wrongUser_throws() {
        User other = User.builder().id(99L).email("other@example.com")
                .role(UserRole.CUSTOMER).authProvider(AuthProvider.LOCAL)
                .kycStatus(KycStatus.NOT_SUBMITTED).build();

        when(userRepository.findByEmail("other@example.com")).thenReturn(Optional.of(other));
        when(bookingRepository.findByBookingReference("BK-ABCD1234")).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> bookingService.getBookingByReference("BK-ABCD1234", "other@example.com"))
                .isInstanceOf(AccessDeniedException.class);
    }

    // ---- admin: approveBooking / completeBooking ----

    @Test
    @DisplayName("approveBooking: sets status to ACTIVE")
    void approveBooking_success() {
        when(bookingRepository.findById(100L)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenReturn(booking);

        BookingResponse resp = bookingService.approveBooking(100L);

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.ACTIVE);
    }

    @Test
    @DisplayName("completeBooking: sets status to COMPLETED")
    void completeBooking_success() {
        when(bookingRepository.findById(100L)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenReturn(booking);

        bookingService.completeBooking(100L);

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.COMPLETED);
        assertThat(booking.getCompletedAt()).isNotNull();
    }

    // ---- helper ----

    private CreateBookingRequest buildRequest(LocalDateTime pickup, LocalDateTime dropoff) {
        CreateBookingRequest r = new CreateBookingRequest();
        r.setVehicleId(10L);
        r.setPickupDateTime(pickup);
        r.setDropoffDateTime(dropoff);
        r.setPickupLocation("Mumbai");
        r.setDropoffLocation("Mumbai");
        r.setBookingType(BookingType.INSTANT);
        r.setChauffeurRequested(false);
        return r;
    }
}
