package com.rentalcar.booking.repository;

import com.rentalcar.booking.entity.Vehicle;
import com.rentalcar.booking.enums.CarType;
import com.rentalcar.booking.enums.FuelType;
import com.rentalcar.booking.enums.TransmissionType;
import com.rentalcar.booking.enums.VehicleStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, Long>, JpaSpecificationExecutor<Vehicle> {

    boolean existsByLicensePlate(String licensePlate);

    Page<Vehicle> findByStatus(VehicleStatus status, Pageable pageable);

    Page<Vehicle> findByCityIgnoreCaseAndStatus(String city, VehicleStatus status, Pageable pageable);

    @Query("""
            SELECT v FROM Vehicle v
            WHERE v.status = 'AVAILABLE'
            AND (:city IS NULL OR LOWER(v.city) = LOWER(:city))
            AND (:carType IS NULL OR v.carType = :carType)
            AND (:fuelType IS NULL OR v.fuelType = :fuelType)
            AND (:transmission IS NULL OR v.transmissionType = :transmission)
            AND (:brand IS NULL OR LOWER(v.brand) LIKE LOWER(CONCAT('%', :brand, '%')))
            AND (:minPrice IS NULL OR v.pricePerDay >= :minPrice)
            AND (:maxPrice IS NULL OR v.pricePerDay <= :maxPrice)
            AND (:seats IS NULL OR v.seatingCapacity >= :seats)
            AND v.id NOT IN (
                SELECT b.vehicle.id FROM Booking b
                WHERE b.status NOT IN ('CANCELLED', 'COMPLETED')
                AND NOT (b.dropoffDateTime <= :pickupDate OR b.pickupDateTime >= :dropoffDate)
            )
            """)
    Page<Vehicle> searchAvailableVehicles(
            @Param("city") String city,
            @Param("carType") CarType carType,
            @Param("fuelType") FuelType fuelType,
            @Param("transmission") TransmissionType transmission,
            @Param("brand") String brand,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            @Param("seats") Integer seats,
            @Param("pickupDate") LocalDateTime pickupDate,
            @Param("dropoffDate") LocalDateTime dropoffDate,
            Pageable pageable
    );

    @Query("SELECT DISTINCT v.city FROM Vehicle v WHERE v.city IS NOT NULL ORDER BY v.city")
    List<String> findAllCities();

    @Query("SELECT DISTINCT v.brand FROM Vehicle v WHERE v.brand IS NOT NULL ORDER BY v.brand")
    List<String> findAllBrands();

    @Query("SELECT v FROM Vehicle v WHERE v.id = :id AND v.status = 'AVAILABLE'")
    java.util.Optional<Vehicle> findAvailableById(@Param("id") Long id);
}
