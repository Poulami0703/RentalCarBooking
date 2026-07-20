package com.rentalcar.booking.controller;

import com.rentalcar.booking.dto.request.VehicleSearchRequest;
import com.rentalcar.booking.dto.response.ApiResponse;
import com.rentalcar.booking.dto.response.VehicleResponse;
import com.rentalcar.booking.service.VehicleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/vehicles")
@RequiredArgsConstructor
@Tag(name = "Vehicles", description = "Vehicle search and details")
public class VehicleController {

    private final VehicleService vehicleService;

    @GetMapping("/search")
    @Operation(summary = "Search available vehicles with filters")
    public ResponseEntity<ApiResponse<Page<VehicleResponse>>> searchVehicles(
            VehicleSearchRequest request) {
        Page<VehicleResponse> results = vehicleService.searchVehicles(request);
        return ResponseEntity.ok(ApiResponse.success(results));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get vehicle details by ID")
    public ResponseEntity<ApiResponse<VehicleResponse>> getVehicle(@PathVariable Long id) {
        VehicleResponse response = vehicleService.getVehicleById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/cities")
    @Operation(summary = "Get all available pickup cities")
    public ResponseEntity<ApiResponse<List<String>>> getCities() {
        return ResponseEntity.ok(ApiResponse.success(vehicleService.getAllCities()));
    }

    @GetMapping("/brands")
    @Operation(summary = "Get all available vehicle brands")
    public ResponseEntity<ApiResponse<List<String>>> getBrands() {
        return ResponseEntity.ok(ApiResponse.success(vehicleService.getAllBrands()));
    }
}
