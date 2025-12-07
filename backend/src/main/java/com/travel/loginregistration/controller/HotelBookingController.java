package com.travel.loginregistration.controller;

import com.travel.loginregistration.dto.RoomBookingRequest;
import com.travel.loginregistration.dto.RoomBookingResponse;
import com.travel.loginregistration.service.HotelBookingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/hotels")
public class HotelBookingController {

    private final HotelBookingService service;

    public HotelBookingController(HotelBookingService service) {
        this.service = service;
    }

    @PostMapping("/{hotelId}/rooms/{roomId}/book")
    public ResponseEntity<?> book(@PathVariable UUID hotelId,
            @PathVariable UUID roomId,
            @RequestBody RoomBookingRequest req,
            org.springframework.security.core.Authentication auth) {
        try {
            req.roomId = roomId;
            // Optional hotelId could be logged later; we keep signature untouched.
            String email = auth != null ? (String) auth.getPrincipal() : null;
            RoomBookingResponse res = service.book(req, email);
            return ResponseEntity.ok(res);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/bookings/{bookingId}/cancel")
    public ResponseEntity<?> cancel(@PathVariable UUID bookingId, org.springframework.security.core.Authentication auth) {
        try {
            String email = auth != null ? (String) auth.getPrincipal() : null;
            RoomBookingResponse res = service.cancel(bookingId, email);
            return ResponseEntity.ok(res);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
