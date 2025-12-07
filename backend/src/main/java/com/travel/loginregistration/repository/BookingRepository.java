package com.travel.loginregistration.repository;

import com.travel.loginregistration.model.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

// Repository for accessing the bookings table using the Booking entity
public interface BookingRepository extends JpaRepository<Booking, UUID> {
    List<Booking> findByUserIdOrderByCreatedAtDesc(UUID userId);
    List<Booking> findByUserIdOrUserEmailOrderByCreatedAtDesc(UUID userId, String userEmail);
    List<Booking> findByPackageIdOrderByCreatedAtDesc(UUID packageId);

    @Query("select coalesce(sum(b.totalPersons),0) from Booking b where b.packageId = :packageId and (b.status is null or upper(b.status) <> 'CANCELED')")
    long sumPersonsForPackage(@Param("packageId") UUID packageId);

    boolean existsByTransactionId(String transactionId);

    List<Booking> findAllByOrderByCreatedAtDesc();
}
