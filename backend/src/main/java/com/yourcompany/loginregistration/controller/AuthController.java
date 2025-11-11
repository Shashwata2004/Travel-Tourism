package com.yourcompany.loginregistration.controller;

import com.yourcompany.loginregistration.dto.RegisterRequest;
import com.yourcompany.loginregistration.dto.LoginRequest;
import com.yourcompany.loginregistration.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    // ---------- REGISTER ----------
    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody RegisterRequest req) {
        try {
            String response = authService.registerUser(req);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Unexpected error: " + e.getMessage());
        }
    }

    // ---------- LOGIN ----------
    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody LoginRequest req) {
        try {
            String token = authService.loginUser(req.getEmail(), req.getPassword());
            return ResponseEntity.ok(token);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Unexpected error: " + e.getMessage());
        }
    }

    // ---------- (Optional) PING ----------
    @GetMapping("/ping")
    public ResponseEntity<String> ping() {
        return ResponseEntity.ok("AuthController is alive 🚀");
    }
}
