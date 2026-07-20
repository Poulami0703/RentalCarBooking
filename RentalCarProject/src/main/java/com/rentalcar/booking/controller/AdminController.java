package com.rentalcar.booking.controller;

import com.rentalcar.booking.dto.request.VehicleRequest;
import com.rentalcar.booking.dto.response.*;
import com.rentalcar.booking.enums.VehicleStatus;
import com.rentalcar.booking.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Admin Panel", description = "Admin-only endpoints for managing vehicles, users and bookings")
public class AdminController {

    private final AdminService adminService;
    private final VehicleService vehicleService;
    private final UserService userService;
    private final BookingService bookingService;
    private final ReviewService reviewService;
    private final PaymentService paymentService;

    // ---- Dashboard ----

    @GetMapping("/dashboard")
    @Operation(summary = "Get admin dashboard statistics")
    public ResponseEntity<ApiResponse<DashboardResponse>> getDashboard() {
        return ResponseEntity.ok(ApiResponse.success(adminService.getDashboardStats()));
    }

    // ---- Vehicle Management ----

    @GetMapping("/vehicles")
    @Operation(summary = "List all vehicles (paginated)")
    public ResponseEntity<ApiResponse<Page<VehicleResponse>>> getAllVehicles(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(vehicleService.getAllVehicles(pageable)));
    }

    @PostMapping("/vehicles")
    @Operation(summary = "Add a new vehicle")
    public ResponseEntity<ApiResponse<VehicleResponse>> addVehicle(@Valid @RequestBody VehicleRequest request) {
        VehicleResponse response = vehicleService.createVehicle(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Vehicle added successfully", response));
    }

    @PutMapping("/vehicles/{id}")
    @Operation(summary = "Update an existing vehicle")
    public ResponseEntity<ApiResponse<VehicleResponse>> updateVehicle(
            @PathVariable Long id,
            @Valid @RequestBody VehicleRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Vehicle updated", vehicleService.updateVehicle(id, request)));
    }

    @DeleteMapping("/vehicles/{id}")
    @Operation(summary = "Retire (soft-delete) a vehicle")
    public ResponseEntity<ApiResponse<Void>> deleteVehicle(@PathVariable Long id) {
        vehicleService.deleteVehicle(id);
        return ResponseEntity.ok(ApiResponse.success("Vehicle retired", null));
    }

    @PatchMapping("/vehicles/{id}/status")
    @Operation(summary = "Update a vehicle's availability status")
    public ResponseEntity<ApiResponse<VehicleResponse>> updateVehicleStatus(
            @PathVariable Long id,
            @RequestParam VehicleStatus status) {
        return ResponseEntity.ok(ApiResponse.success(vehicleService.updateVehicleStatus(id, status)));
    }

    // ---- Customer Management ----

    @GetMapping("/users")
    @Operation(summary = "List all users (paginated)")
    public ResponseEntity<ApiResponse<Page<UserResponse>>> getAllUsers(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(userService.getAllUsers(pageable)));
    }

    @GetMapping("/users/{id}")
    @Operation(summary = "Get user profile by ID")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(userService.getUserById(id)));
    }

    @PatchMapping("/users/{id}/kyc/approve")
    @Operation(summary = "Approve KYC for a user")
    public ResponseEntity<ApiResponse<UserResponse>> approveKyc(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("KYC approved", userService.approveKyc(id)));
    }

    @PatchMapping("/users/{id}/kyc/reject")
    @Operation(summary = "Reject KYC for a user")
    public ResponseEntity<ApiResponse<UserResponse>> rejectKyc(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("KYC rejected", userService.rejectKyc(id)));
    }

    @PatchMapping("/users/{id}/disable")
    @Operation(summary = "Disable a user account")
    public ResponseEntity<ApiResponse<Void>> disableUser(@PathVariable Long id) {
        userService.disableUser(id);
        return ResponseEntity.ok(ApiResponse.success("User disabled", null));
    }

    // ---- Booking Management ----

    @GetMapping("/bookings")
    @Operation(summary = "List all bookings (paginated)")
    public ResponseEntity<ApiResponse<Page<BookingResponse>>> getAllBookings(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(bookingService.getAllBookings(pageable)));
    }

    @PatchMapping("/bookings/{id}/approve")
    @Operation(summary = "Approve/activate a booking")
    public ResponseEntity<ApiResponse<BookingResponse>> approveBooking(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Booking approved", bookingService.approveBooking(id)));
    }

    @PatchMapping("/bookings/{id}/complete")
    @Operation(summary = "Mark a booking as completed")
    public ResponseEntity<ApiResponse<BookingResponse>> completeBooking(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Booking completed", bookingService.completeBooking(id)));
    }

    @PostMapping("/bookings/{bookingId}/refund")
    @Operation(summary = "Process a refund for a booking")
    public ResponseEntity<ApiResponse<PaymentResponse>> processRefund(
            @PathVariable Long bookingId,
            @RequestParam BigDecimal amount) {
        PaymentResponse response = paymentService.processRefund(bookingId, amount);
        return ResponseEntity.ok(ApiResponse.success("Refund processed", response));
    }

    // ---- Review Management ----

    @GetMapping("/reviews/pending")
    @Operation(summary = "Get all pending (unapproved) reviews")
    public ResponseEntity<ApiResponse<Page<ReviewResponse>>> getPendingReviews(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(reviewService.getPendingReviews(pageable)));
    }

    @PatchMapping("/reviews/{id}/approve")
    @Operation(summary = "Approve a customer review")
    public ResponseEntity<ApiResponse<ReviewResponse>> approveReview(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Review approved", reviewService.approveReview(id)));
    }

    @DeleteMapping("/reviews/{id}")
    @Operation(summary = "Delete a review")
    public ResponseEntity<ApiResponse<Void>> deleteReview(@PathVariable Long id) {
        reviewService.deleteReview(id);
        return ResponseEntity.ok(ApiResponse.success("Review deleted", null));
    }
}
