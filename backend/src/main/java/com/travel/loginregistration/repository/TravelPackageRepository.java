package com.travel.loginregistration.repository;

import com.travel.loginregistration.model.TravelPackage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TravelPackageRepository extends JpaRepository<TravelPackage, UUID> {
    List<TravelPackage> findByActiveTrueOrderByNameAsc();
}

