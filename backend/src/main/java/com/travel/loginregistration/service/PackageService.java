package com.travel.loginregistration.service;

import com.travel.loginregistration.dto.PackageDetails;
import com.travel.loginregistration.dto.ItineraryItem;
import com.travel.loginregistration.dto.PackageSummary;
import com.travel.loginregistration.model.PackageItinerary;
import com.travel.loginregistration.model.TravelPackage;
import com.travel.loginregistration.repository.PackageItineraryRepository;
import com.travel.loginregistration.repository.TravelPackageRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/*
    Controllers call it whenever the frontend needs package data.
    It interacts with the TravelPackageRepository to fetch package info from the database.
    
*/

@Service
public class PackageService {
    private final TravelPackageRepository repo;
    private final PackageItineraryRepository itineraryRepo;

    public PackageService(TravelPackageRepository repo, PackageItineraryRepository itineraryRepo) {
        this.repo = repo;
        this.itineraryRepo = itineraryRepo;
    }

    public List<PackageSummary> listActive() {
        return repo.findByActiveTrueOrderByNameAsc().stream().map(this::toSummary).collect(Collectors.toList());
    }

    public PackageDetails details(UUID id) {
        TravelPackage p = repo.findById(id).orElseThrow(() -> new IllegalArgumentException("Package not found"));
        List<PackageItinerary> steps = itineraryRepo.findByTravelPackageIdOrderByDayNumberAsc(p.getId());
        return toDetails(p, steps);
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

    private PackageDetails toDetails(TravelPackage p, List<PackageItinerary> steps) {
        PackageDetails d = new PackageDetails();
        d.id = p.getId();
        d.name = p.getName();
        d.location = p.getLocation();
        d.basePrice = p.getBasePrice();
        d.destImageUrl = p.getDestImageUrl();
        d.hotelImageUrl = p.getHotelImageUrl();
        d.image1 = p.getImage1();
        d.image2 = p.getImage2();
        d.image3 = p.getImage3();
        d.image4 = p.getImage4();
        d.image5 = p.getImage5();
        d.overview = p.getOverview();
        d.locationPoints = p.getLocationPoints();
        d.timing = p.getTiming();
        d.itinerary = steps.stream().map(this::toItineraryItem).collect(Collectors.toList());
        d.groupSize = p.getGroupSize();
        d.bookingDeadline = p.getBookingDeadline();
        return d;
    }

    private ItineraryItem toItineraryItem(PackageItinerary it) {
        ItineraryItem i = new ItineraryItem();
        i.dayNumber = it.getDayNumber();
        i.title = it.getTitle();
        i.subtitle = it.getSubtitle();
        return i;
    }
}

