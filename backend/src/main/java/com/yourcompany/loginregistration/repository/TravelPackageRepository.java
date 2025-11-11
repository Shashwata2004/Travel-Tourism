package com.yourcompany.loginregistration.repository;

import com.yourcompany.loginregistration.model.TravelPackage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TravelPackageRepository extends JpaRepository<TravelPackage, UUID> {
    List<TravelPackage> findByActiveTrueOrderByNameAsc();
}

