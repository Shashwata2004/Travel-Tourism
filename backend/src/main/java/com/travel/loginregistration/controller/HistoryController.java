package com.travel.loginregistration.controller;

import com.travel.loginregistration.dto.HistoryPackageItem;
import com.travel.loginregistration.dto.HistoryResponse;
import com.travel.loginregistration.dto.HistoryRoomItem;
import com.travel.loginregistration.model.Booking;
import com.travel.loginregistration.model.HotelRoomBooking;
import com.travel.loginregistration.model.TravelPackage;
import com.travel.loginregistration.model.User;
import com.travel.loginregistration.repository.BookingRepository;
import com.travel.loginregistration.repository.HotelRoomBookingRepository;
import com.travel.loginregistration.repository.TravelPackageRepository;
import com.travel.loginregistration.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/history")
public class HistoryController {

    private final BookingRepository bookingRepository;
    private final HotelRoomBookingRepository roomBookingRepository;
    private final TravelPackageRepository travelPackageRepository;
    private final UserRepository userRepository;

    public HistoryController(BookingRepository bookingRepository,
                             HotelRoomBookingRepository roomBookingRepository,
                             TravelPackageRepository travelPackageRepository,
                             UserRepository userRepository) {
        this.bookingRepository = bookingRepository;
        this.roomBookingRepository = roomBookingRepository;
        this.travelPackageRepository = travelPackageRepository;
        this.userRepository = userRepository;
    }

    @GetMapping
    public ResponseEntity<?> history(Authentication auth) {
        try {
            String email = (String) auth.getPrincipal();
            if (email == null) return ResponseEntity.badRequest().body("No user");
            User user = userRepository.findByEmail(email.toLowerCase(Locale.ROOT))
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));
            UUID userId = user.getId();

            String emailLower = user.getEmail() == null ? null : user.getEmail().toLowerCase(Locale.ROOT);
            List<HotelRoomBooking> roomBookings = roomBookingRepository.findByUserIdOrUserEmailOrderByCreatedAtDesc(userId, emailLower);
            List<Booking> packageBookings = bookingRepository.findByUserIdOrUserEmailOrderByCreatedAtDesc(userId, emailLower);

            HistoryResponse resp = new HistoryResponse();
            resp.rooms = roomBookings.stream().map(this::mapRoom).collect(Collectors.toList());
            resp.packages = packageBookings.stream().map(this::mapPackage).collect(Collectors.toList());
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    private HistoryRoomItem mapRoom(HotelRoomBooking b) {
        HistoryRoomItem dto = new HistoryRoomItem();
        dto.id = b.getId();
        dto.hotelName = b.getHotelName();
        dto.roomName = b.getRoomName();
        dto.checkIn = b.getCheckIn();
        dto.checkOut = b.getCheckOut();
        dto.totalGuests = b.getTotalGuests();
        dto.roomsBooked = b.getRoomsBooked();
        dto.totalPrice = b.getTotalPrice();
        dto.createdAt = b.getCreatedAt();
        dto.status = "Upcoming"; // placeholder
        return dto;
    }

    private HistoryPackageItem mapPackage(Booking b) {
        HistoryPackageItem dto = new HistoryPackageItem();
        dto.id = b.getId();
        dto.packageId = b.getPackageId();
        dto.totalPersons = b.getTotalPersons();
        dto.totalPrice = b.getPriceTotal();
        dto.createdAt = b.getCreatedAt();
        dto.status = "Upcoming"; // placeholder
        dto.packageName = null;
        dto.location = null;
        dto.bookingDeadline = null;
        if (b.getPackageId() != null) {
            travelPackageRepository.findById(b.getPackageId()).ifPresent(p -> {
                dto.packageName = p.getName();
                dto.location = p.getLocation();
                dto.bookingDeadline = p.getBookingDeadline();
                dto.durationDays = parseDurationDays(p.getTiming());
            });
        }
        return dto;
    }

    private Integer parseDurationDays(String timing) {
        if (timing == null) return null;
        // Expect formats like "3 days, 2 nights" or "3 Days ,2 Nights"
        String lower = timing.toLowerCase(Locale.ROOT);
        int idx = lower.indexOf("day");
        if (idx == -1) return null;
        String prefix = lower.substring(0, idx).replaceAll("[^0-9]", "");
        try {
            return prefix.isBlank() ? null : Integer.parseInt(prefix);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
