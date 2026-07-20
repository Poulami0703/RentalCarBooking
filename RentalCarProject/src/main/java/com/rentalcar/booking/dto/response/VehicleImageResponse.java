package com.rentalcar.booking.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleImageResponse {
    private Long id;
    private String imageUrl;
    private String caption;
    private boolean primary;
    private Integer displayOrder;
}
