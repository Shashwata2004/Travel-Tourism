package com.travel.loginregistration.repository;

import com.travel.loginregistration.model.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

// Repository for accessing the user_profiles table using the UserProfile entity
public interface UserProfileRepository extends JpaRepository<UserProfile, UUID> {
    Optional<UserProfile> findByUserId(UUID userId);
}

