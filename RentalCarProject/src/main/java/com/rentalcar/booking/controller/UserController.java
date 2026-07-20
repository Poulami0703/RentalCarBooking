package com.rentalcar.booking.controller;

import com.rentalcar.booking.dto.request.UpdateProfileRequest;
import com.rentalcar.booking.dto.request.KycRequest;
import com.rentalcar.booking.dto.response.ApiResponse;
import com.rentalcar.booking.dto.response.UserResponse;
import com.rentalcar.booking.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "User Profile", description = "Profile management and KYC endpoints")
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    @Operation(summary = "Get the currently authenticated user's profile")
    public ResponseEntity<ApiResponse<UserResponse>> getMyProfile(
            @AuthenticationPrincipal UserDetails userDetails) {
        UserResponse response = userService.getUserProfile(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/me")
    @Operation(summary = "Update the currently authenticated user's profile")
    public ResponseEntity<ApiResponse<UserResponse>> updateProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody UpdateProfileRequest request) {
        UserResponse response = userService.updateProfile(userDetails.getUsername(), request);
        return ResponseEntity.ok(ApiResponse.success("Profile updated successfully", response));
    }

    @PostMapping("/me/kyc")
    @Operation(summary = "Submit KYC documents (driving license)")
    public ResponseEntity<ApiResponse<UserResponse>> submitKyc(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody KycRequest request) {
        UserResponse response = userService.submitKyc(userDetails.getUsername(), request);
        return ResponseEntity.ok(ApiResponse.success("KYC documents submitted for review", response));
    }
}
