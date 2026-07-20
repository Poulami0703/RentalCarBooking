package com.rentalcar.booking.repository;

import com.rentalcar.booking.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    Page<Review> findByVehicleIdAndApprovedTrue(Long vehicleId, Pageable pageable);

    Page<Review> findByReviewerIdOrderByCreatedAtDesc(Long reviewerId, Pageable pageable);

    Page<Review> findByApprovedFalseOrderByCreatedAtAsc(Pageable pageable);

    Optional<Review> findByBookingId(Long bookingId);

    boolean existsByBookingId(Long bookingId);

    @Query("SELECT AVG(r.vehicleRating) FROM Review r WHERE r.vehicle.id = :vehicleId AND r.approved = true")
    Double calculateAverageRating(@Param("vehicleId") Long vehicleId);

    @Query("SELECT COUNT(r) FROM Review r WHERE r.vehicle.id = :vehicleId AND r.approved = true")
    Long countApprovedReviews(@Param("vehicleId") Long vehicleId);
}
