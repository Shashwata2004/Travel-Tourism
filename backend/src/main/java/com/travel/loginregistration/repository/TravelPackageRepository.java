package com.travel.loginregistration.repository;

import com.travel.loginregistration.model.TravelPackage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

// Repository for accessing the travel_packages table using the TravelPackage entity
public interface TravelPackageRepository extends JpaRepository<TravelPackage, UUID> {
    List<TravelPackage> findByActiveTrueOrderByNameAsc();
    long countByLocationIgnoreCaseAndActiveTrue(String location);
    Optional<TravelPackage> findFirstByLocationIgnoreCaseAndActiveTrueOrderByNameAsc(String location);
}

