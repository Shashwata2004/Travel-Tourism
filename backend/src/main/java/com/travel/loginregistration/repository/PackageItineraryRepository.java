package com.travel.loginregistration.repository;

import com.travel.loginregistration.model.PackageItinerary;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PackageItineraryRepository extends JpaRepository<PackageItinerary, Long> {
    List<PackageItinerary> findByTravelPackageIdOrderByDayNumberAsc(UUID packageId);
    void deleteByTravelPackageId(UUID packageId);
}
