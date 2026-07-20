package com.rentalcar.booking.config;

import com.rentalcar.booking.entity.User;
import com.rentalcar.booking.entity.Vehicle;
import com.rentalcar.booking.entity.VehicleImage;
import com.rentalcar.booking.enums.*;
import com.rentalcar.booking.repository.UserRepository;
import com.rentalcar.booking.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
@Profile("!test")
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final VehicleRepository vehicleRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) {
            log.info("Database already seeded, skipping initialization.");
            return;
        }

        log.info("Seeding database with sample data...");
        seedUsers();
        seedVehicles();
        log.info("Database seeding complete.");
    }

    private void seedUsers() {
        // Admin user
        User admin = User.builder()
                .firstName("Admin")
                .lastName("User")
                .email("admin@rentalcar.com")
                .passwordHash(passwordEncoder.encode("Admin@12345"))
                .role(UserRole.ADMIN)
                .authProvider(AuthProvider.LOCAL)
                .emailVerified(true)
                .phoneNumber("+919876543210")
                .kycStatus(KycStatus.APPROVED)
                .build();
        userRepository.save(admin);

        // Customer user
        User customer = User.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .passwordHash(passwordEncoder.encode("Customer@12345"))
                .role(UserRole.CUSTOMER)
                .authProvider(AuthProvider.LOCAL)
                .emailVerified(true)
                .phoneNumber("+919123456789")
                .city("Mumbai")
                .state("Maharashtra")
                .country("India")
                .kycStatus(KycStatus.APPROVED)
                .drivingLicenseNumber("MH0120230012345")
                .licenseExpiryDate(LocalDate.of(2028, 6, 30))
                .build();
        userRepository.save(customer);

        log.info("Seeded users: admin@rentalcar.com / Admin@12345  |  john.doe@example.com / Customer@12345");
    }

    private void seedVehicles() {
        Vehicle suv1 = createVehicle("Toyota", "Fortuner", 2023, "MH01AB1234",
                CarType.SUV, FuelType.DIESEL, TransmissionType.AUTOMATIC,
                7, new BigDecimal("4500.00"), new BigDecimal("300.00"),
                "White", "Mumbai", "Andheri West, Mumbai",
                "7-seater SUV, perfect for long trips. Features include: leather seats, sunroof, cruise control.",
                true);

        Vehicle sedan1 = createVehicle("Honda", "City", 2023, "MH01CD5678",
                CarType.SEDAN, FuelType.PETROL, TransmissionType.AUTOMATIC,
                5, new BigDecimal("2500.00"), new BigDecimal("200.00"),
                "Silver", "Mumbai", "Bandra, Mumbai",
                "Comfortable sedan with advanced safety features, touchscreen infotainment, rear camera.",
                false);

        Vehicle hatchback1 = createVehicle("Maruti", "Swift", 2023, "DL01EF9012",
                CarType.HATCHBACK, FuelType.PETROL, TransmissionType.MANUAL,
                5, new BigDecimal("1500.00"), new BigDecimal("150.00"),
                "Red", "Delhi", "Connaught Place, Delhi",
                "Fuel-efficient hatchback ideal for city driving.",
                false);

        Vehicle ev1 = createVehicle("Tata", "Nexon EV", 2024, "KA01GH3456",
                CarType.SUV, FuelType.ELECTRIC, TransmissionType.AUTOMATIC,
                5, new BigDecimal("3500.00"), new BigDecimal("250.00"),
                "Blue", "Bangalore", "Koramangala, Bangalore",
                "Zero-emission electric SUV with 312km range. Fast charging support.",
                true);

        Vehicle luxury1 = createVehicle("Mercedes-Benz", "E-Class", 2024, "MH02IJ7890",
                CarType.LUXURY, FuelType.PETROL, TransmissionType.AUTOMATIC,
                5, new BigDecimal("8500.00"), new BigDecimal("600.00"),
                "Black", "Mumbai", "Juhu, Mumbai",
                "Premium luxury sedan. Chauffeur driven experience available. Air suspension, MBUX system.",
                true);

        // Add images to vehicles
        addImage(suv1, "https://images.unsplash.com/photo-toyota-fortuner", "Exterior front view", true, 1);
        addImage(sedan1, "https://images.unsplash.com/photo-honda-city", "Exterior side view", true, 1);
        addImage(hatchback1, "https://images.unsplash.com/photo-swift", "Exterior front view", true, 1);
        addImage(ev1, "https://images.unsplash.com/photo-tata-nexon", "Exterior front view", true, 1);
        addImage(luxury1, "https://images.unsplash.com/photo-mercedes-e", "Exterior front view", true, 1);

        vehicleRepository.saveAll(List.of(suv1, sedan1, hatchback1, ev1, luxury1));
        log.info("Seeded 5 sample vehicles");
    }

    private Vehicle createVehicle(String brand, String model, int year, String plate,
                                   CarType carType, FuelType fuelType, TransmissionType transmission,
                                   int seats, BigDecimal pricePerDay, BigDecimal pricePerHour,
                                   String color, String city, String location, String description,
                                   boolean chauffeur) {
        return Vehicle.builder()
                .brand(brand)
                .model(model)
                .year(year)
                .licensePlate(plate)
                .carType(carType)
                .fuelType(fuelType)
                .transmissionType(transmission)
                .seatingCapacity(seats)
                .pricePerDay(pricePerDay)
                .pricePerHour(pricePerHour)
                .color(color)
                .city(city)
                .pickupLocation(location)
                .description(description)
                .status(VehicleStatus.AVAILABLE)
                .chauffeurAvailable(chauffeur)
                .averageRating(0.0)
                .totalRatings(0)
                .totalBookings(0)
                .build();
    }

    private void addImage(Vehicle vehicle, String url, String caption, boolean primary, int order) {
        VehicleImage image = VehicleImage.builder()
                .vehicle(vehicle)
                .imageUrl(url)
                .caption(caption)
                .primary(primary)
                .displayOrder(order)
                .build();
        vehicle.getImages().add(image);
    }
}
