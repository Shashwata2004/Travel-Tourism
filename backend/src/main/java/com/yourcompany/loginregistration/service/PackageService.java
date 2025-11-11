package com.yourcompany.loginregistration.service;

import com.yourcompany.loginregistration.dto.PackageDetails;
import com.yourcompany.loginregistration.dto.PackageSummary;
import com.yourcompany.loginregistration.model.TravelPackage;
import com.yourcompany.loginregistration.repository.TravelPackageRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class PackageService {
    private final TravelPackageRepository repo;

    public PackageService(TravelPackageRepository repo) {
        this.repo = repo;
    }

    public List<PackageSummary> listActive() {
        return repo.findByActiveTrueOrderByNameAsc().stream().map(this::toSummary).collect(Collectors.toList());
    }

    public PackageDetails details(UUID id) {
        TravelPackage p = repo.findById(id).orElseThrow(() -> new IllegalArgumentException("Package not found"));
        return toDetails(p);
    }

    private PackageSummary toSummary(TravelPackage p) {
        PackageSummary s = new PackageSummary();
        s.id = p.getId();
        s.name = p.getName();
        s.location = p.getLocation();
        s.basePrice = p.getBasePrice();
        s.destImageUrl = p.getDestImageUrl();
        return s;
    }

    private PackageDetails toDetails(TravelPackage p) {
        PackageDetails d = new PackageDetails();
        d.id = p.getId();
        d.name = p.getName();
        d.location = p.getLocation();
        d.basePrice = p.getBasePrice();
        d.destImageUrl = p.getDestImageUrl();
        d.hotelImageUrl = p.getHotelImageUrl();
        d.overview = p.getOverview();
        d.locationPoints = p.getLocationPoints();
        d.timing = p.getTiming();
        return d;
    }
}

