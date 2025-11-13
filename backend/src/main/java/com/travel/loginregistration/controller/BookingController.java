package com.travel.loginregistration.controller;

import com.travel.loginregistration.dto.BookingRequest;
import com.travel.loginregistration.dto.BookingResponse;
import com.travel.loginregistration.service.BookingService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {
    private final BookingService service;

    public BookingController(BookingService service) {
        this.service = service;
    }

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

