package com.travel.loginregistration.controller;

import com.travel.loginregistration.dto.PackageBookingAdminResponse;
import com.travel.loginregistration.dto.PackageBookingAdminView;
import com.travel.loginregistration.model.Booking;
import com.travel.loginregistration.model.TravelPackage;
import com.travel.loginregistration.repository.BookingRepository;
import com.travel.loginregistration.repository.UserRepository;
import com.travel.loginregistration.repository.UserProfileRepository;
import com.travel.loginregistration.repository.TravelPackageRepository;
import com.travel.loginregistration.service.BookingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/packages")
public class AdminPackageBookingController {

    private final BookingRepository bookingRepository;
    private final TravelPackageRepository travelPackageRepository;
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final BookingService bookingService;

    public AdminPackageBookingController(BookingRepository bookingRepository,
                                         TravelPackageRepository travelPackageRepository,
                                         UserRepository userRepository,
                                         UserProfileRepository userProfileRepository,
                                         BookingService bookingService) {
        this.bookingRepository = bookingRepository;
        this.travelPackageRepository = travelPackageRepository;
        this.userRepository = userRepository;
        this.userProfileRepository = userProfileRepository;
        this.bookingService = bookingService;
    }

    @GetMapping("/{packageId}/bookings")
    public ResponseEntity<PackageBookingAdminResponse> bookings(@PathVariable UUID packageId) {
        try {
            TravelPackage pkg = travelPackageRepository.findById(packageId)
                    .orElseThrow(() -> new IllegalArgumentException("PACKAGE_NOT_FOUND"));
            List<Booking> bookings = bookingRepository.findByPackageIdOrderByCreatedAtDesc(packageId);
            long persons = bookingRepository.sumPersonsForPackage(packageId);

            PackageBookingAdminResponse res = new PackageBookingAdminResponse();
            res.packageId = pkg.getId();
            res.packageName = pkg.getName();
            res.bookingDeadline = pkg.getBookingDeadline();
            res.totalPersons = persons;
            res.items = bookings.stream().map(b -> toView(b, pkg.getName())).collect(Collectors.toList());
            return ResponseEntity.ok(res);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // List all package bookings (all packages)
    @GetMapping("/bookings")
    public ResponseEntity<List<PackageBookingAdminView>> all() {
        List<Booking> bookings = bookingRepository.findAllByOrderByCreatedAtDesc();
        var names = travelPackageRepository.findAll().stream()
                .collect(java.util.stream.Collectors.toMap(TravelPackage::getId, TravelPackage::getName));
        List<PackageBookingAdminView> items = bookings.stream()
                .map(b -> toView(b, names.get(b.getPackageId())))
                .collect(Collectors.toList());
        return ResponseEntity.ok(items);
    }

    @PostMapping("/bookings/{bookingId}/cancel")
    public ResponseEntity<?> adminCancel(@PathVariable UUID bookingId) {
        try {
            return ResponseEntity.ok(bookingService.adminCancel(bookingId, "ADMIN"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    private PackageBookingAdminView toView(Booking b, String packageName) {
        PackageBookingAdminView v = new PackageBookingAdminView();
        v.id = b.getId();
        v.packageId = b.getPackageId();
        v.packageName = packageName;
        String email = b.getUserEmail();
        if ((email == null || email.isBlank()) && b.getUserId() != null) {
            email = userRepository.findById(b.getUserId()).map(u -> u.getEmail()).orElse(null);
        }
        v.userEmail = email;
        v.customerName = b.getCustomerName();
        String idType = b.getIdType();
        if ((idType == null || idType.isBlank()) && b.getUserId() != null) {
            idType = userProfileRepository.findByUserId(b.getUserId())
                    .map(p -> p.getIdType())
                    .orElse(null);
        }
        v.idType = idType;
        v.idNumber = b.getIdNumber();
        v.totalPersons = b.getTotalPersons();
        v.priceTotal = b.getPriceTotal();
        v.createdAt = b.getCreatedAt();
        v.bookingDeadline = travelPackageRepository.findById(b.getPackageId())
                .map(TravelPackage::getBookingDeadline)
                .orElse(null);
        v.transactionId = b.getTransactionId();
        v.status = b.getStatus();
        v.canceledAt = b.getCanceledAt();
        v.canceledBy = b.getCanceledBy();
        return v;
    }
}
