package com.travel.loginregistration.service;

import com.travel.loginregistration.dto.BookingRequest;
import com.travel.loginregistration.dto.BookingResponse;
import com.travel.loginregistration.model.Booking;
import com.travel.loginregistration.model.TravelPackage;
import com.travel.loginregistration.model.User;
import com.travel.loginregistration.model.UserProfile;
import com.travel.loginregistration.repository.BookingRepository;
import com.travel.loginregistration.repository.TravelPackageRepository;
import com.travel.loginregistration.repository.UserProfileRepository;
import com.travel.loginregistration.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.Locale;

@Service
public class BookingService {
    private final BookingRepository bookingRepo;
    private final TravelPackageRepository packageRepo;
    private final UserRepository userRepo;
    private final UserProfileRepository profileRepo;

    public BookingService(BookingRepository bookingRepo,
                          TravelPackageRepository packageRepo,
                          UserRepository userRepo,
                          UserProfileRepository profileRepo) {
        this.bookingRepo = bookingRepo;
        this.packageRepo = packageRepo;
        this.userRepo = userRepo;
        this.profileRepo = profileRepo;
    }

    @Transactional
    public BookingResponse book(String email, BookingRequest req) {
        if (req == null || req.packageId == null) throw new IllegalArgumentException("packageId required");
        if (req.totalPersons <= 0) throw new IllegalArgumentException("totalPersons must be > 0");

        User user = userRepo.findByEmail(email.toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        TravelPackage pack = packageRepo.findById(req.packageId)
                .orElseThrow(() -> new IllegalArgumentException("Package not found"));

        UserProfile profile = profileRepo.findByUserId(user.getId()).orElse(null);
        // Enforce eligibility: ID Type and ID Number must be present
        if (profile == null || profile.getIdNumber() == null || profile.getIdNumber().isBlank()
                || profile.getIdType() == null || profile.getIdType().isBlank()) {
            throw new IllegalArgumentException("Complete Personal Information first: ID Type and ID Number are required to book.");
        }
        String customerName = profile.getFullName();
        String idNumber = profile.getIdNumber();

        BigDecimal total = pack.getBasePrice().multiply(BigDecimal.valueOf(req.totalPersons));

        Booking b = new Booking();
        b.setUserId(user.getId());
        b.setPackageId(pack.getId());
        b.setTotalPersons(req.totalPersons);
        b.setPriceTotal(total);
        b.setCustomerName(customerName);
        b.setIdNumber(idNumber);
        b.setCreatedAt(Instant.now());
        bookingRepo.save(b);

        logToFile(b, user.getEmail(), pack.getName());

        BookingResponse res = new BookingResponse();
        res.id = b.getId();
        res.packageId = b.getPackageId();
        res.totalPersons = b.getTotalPersons();
        res.priceTotal = b.getPriceTotal();
        res.createdAt = b.getCreatedAt();
        return res;
    }

    private void logToFile(Booking b, String email, String packageName) {
        try {
            String line = String.format("%s | booking %s | user=%s | package=%s | persons=%d | total=%s | id=%s | name=%s%n",
                    Instant.now(), b.getId(), email, packageName, b.getTotalPersons(), b.getPriceTotal(), b.getIdNumber(), b.getCustomerName());
            Path p = Path.of("bookings.log");
            Files.writeString(p, line, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (Exception ignore) {
            // Fail-safe: ignore file logging errors
        }
    }
}
