package com.travel.loginregistration.controller;

import com.travel.loginregistration.dto.ProfileResponse;
import com.travel.loginregistration.dto.ProfileUpdateRequest;
import com.travel.loginregistration.service.ProfileService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/*
    Handles user profile-related API requests, delegating to ProfileService to get and update profile data for authenticated users.
*/

@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping("/me")      // shows user profile
    public ResponseEntity<ProfileResponse> me(Authentication auth) {
        String email = (String) auth.getPrincipal();        //find current user from jwt token
        return ResponseEntity.ok(profileService.getProfile(email));
    }

    @PutMapping("/me")          // user can update profile
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

