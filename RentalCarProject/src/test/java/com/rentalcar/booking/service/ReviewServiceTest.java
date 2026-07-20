package com.rentalcar.booking.service;

import com.rentalcar.booking.dto.request.CreateReviewRequest;
import com.rentalcar.booking.dto.response.ReviewResponse;
import com.rentalcar.booking.entity.*;
import com.rentalcar.booking.enums.*;
import com.rentalcar.booking.exception.AccessDeniedException;
import com.rentalcar.booking.exception.BookingModificationException;
import com.rentalcar.booking.exception.ResourceNotFoundException;
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
class ReviewServiceTest {

    @Mock ReviewRepository reviewRepository;
    @Mock BookingRepository bookingRepository;
    @Mock UserRepository userRepository;
    @Mock VehicleRepository vehicleRepository;
    @InjectMocks ReviewService reviewService;

    private User reviewer;
    private Vehicle vehicle;
    private Booking booking;
    private Review review;

    @BeforeEach
    void setUp() {
        reviewer = User.builder()
                .id(1L).firstName("John").lastName("Doe")
                .email("john@example.com")
                .role(UserRole.CUSTOMER).authProvider(AuthProvider.LOCAL)
                .kycStatus(KycStatus.NOT_SUBMITTED).build();

        vehicle = Vehicle.builder()
                .id(10L).brand("Toyota").model("Fortuner")
                .pricePerDay(new BigDecimal("4500.00"))
                .status(VehicleStatus.AVAILABLE)
                .averageRating(4.5).totalRatings(2).totalBookings(5)
                .chauffeurAvailable(false)
                .images(new ArrayList<>()).bookings(new ArrayList<>())
                .reviews(new ArrayList<>()).availabilitySlots(new ArrayList<>())
                .build();

        booking = Booking.builder()
                .id(100L).bookingReference("BK-REV1234")
                .customer(reviewer).vehicle(vehicle)
                .status(BookingStatus.COMPLETED)
                .totalAmount(new BigDecimal("10620.00"))
                .baseAmount(new BigDecimal("9000.00"))
                .taxAmount(new BigDecimal("1620.00"))
                .discountAmount(BigDecimal.ZERO)
                .chauffeurRequested(false)
                .pickupDateTime(LocalDateTime.now().minusDays(5))
                .dropoffDateTime(LocalDateTime.now().minusDays(3))
                .bookingType(BookingType.INSTANT)
                .build();

        review = Review.builder()
                .id(1L).vehicle(vehicle).reviewer(reviewer).booking(booking)
                .vehicleRating(5).reviewText("Excellent ride!")
                .approved(false).build();
    }

    // ---- createReview ----

    @Test
    @DisplayName("createReview: completed booking creates review pending approval")
    void createReview_success() {
        CreateReviewRequest req = new CreateReviewRequest();
        req.setVehicleId(10L); req.setBookingId(100L);
        req.setVehicleRating(5); req.setReviewText("Excellent!");

        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(reviewer));
        when(vehicleRepository.findById(10L)).thenReturn(Optional.of(vehicle));
        when(bookingRepository.findById(100L)).thenReturn(Optional.of(booking));
        when(reviewRepository.existsByBookingId(100L)).thenReturn(false);
        when(reviewRepository.save(any(Review.class))).thenReturn(review);

        ReviewResponse resp = reviewService.createReview("john@example.com", req);

        assertThat(resp.getVehicleRating()).isEqualTo(5);
        assertThat(resp.isApproved()).isFalse();
        verify(reviewRepository).save(any(Review.class));
    }

    @Test
    @DisplayName("createReview: booking not completed throws BookingModificationException")
    void createReview_notCompleted_throws() {
        booking.setStatus(BookingStatus.CONFIRMED);
        CreateReviewRequest req = new CreateReviewRequest();
        req.setVehicleId(10L); req.setBookingId(100L);
        req.setVehicleRating(4); req.setReviewText("Good");

        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(reviewer));
        when(vehicleRepository.findById(10L)).thenReturn(Optional.of(vehicle));
        when(bookingRepository.findById(100L)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> reviewService.createReview("john@example.com", req))
                .isInstanceOf(BookingModificationException.class)
                .hasMessageContaining("completed bookings");
    }

    @Test
    @DisplayName("createReview: another user's booking throws AccessDeniedException")
    void createReview_wrongUser_throws() {
        User other = User.builder().id(99L).email("other@example.com")
                .role(UserRole.CUSTOMER).authProvider(AuthProvider.LOCAL)
                .kycStatus(KycStatus.NOT_SUBMITTED).build();
        CreateReviewRequest req = new CreateReviewRequest();
        req.setVehicleId(10L); req.setBookingId(100L);
        req.setVehicleRating(3); req.setReviewText("OK");

        when(userRepository.findByEmail("other@example.com")).thenReturn(Optional.of(other));
        when(vehicleRepository.findById(10L)).thenReturn(Optional.of(vehicle));
        when(bookingRepository.findById(100L)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> reviewService.createReview("other@example.com", req))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("createReview: duplicate review for same booking throws BookingModificationException")
    void createReview_duplicate_throws() {
        CreateReviewRequest req = new CreateReviewRequest();
        req.setVehicleId(10L); req.setBookingId(100L);
        req.setVehicleRating(4); req.setReviewText("Nice");

        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(reviewer));
        when(vehicleRepository.findById(10L)).thenReturn(Optional.of(vehicle));
        when(bookingRepository.findById(100L)).thenReturn(Optional.of(booking));
        when(reviewRepository.existsByBookingId(100L)).thenReturn(true);

        assertThatThrownBy(() -> reviewService.createReview("john@example.com", req))
                .isInstanceOf(BookingModificationException.class)
                .hasMessageContaining("already been submitted");
    }

    // ---- approveReview ----

    @Test
    @DisplayName("approveReview: sets approved=true and recalculates vehicle rating")
    void approveReview_success() {
        when(reviewRepository.findById(1L)).thenReturn(Optional.of(review));
        when(reviewRepository.save(any(Review.class))).thenReturn(review);
        when(reviewRepository.calculateAverageRating(10L)).thenReturn(4.8);
        when(reviewRepository.countApprovedReviews(10L)).thenReturn(3L);
        when(vehicleRepository.save(any(Vehicle.class))).thenReturn(vehicle);

        ReviewResponse resp = reviewService.approveReview(1L);

        assertThat(review.isApproved()).isTrue();
        assertThat(vehicle.getAverageRating()).isEqualTo(4.8);
        assertThat(vehicle.getTotalRatings()).isEqualTo(3);
    }

    @Test
    @DisplayName("approveReview: unknown id throws ResourceNotFoundException")
    void approveReview_notFound_throws() {
        when(reviewRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.approveReview(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ---- getVehicleReviews ----

    @Test
    @DisplayName("getVehicleReviews: returns only approved reviews")
    void getVehicleReviews_success() {
        review.setApproved(true);
        Pageable pageable = PageRequest.of(0, 10);
        Page<Review> page = new PageImpl<>(List.of(review));

        when(reviewRepository.findByVehicleIdAndApprovedTrue(10L, pageable)).thenReturn(page);

        Page<ReviewResponse> result = reviewService.getVehicleReviews(10L, pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getReviewText()).isEqualTo("Excellent ride!");
    }

    // ---- deleteReview ----

    @Test
    @DisplayName("deleteReview: removes review from repository")
    void deleteReview_success() {
        when(reviewRepository.findById(1L)).thenReturn(Optional.of(review));

        reviewService.deleteReview(1L);

        verify(reviewRepository).delete(review);
    }
}
