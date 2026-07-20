package com.rentalcar.booking.controller;

import com.rentalcar.booking.dto.request.CreateReviewRequest;
import com.rentalcar.booking.dto.response.ApiResponse;
import com.rentalcar.booking.dto.response.ReviewResponse;
import com.rentalcar.booking.service.ReviewService;
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
@RequestMapping("/api/v1/reviews")
@RequiredArgsConstructor
@Tag(name = "Reviews & Ratings", description = "Submit and view vehicle reviews")
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping
    @Operation(summary = "Submit a review for a completed booking")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ApiResponse<ReviewResponse>> createReview(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CreateReviewRequest request) {
        ReviewResponse response = reviewService.createReview(userDetails.getUsername(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Review submitted, pending approval", response));
    }

    @GetMapping("/vehicle/{vehicleId}")
    @Operation(summary = "Get all approved reviews for a vehicle")
    public ResponseEntity<ApiResponse<Page<ReviewResponse>>> getVehicleReviews(
            @PathVariable Long vehicleId,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<ReviewResponse> reviews = reviewService.getVehicleReviews(vehicleId, pageable);
        return ResponseEntity.ok(ApiResponse.success(reviews));
    }

    @GetMapping("/my")
    @Operation(summary = "Get all reviews submitted by the current user")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ApiResponse<Page<ReviewResponse>>> getMyReviews(
            @AuthenticationPrincipal UserDetails userDetails,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<ReviewResponse> reviews = reviewService.getMyReviews(userDetails.getUsername(), pageable);
        return ResponseEntity.ok(ApiResponse.success(reviews));
    }
}
