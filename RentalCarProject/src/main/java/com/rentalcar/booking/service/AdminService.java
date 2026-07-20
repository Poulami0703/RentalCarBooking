package com.rentalcar.booking.service;

import com.rentalcar.booking.dto.response.DashboardResponse;
import com.rentalcar.booking.enums.BookingStatus;
import com.rentalcar.booking.enums.KycStatus;
import com.rentalcar.booking.enums.VehicleStatus;
import com.rentalcar.booking.repository.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;

@Service
@RequiredArgsConstructor
public class AdminService {

    private static final Logger log = LoggerFactory.getLogger(AdminService.class);

    private final BookingRepository bookingRepository;
    private final VehicleRepository vehicleRepository;
    private final UserRepository userRepository;
    private final ReviewRepository reviewRepository;

    @Transactional(readOnly = true)
    public DashboardResponse getDashboardStats() {
        long totalBookings = bookingRepository.count();
        long activeBookings = bookingRepository.findByStatusOrderByCreatedAtDesc(BookingStatus.ACTIVE,
                org.springframework.data.domain.Pageable.unpaged()).getTotalElements();
        long completedBookings = bookingRepository.findByStatusOrderByCreatedAtDesc(BookingStatus.COMPLETED,
                org.springframework.data.domain.Pageable.unpaged()).getTotalElements();
        long cancelledBookings = bookingRepository.findByStatusOrderByCreatedAtDesc(BookingStatus.CANCELLED,
                org.springframework.data.domain.Pageable.unpaged()).getTotalElements();

        LocalDateTime startOfMonth = LocalDateTime.now().with(TemporalAdjusters.firstDayOfMonth()).withHour(0);
        LocalDateTime now = LocalDateTime.now();
        BigDecimal monthlyRevenue = bookingRepository.sumRevenueByDateRange(startOfMonth, now);
        BigDecimal totalRevenue = bookingRepository.sumRevenueByDateRange(LocalDateTime.of(2000, 1, 1, 0, 0), now);

        long totalVehicles = vehicleRepository.count();
        long availableVehicles = vehicleRepository.findByStatus(VehicleStatus.AVAILABLE,
                org.springframework.data.domain.Pageable.unpaged()).getTotalElements();

        long totalCustomers = userRepository.count();

        long pendingKycCount = userRepository.findAll().stream()
                .filter(u -> u.getKycStatus() == KycStatus.PENDING_REVIEW).count();

        long pendingReviewsCount = reviewRepository.findByApprovedFalseOrderByCreatedAtAsc(
                org.springframework.data.domain.Pageable.unpaged()).getTotalElements();

        log.info("Dashboard stats fetched");
        return DashboardResponse.builder()
                .totalBookings(totalBookings)
                .activeBookings(activeBookings)
                .completedBookings(completedBookings)
                .cancelledBookings(cancelledBookings)
                .totalRevenue(totalRevenue)
                .monthlyRevenue(monthlyRevenue)
                .totalVehicles(totalVehicles)
                .availableVehicles(availableVehicles)
                .totalCustomers(totalCustomers)
                .pendingKycCount(pendingKycCount)
                .pendingReviewsCount(pendingReviewsCount)
                .build();
    }
}
