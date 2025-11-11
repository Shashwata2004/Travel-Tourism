package com.yourcompany.loginregistration.controller;

import com.yourcompany.loginregistration.dto.ProfileResponse;
import com.yourcompany.loginregistration.dto.ProfileUpdateRequest;
import com.yourcompany.loginregistration.service.ProfileService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping("/me")
    public ResponseEntity<ProfileResponse> me(Authentication auth) {
        String email = (String) auth.getPrincipal();
        return ResponseEntity.ok(profileService.getProfile(email));
    }

    @PutMapping("/me")
    public ResponseEntity<?> update(Authentication auth, @RequestBody ProfileUpdateRequest req) {
        try {
            String email = (String) auth.getPrincipal();
            ProfileResponse updated = profileService.updateProfile(email, req);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}

