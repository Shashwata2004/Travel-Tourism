package com.travel.loginregistration.service;

import com.travel.loginregistration.dto.ProfileResponse;
import com.travel.loginregistration.dto.ProfileUpdateRequest;
import com.travel.loginregistration.model.User;
import com.travel.loginregistration.model.UserProfile;
import com.travel.loginregistration.repository.UserProfileRepository;
import com.travel.loginregistration.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

/*
     manages user profile data retrieval and updates
     profilecontroller calls this service with user email from jwt and profile update data
     this actually does the work of getting and updating profile info in the database , the profilecontroller just delegates to it
 */

@Service
public class ProfileService {
    private final UserRepository userRepository;
    private final UserProfileRepository profileRepository;

    public ProfileService(UserRepository userRepository, UserProfileRepository profileRepository) {
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
    }

    @Transactional(readOnly = true)
    public ProfileResponse getProfile(String email) {
        User user = userRepository.findByEmail(email.toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        UserProfile profile = profileRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    UserProfile p = new UserProfile();
                    p.setUserId(user.getId());
                    return p;
                });

        ProfileResponse res = new ProfileResponse();
        res.setUserId(user.getId());
        res.setEmail(user.getEmail());
        res.setUsername(user.getUsername());
        res.setLocation(user.getLocation());
        res.setFullName(profile.getFullName());
        res.setIdType(profile.getIdType());
        res.setIdNumber(profile.getIdNumber());
        res.setGender(profile.getGender());
        return res;
    }

    @Transactional
    public ProfileResponse updateProfile(String email, ProfileUpdateRequest req) {
        User user = userRepository.findByEmail(email.toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (req.getUsername() != null && !req.getUsername().isBlank()) {
            user.setUsername(req.getUsername().trim());
        }
        if (req.getLocation() != null) {
            user.setLocation(req.getLocation().trim());
        }
        userRepository.save(user);

        UserProfile profile = profileRepository.findByUserId(user.getId()).orElseGet(() -> {
            UserProfile p = new UserProfile();
            p.setUserId(user.getId());
            return p;
        });

        if (req.getFullName() != null) profile.setFullName(req.getFullName().trim());
        if (req.getIdType() != null) profile.setIdType(validateIdType(req.getIdType()));
        if (req.getIdNumber() != null) profile.setIdNumber(req.getIdNumber().trim());
        if (req.getGender() != null) profile.setGender(validateGender(req.getGender()));

        profileRepository.save(profile);

        return getProfile(email);
    }

    private String validateIdType(String idType) {
        String v = idType.trim().toUpperCase(Locale.ROOT);
        switch (v) {
            case "NID":
            case "BIRTH_CERTIFICATE":
            case "PASSPORT":
                return v;
            default:
                throw new IllegalArgumentException("Invalid idType");
        }
    }

    private String validateGender(String gender) {
        String v = gender.trim().toUpperCase(Locale.ROOT);
        switch (v) {
            case "MALE":
            case "FEMALE":
                return v;
            default:
                throw new IllegalArgumentException("Invalid gender");
        }
    }
}

