package com.travel.loginregistration.service;

import com.travel.loginregistration.dto.RoomBookingRequest;
import com.travel.loginregistration.dto.RoomBookingResponse;
import com.travel.loginregistration.model.HotelRoom;
import com.travel.loginregistration.model.HotelRoomBooking;
import com.travel.loginregistration.repository.HotelRoomBookingRepository;
import com.travel.loginregistration.repository.HotelRoomRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Locale;
import java.util.UUID;

@Service
public class HotelBookingService {

    private final HotelRoomRepository roomRepository;
    private final HotelRoomBookingRepository bookingRepository;

    public HotelBookingService(HotelRoomRepository roomRepository, HotelRoomBookingRepository bookingRepository) {
        this.roomRepository = roomRepository;
        this.bookingRepository = bookingRepository;
    }

    @Transactional
    public RoomBookingResponse book(RoomBookingRequest req) {
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

        HotelRoomBooking b = new HotelRoomBooking();
        b.setRoomId(room.getId());
        b.setCheckIn(req.checkIn);
        b.setCheckOut(req.checkOut);
        b.setRoomsBooked(req.rooms);
        b.setCreatedAt(java.time.Instant.now());
        bookingRepository.save(b);

        RoomBookingResponse res = new RoomBookingResponse();
        res.id = b.getId();
        res.roomId = b.getRoomId();
        res.checkIn = b.getCheckIn();
        res.checkOut = b.getCheckOut();
        res.rooms = b.getRoomsBooked();
        res.createdAt = b.getCreatedAt();
        return res;
    }

    private void validate(RoomBookingRequest req) {
        if (req == null || req.roomId == null) throw new IllegalArgumentException("roomId required");
        if (req.checkIn == null || req.checkOut == null) throw new IllegalArgumentException("dates required");
        if (!req.checkIn.isBefore(req.checkOut)) throw new IllegalArgumentException("checkIn must be before checkOut");
        if (req.rooms <= 0) throw new IllegalArgumentException("rooms must be > 0");
    }
}
