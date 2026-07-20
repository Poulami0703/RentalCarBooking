package com.rentalcar.booking.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardResponse {
    private Long totalBookings;
    private Long activeBookings;
    private Long completedBookings;
    private Long cancelledBookings;
    private BigDecimal totalRevenue;
    private BigDecimal monthlyRevenue;
    private Long totalVehicles;
    private Long availableVehicles;
    private Long totalCustomers;
    private Long pendingKycCount;
    private Long pendingReviewsCount;
}
