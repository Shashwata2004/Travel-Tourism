package com.travel.loginregistration.controller;

import com.travel.loginregistration.dto.RoomBookingAdminView;
import com.travel.loginregistration.model.HotelRoomBooking;
import com.travel.loginregistration.repository.HotelRoomBookingRepository;
import com.travel.loginregistration.repository.HotelRoomRepository;
import com.travel.loginregistration.repository.HotelRepository;
import com.travel.loginregistration.repository.DestinationRepository;
import com.travel.loginregistration.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/rooms")
public class AdminRoomBookingController {

    private final HotelRoomBookingRepository bookingRepository;
    private final HotelRoomRepository roomRepository;
    private final HotelRepository hotelRepository;
    private final DestinationRepository destinationRepository;
    private final UserRepository userRepository;

    public AdminRoomBookingController(HotelRoomBookingRepository bookingRepository,
                                      HotelRoomRepository roomRepository,
                                      HotelRepository hotelRepository,
                                      DestinationRepository destinationRepository,
                                      UserRepository userRepository) {
        this.bookingRepository = bookingRepository;
        this.roomRepository = roomRepository;
        this.hotelRepository = hotelRepository;
        this.destinationRepository = destinationRepository;
        this.userRepository = userRepository;
    }

    @GetMapping("/{roomId}/bookings")
    public ResponseEntity<List<RoomBookingAdminView>> bookings(@PathVariable UUID roomId) {
        var bookings = bookingRepository.findByRoomIdOrderByCheckInAsc(roomId);
        var roomName = roomRepository.findById(roomId).map(r -> r.getName()).orElse(null);
        var hotelName = roomRepository.findById(roomId)
                .flatMap(r -> hotelRepository.findById(r.getHotelId()))
                .map(h -> h.getName())
                .orElse(null);
        List<RoomBookingAdminView> views = bookings.stream().map(b -> toView(b, roomName, hotelName)).collect(Collectors.toList());
        return ResponseEntity.ok(views);
    }

    @GetMapping("/{roomId}/occupancy")
    public ResponseEntity<LocalDate> nextAvailable(@PathVariable UUID roomId) {
        var bookings = bookingRepository.findByRoomIdOrderByCheckInAsc(roomId).stream()
                .filter(b -> b.getStatus() == null || !"CANCELED".equalsIgnoreCase(b.getStatus()))
                .collect(Collectors.toList());
        if (bookings.isEmpty()) return ResponseEntity.ok(LocalDate.now());
        bookings.sort(Comparator.comparing(HotelRoomBooking::getCheckOut));
        LocalDate latest = bookings.get(bookings.size() - 1).getCheckOut();
        return ResponseEntity.ok(latest);
    }

    // List all room bookings across all destinations/hotels
    @GetMapping("/bookings")
    public ResponseEntity<List<RoomBookingAdminView>> allBookings() {
        var bookings = bookingRepository.findAllByOrderByCreatedAtDesc();
        List<RoomBookingAdminView> views = bookings.stream().map(b -> {
            String roomName = roomRepository.findById(b.getRoomId()).map(r -> r.getName()).orElse(null);
            String hotelName = roomRepository.findById(b.getRoomId())
                    .flatMap(r -> hotelRepository.findById(r.getHotelId()))
                    .map(h -> h.getName())
                    .orElse(null);
            return toView(b, roomName, hotelName);
        }).collect(Collectors.toList());
        return ResponseEntity.ok(views);
    }

    private RoomBookingAdminView toView(HotelRoomBooking b, String roomName, String hotelName) {
        RoomBookingAdminView v = new RoomBookingAdminView();
        v.id = b.getId();
        v.roomId = b.getRoomId();
        v.roomName = roomName;
        v.hotelName = hotelName;
        v.checkIn = b.getCheckIn();
        v.checkOut = b.getCheckOut();
        v.roomsBooked = b.getRoomsBooked();
        v.totalGuests = b.getTotalGuests();
        v.totalPrice = b.getTotalPrice();
        v.customerName = b.getCustomerName();
        v.idType = b.getIdType();
        v.idNumber = b.getIdNumber();
        String email = b.getUserEmail();
        if ((email == null || email.isBlank()) && b.getUserId() != null) {
            email = userRepository.findById(b.getUserId()).map(u -> u.getEmail()).orElse(null);
        }
        v.userEmail = email;
        v.createdAt = b.getCreatedAt();
        v.transactionId = b.getTransactionId();
        v.status = b.getStatus();
        v.canceledAt = b.getCanceledAt();
        v.canceledBy = b.getCanceledBy();
        roomRepository.findById(b.getRoomId())
                .flatMap(r -> hotelRepository.findById(r.getHotelId()))
                .flatMap(h -> destinationRepository.findById(h.getDestinationId()))
                .ifPresent(d -> v.destinationName = d.getName());
        return v;
    }
}
