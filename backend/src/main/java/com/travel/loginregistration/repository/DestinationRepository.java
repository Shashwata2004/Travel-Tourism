package com.travel.loginregistration.repository;

import com.travel.loginregistration.model.Destination;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DestinationRepository extends JpaRepository<Destination, UUID> {
    List<Destination> findByActiveTrueOrderByNameAsc();
    List<Destination> findByActiveTrueAndNameContainingIgnoreCaseOrderByNameAsc(String name);
    boolean existsByNameIgnoreCaseAndActiveTrue(String name);
}
