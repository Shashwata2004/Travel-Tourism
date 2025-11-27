package com.travel.loginregistration.repository;

import com.travel.loginregistration.model.Hotel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface HotelRepository extends JpaRepository<Hotel, UUID> {
    List<Hotel> findByDestinationIdOrderByNameAsc(UUID destinationId);
    long countByDestinationId(UUID destinationId);
}
