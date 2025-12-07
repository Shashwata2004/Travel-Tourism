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
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;

/*
    handles booking travel packages for users, ensuring eligibility and logging bookings
    bookingcontroller calls this service with booking request data and user email from jwt
    saves booking to database and logs each booking to a file for auditing
*/

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
        // Validate incoming payload before hitting repositories
        if (req == null || req.packageId == null)
            throw new IllegalArgumentException("packageId required");
        if (req.totalPersons <= 0)
            throw new IllegalArgumentException("totalPersons must be > 0");

        // Look up user + package referenced in the request
        User user = userRepo.findByEmail(email.toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        TravelPackage pack = packageRepo.findById(req.packageId)
                .orElseThrow(() -> new IllegalArgumentException("Package not found"));
        if (pack.getBookingDeadline() != null && java.time.LocalDate.now().isAfter(pack.getBookingDeadline())) {
            throw new IllegalArgumentException("Booking deadline passed for this package");
        }

        UserProfile profile = profileRepo.findByUserId(user.getId()).orElse(null);
        // Enforce eligibility: ID Type and ID Number must be present
        if (profile == null || profile.getIdNumber() == null || profile.getIdNumber().isBlank()
                || profile.getIdType() == null || profile.getIdType().isBlank()) {
            throw new IllegalArgumentException(
                    "Complete Personal Information first: ID Type and ID Number are required to book.");
        }
        String customerName = profile.getFullName();
        String idNumber = profile.getIdNumber();

        // Calculate total cost = base price * number of persons
        BigDecimal total = pack.getBasePrice().multiply(BigDecimal.valueOf(req.totalPersons));

        // Persist booking row and mirror key fields on the response
        Booking b = new Booking();
        b.setUserId(user.getId());
        b.setPackageId(pack.getId());
        b.setTotalPersons(req.totalPersons);
        b.setPriceTotal(total);
        b.setCustomerName(customerName);
        b.setIdNumber(idNumber);
        b.setIdType(profile.getIdType());
        b.setUserEmail(user.getEmail());
        b.setCreatedAt(Instant.now());
        b.setTransactionId(generateTxnId(tx -> bookingRepo.existsByTransactionId(tx)));
        b.setCardLast4(generateLast4());
        bookingRepo.save(b); // booking saved to database

        logToFile(b, user.getEmail(), pack.getName());

        BookingResponse res = new BookingResponse();
        res.id = b.getId();
        res.packageId = b.getPackageId();
        res.totalPersons = b.getTotalPersons();
        res.priceTotal = b.getPriceTotal();
        res.createdAt = b.getCreatedAt();
        res.transactionId = b.getTransactionId();
        res.cardLast4 = b.getCardLast4();
        return res;
    }

    // Append each booking to backend/bookings.log for manual auditing
    private void logToFile(Booking b, String email, String packageName) {
        try {
            String line = String.format(
                    "%s | booking %s | user=%s | package=%s | persons=%d | total=%s | id=%s | name=%s%n",
                    Instant.now(), b.getId(), email, packageName, b.getTotalPersons(), b.getPriceTotal(),
                    b.getIdNumber(), b.getCustomerName());
            Path p = Path.of("bookings.log");
            Files.writeString(p, line, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (Exception ignore) {
            // Fail-safe: ignore file logging errors
        }
    }

    private String generateTxnId(Predicate<String> exists) {
        for (int i = 0; i < 20; i++) {
            int num = ThreadLocalRandom.current().nextInt(0, 1_000_000);
            String tx = "TXN-" + String.format("%06d", num);
            if (!exists.test(tx))
                return tx;
        }
        throw new RuntimeException("Could not generate unique transaction ID");
    }

    private String generateLast4() {
        return String.format("%04d", ThreadLocalRandom.current().nextInt(0, 10000));
    }
}
