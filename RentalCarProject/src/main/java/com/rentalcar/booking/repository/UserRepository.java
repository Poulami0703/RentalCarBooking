package com.rentalcar.booking.repository;

import com.rentalcar.booking.entity.User;
import com.rentalcar.booking.enums.AuthProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByPhoneNumber(String phoneNumber);

    Optional<User> findByPhoneNumber(String phoneNumber);

    Optional<User> findBySocialIdAndAuthProvider(String socialId, AuthProvider provider);

    @Query("SELECT u FROM User u WHERE u.email = :email AND u.enabled = true")
    Optional<User> findActiveByEmail(@Param("email") String email);

    @Query("SELECT u FROM User u WHERE u.otpCode = :otp AND u.phoneNumber = :phone")
    Optional<User> findByOtpAndPhone(@Param("otp") String otp, @Param("phone") String phone);
}
