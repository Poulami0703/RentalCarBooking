package com.rentalcar.booking.repository;

import com.rentalcar.booking.entity.VehicleImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VehicleImageRepository extends JpaRepository<VehicleImage, Long> {

    List<VehicleImage> findByVehicleIdOrderByDisplayOrderAsc(Long vehicleId);

    Optional<VehicleImage> findByVehicleIdAndPrimaryTrue(Long vehicleId);

    void deleteByVehicleId(Long vehicleId);
}
