package com.travel.loginregistration.controller;

import com.travel.loginregistration.dto.BookingRequest;
import com.travel.loginregistration.dto.BookingResponse;
import com.travel.loginregistration.service.BookingService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/*
    Handles booking-related API requests, delegating to BookingService.
    sends credentials to BookingService to process bookings for authenticated users.
*/

@RestController
@RequestMapping("/api/bookings")            
public class BookingController {
    private final BookingService service;

    public BookingController(BookingService service) {
        this.service = service;
    }

    // Accepts POST /api/bookings with JWT-authenticated user, delegates to BookingService.
    @PostMapping
    public ResponseEntity<?> book(Authentication auth, @RequestBody BookingRequest req) {
        try {
            String email = (String) auth.getPrincipal();
            BookingResponse res = service.book(email, req);
            return ResponseEntity.ok(res);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
