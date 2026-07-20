package com.rentalcar.booking.controller;

import com.rentalcar.booking.dto.request.CreateBookingRequest;
import com.rentalcar.booking.dto.response.ApiResponse;
import com.rentalcar.booking.dto.response.BookingResponse;
import com.rentalcar.booking.service.BookingService;
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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Bookings", description = "Booking creation, management, and cancellation")
public class BookingController {

    private final BookingService bookingService;

    @PostMapping
    @Operation(summary = "Create a new booking (instant or scheduled)")
    public ResponseEntity<ApiResponse<BookingResponse>> createBooking(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CreateBookingRequest request) {
        BookingResponse response = bookingService.createBooking(userDetails.getUsername(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Booking created successfully", response));
    }

    @GetMapping
    @Operation(summary = "Get current user's booking history")
    public ResponseEntity<ApiResponse<Page<BookingResponse>>> getMyBookings(
            @AuthenticationPrincipal UserDetails userDetails,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<BookingResponse> response = bookingService.getCustomerBookings(userDetails.getUsername(), pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{reference}")
    @Operation(summary = "Get booking details by booking reference")
    public ResponseEntity<ApiResponse<BookingResponse>> getBookingByReference(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String reference) {
        BookingResponse response = bookingService.getBookingByReference(reference, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Modify an existing confirmed booking")
    public ResponseEntity<ApiResponse<BookingResponse>> modifyBooking(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id,
            @Valid @RequestBody CreateBookingRequest request) {
        BookingResponse response = bookingService.modifyBooking(userDetails.getUsername(), id, request);
        return ResponseEntity.ok(ApiResponse.success("Booking updated successfully", response));
    }

    @DeleteMapping("/{id}/cancel")
    @Operation(summary = "Cancel a booking")
    public ResponseEntity<ApiResponse<BookingResponse>> cancelBooking(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id,
            @RequestParam(required = false) String reason) {
        BookingResponse response = bookingService.cancelBooking(userDetails.getUsername(), id, reason);
        return ResponseEntity.ok(ApiResponse.success("Booking cancelled successfully", response));
    }
}
