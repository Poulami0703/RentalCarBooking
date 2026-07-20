package com.rentalcar.booking.repository;

import com.rentalcar.booking.entity.Booking;
import com.rentalcar.booking.enums.BookingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    Optional<Booking> findByBookingReference(String bookingReference);

    Page<Booking> findByCustomerIdOrderByCreatedAtDesc(Long customerId, Pageable pageable);

    Page<Booking> findByStatusOrderByCreatedAtDesc(BookingStatus status, Pageable pageable);

    @Query("""
            SELECT b FROM Booking b
            WHERE b.vehicle.id = :vehicleId
            AND b.status NOT IN ('CANCELLED', 'COMPLETED')
            AND NOT (b.dropoffDateTime <= :pickupDate OR b.pickupDateTime >= :dropoffDate)
            """)
    List<Booking> findConflictingBookings(
            @Param("vehicleId") Long vehicleId,
            @Param("pickupDate") LocalDateTime pickupDate,
            @Param("dropoffDate") LocalDateTime dropoffDate
    );

    @Query("SELECT COUNT(b) FROM Booking b WHERE b.status NOT IN ('CANCELLED')")
    Long countActiveBookings();

    @Query("""
            SELECT COALESCE(SUM(b.totalAmount), 0)
            FROM Booking b
            WHERE b.status = 'COMPLETED'
            AND b.completedAt BETWEEN :from AND :to
            """)
    java.math.BigDecimal sumRevenueByDateRange(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );

    @Query("SELECT b FROM Booking b WHERE b.customer.id = :customerId AND b.id = :bookingId")
    Optional<Booking> findByIdAndCustomerId(@Param("bookingId") Long bookingId, @Param("customerId") Long customerId);

    List<Booking> findByVehicleIdAndStatusNotIn(Long vehicleId, List<BookingStatus> statuses);
}
