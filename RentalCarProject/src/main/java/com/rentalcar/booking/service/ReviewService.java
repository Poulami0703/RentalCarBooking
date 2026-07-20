package com.rentalcar.booking.service;

import com.rentalcar.booking.dto.request.CreateReviewRequest;
import com.rentalcar.booking.dto.response.ReviewResponse;
import com.rentalcar.booking.entity.Booking;
import com.rentalcar.booking.entity.Review;
import com.rentalcar.booking.entity.User;
import com.rentalcar.booking.entity.Vehicle;
import com.rentalcar.booking.enums.BookingStatus;
import com.rentalcar.booking.exception.AccessDeniedException;
import com.rentalcar.booking.exception.BookingModificationException;
import com.rentalcar.booking.exception.ResourceNotFoundException;
import com.rentalcar.booking.repository.BookingRepository;
import com.rentalcar.booking.repository.ReviewRepository;
import com.rentalcar.booking.repository.UserRepository;
import com.rentalcar.booking.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private static final Logger log = LoggerFactory.getLogger(ReviewService.class);

    private final ReviewRepository reviewRepository;
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final VehicleRepository vehicleRepository;

    @Transactional
    public ReviewResponse createReview(String email, CreateReviewRequest request) {
        User reviewer = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));

        Vehicle vehicle = vehicleRepository.findById(request.getVehicleId())
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle", "id", request.getVehicleId()));

        Booking booking = bookingRepository.findById(request.getBookingId())
                .orElseThrow(() -> new ResourceNotFoundException("Booking", "id", request.getBookingId()));

        if (!booking.getCustomer().getId().equals(reviewer.getId())) {
            throw new AccessDeniedException("You can only review your own bookings.");
        }

        if (booking.getStatus() != BookingStatus.COMPLETED) {
            throw new BookingModificationException("You can only review completed bookings.");
        }

        if (reviewRepository.existsByBookingId(request.getBookingId())) {
            throw new BookingModificationException("A review has already been submitted for this booking.");
        }

        Review review = Review.builder()
                .vehicle(vehicle)
                .reviewer(reviewer)
                .booking(booking)
                .vehicleRating(request.getVehicleRating())
                .driverRating(request.getDriverRating())
                .reviewText(request.getReviewText())
                .approved(false)
                .build();

        review = reviewRepository.save(review);
        log.info("Review submitted for vehicle id={} by user={}", vehicle.getId(), email);
        return mapToResponse(review);
    }

    @Transactional(readOnly = true)
    public Page<ReviewResponse> getVehicleReviews(Long vehicleId, Pageable pageable) {
        return reviewRepository.findByVehicleIdAndApprovedTrue(vehicleId, pageable)
                .map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public Page<ReviewResponse> getMyReviews(String email, Pageable pageable) {
        User reviewer = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
        return reviewRepository.findByReviewerIdOrderByCreatedAtDesc(reviewer.getId(), pageable)
                .map(this::mapToResponse);
    }

    // Admin methods
    @Transactional(readOnly = true)
    public Page<ReviewResponse> getPendingReviews(Pageable pageable) {
        return reviewRepository.findByApprovedFalseOrderByCreatedAtAsc(pageable)
                .map(this::mapToResponse);
    }

    @Transactional
    public ReviewResponse approveReview(Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review", "id", reviewId));
        review.setApproved(true);
        review = reviewRepository.save(review);

        // Recalculate vehicle rating
        Double avgRating = reviewRepository.calculateAverageRating(review.getVehicle().getId());
        Long count = reviewRepository.countApprovedReviews(review.getVehicle().getId());
        Vehicle vehicle = review.getVehicle();
        vehicle.setAverageRating(avgRating != null ? avgRating : 0.0);
        vehicle.setTotalRatings(count != null ? count.intValue() : 0);
        vehicleRepository.save(vehicle);

        log.info("Review approved: id={}", reviewId);
        return mapToResponse(review);
    }

    @Transactional
    public void deleteReview(Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review", "id", reviewId));
        reviewRepository.delete(review);
        log.info("Review deleted: id={}", reviewId);
    }

    private ReviewResponse mapToResponse(Review review) {
        return ReviewResponse.builder()
                .id(review.getId())
                .vehicleId(review.getVehicle().getId())
                .vehicleName(review.getVehicle().getBrand() + " " + review.getVehicle().getModel())
                .reviewerId(review.getReviewer().getId())
                .reviewerName(review.getReviewer().getFullName())
                .vehicleRating(review.getVehicleRating())
                .driverRating(review.getDriverRating())
                .reviewText(review.getReviewText())
                .approved(review.isApproved())
                .adminResponse(review.getAdminResponse())
                .createdAt(review.getCreatedAt())
                .build();
    }
}
