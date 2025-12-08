package com.travel.loginregistration.service;

import com.travel.loginregistration.dto.RoomBookingRequest;
import com.travel.loginregistration.dto.RoomBookingResponse;
import com.travel.loginregistration.model.HotelRoom;
import com.travel.loginregistration.model.HotelRoomBooking;
import com.travel.loginregistration.repository.HotelRepository;
import com.travel.loginregistration.repository.HotelRoomBookingRepository;
import com.travel.loginregistration.repository.HotelRoomRepository;
import com.travel.loginregistration.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;
import java.util.Locale;
import java.util.UUID;

@Service
public class HotelBookingService {

    private final HotelRoomRepository roomRepository;
    private final HotelRoomBookingRepository bookingRepository;
    private final HotelRepository hotelRepository;
    private final UserRepository userRepository;

    public HotelBookingService(HotelRoomRepository roomRepository,
            HotelRoomBookingRepository bookingRepository,
            HotelRepository hotelRepository,
            UserRepository userRepository) {
        this.roomRepository = roomRepository;
        this.bookingRepository = bookingRepository;
        this.hotelRepository = hotelRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public RoomBookingResponse book(RoomBookingRequest req, String authEmail) {
        validate(req);
        HotelRoom room = roomRepository.findById(req.roomId)
                .orElseThrow(() -> new IllegalArgumentException("ROOM_NOT_FOUND"));

        int capacity = room.getTotalRooms() != null ? room.getTotalRooms()
                : (room.getAvailableRooms() != null ? room.getAvailableRooms() : 0);
        Integer booked = bookingRepository.sumBookedBetween(room.getId(), req.checkIn, req.checkOut);
        int remaining = capacity - (booked == null ? 0 : booked.intValue());
        if (remaining < req.rooms) {
            throw new IllegalArgumentException("INSUFFICIENT_ROOMS");
        }

        // Try to associate booking with authenticated user if client didn't send userId
        if (req.userId == null && authEmail != null && !authEmail.isBlank()) {
            userRepository.findByEmail(authEmail.toLowerCase(Locale.ROOT)).ifPresent(u -> req.userId = u.getId());
        }

        HotelRoomBooking b = new HotelRoomBooking();
        b.setRoomId(room.getId());
        b.setCheckIn(req.checkIn);
        b.setCheckOut(req.checkOut);
        b.setRoomsBooked(req.rooms);
        b.setCreatedAt(java.time.Instant.now());
        b.setTotalGuests(req.totalGuests <= 0 ? null : req.totalGuests);
        b.setTotalPrice(req.totalPrice == null ? BigDecimal.ZERO : req.totalPrice);
        b.setUserId(req.userId);
        b.setRoomName(room.getName());
        hotelRepository.findById(room.getHotelId()).ifPresent(h -> b.setHotelName(h.getName()));
        b.setCustomerName(req.customerName);
        b.setIdType(req.idType);
        b.setIdNumber(req.idNumber);
        if (req.userId != null) {
            userRepository.findById(req.userId).ifPresent(u -> b.setUserEmail(u.getEmail()));
        } else if (authEmail != null) {
            b.setUserEmail(authEmail.toLowerCase(Locale.ROOT));
        }
        b.setTransactionId(generateTxnId(tx -> bookingRepository.existsByTransactionId(tx)));
        b.setCardLast4(generateLast4());
        b.setStatus("CONFIRMED");
        bookingRepository.save(b);

        RoomBookingResponse res = new RoomBookingResponse();
        res.id = b.getId();
        res.roomId = b.getRoomId();
        res.checkIn = b.getCheckIn();
        res.checkOut = b.getCheckOut();
        res.rooms = b.getRoomsBooked();
        res.createdAt = b.getCreatedAt();
        res.totalGuests = b.getTotalGuests() == null ? 0 : b.getTotalGuests();
        res.totalPrice = b.getTotalPrice();
        res.userId = b.getUserId();
        res.roomName = b.getRoomName();
        res.hotelName = b.getHotelName();
        res.customerName = b.getCustomerName();
        res.idType = b.getIdType();
        res.idNumber = b.getIdNumber();
        res.transactionId = b.getTransactionId();
        res.cardLast4 = b.getCardLast4();
        res.status = b.getStatus();
        res.canceledAt = b.getCanceledAt();
        res.canceledBy = b.getCanceledBy();
        return res;
    }

    @Transactional
    public RoomBookingResponse cancel(UUID bookingId, String authEmail) {
        String emailNorm = authEmail == null ? null : authEmail.toLowerCase(Locale.ROOT);
        HotelRoomBooking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("BOOKING_NOT_FOUND"));
        if ("CANCELED".equalsIgnoreCase(booking.getStatus())) {
            throw new IllegalArgumentException("BOOKING_ALREADY_CANCELED");
        }
        boolean allowed = false;
        if (booking.getUserEmail() != null && emailNorm != null
                && booking.getUserEmail().equalsIgnoreCase(emailNorm)) {
            allowed = true;
        }
        if (!allowed && booking.getUserId() != null) {
            allowed = userRepository.findById(booking.getUserId())
                    .map(u -> u.getEmail() != null && u.getEmail().equalsIgnoreCase(emailNorm))
                    .orElse(false);
        }
        if (!allowed) {
            throw new IllegalArgumentException("CANNOT_CANCEL_FOR_ANOTHER_USER");
        }
        booking.setStatus("CANCELED");
        booking.setCanceledAt(java.time.Instant.now());
        booking.setCanceledBy("USER");
        bookingRepository.save(booking);

        RoomBookingResponse res = new RoomBookingResponse();
        res.id = booking.getId();
        res.roomId = booking.getRoomId();
        res.checkIn = booking.getCheckIn();
        res.checkOut = booking.getCheckOut();
        res.rooms = booking.getRoomsBooked() == null ? 0 : booking.getRoomsBooked();
        res.totalGuests = booking.getTotalGuests() == null ? 0 : booking.getTotalGuests();
        res.totalPrice = booking.getTotalPrice();
        res.createdAt = booking.getCreatedAt();
        res.userId = booking.getUserId();
        res.roomName = booking.getRoomName();
        res.hotelName = booking.getHotelName();
        res.customerName = booking.getCustomerName();
        res.idType = booking.getIdType();
        res.idNumber = booking.getIdNumber();
        res.transactionId = booking.getTransactionId();
        res.cardLast4 = booking.getCardLast4();
        res.status = booking.getStatus();
        res.canceledAt = booking.getCanceledAt();
        res.canceledBy = booking.getCanceledBy();
        return res;
    }

    private void validate(RoomBookingRequest req) {
        if (req == null || req.roomId == null)
            throw new IllegalArgumentException("roomId required");
        if (req.checkIn == null || req.checkOut == null)
            throw new IllegalArgumentException("dates required");
        if (!req.checkIn.isBefore(req.checkOut))
            throw new IllegalArgumentException("checkIn must be before checkOut");
        if (req.rooms <= 0)
            throw new IllegalArgumentException("rooms must be > 0");
        if (req.totalPrice != null && req.totalPrice.signum() < 0)
            throw new IllegalArgumentException("totalPrice must be >= 0");
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

    @Transactional
    public RoomBookingResponse adminCancel(UUID bookingId, String canceledBy) {
        HotelRoomBooking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("BOOKING_NOT_FOUND"));
        if ("CANCELED".equalsIgnoreCase(booking.getStatus())) {
            throw new IllegalArgumentException("BOOKING_ALREADY_CANCELED");
        }
        booking.setStatus("CANCELED");
        booking.setCanceledAt(java.time.Instant.now());
        booking.setCanceledBy(canceledBy == null || canceledBy.isBlank() ? "ADMIN" : canceledBy);
        bookingRepository.save(booking);

        RoomBookingResponse res = new RoomBookingResponse();
        res.id = booking.getId();
        res.roomId = booking.getRoomId();
        res.checkIn = booking.getCheckIn();
        res.checkOut = booking.getCheckOut();
        res.rooms = booking.getRoomsBooked() == null ? 0 : booking.getRoomsBooked();
        res.totalGuests = booking.getTotalGuests() == null ? 0 : booking.getTotalGuests();
        res.totalPrice = booking.getTotalPrice();
        res.createdAt = booking.getCreatedAt();
        res.userId = booking.getUserId();
        res.roomName = booking.getRoomName();
        res.hotelName = booking.getHotelName();
        res.customerName = booking.getCustomerName();
        res.idType = booking.getIdType();
        res.idNumber = booking.getIdNumber();
        res.transactionId = booking.getTransactionId();
        res.cardLast4 = booking.getCardLast4();
        res.status = booking.getStatus();
        res.canceledAt = booking.getCanceledAt();
        res.canceledBy = booking.getCanceledBy();
        return res;
    }
}
