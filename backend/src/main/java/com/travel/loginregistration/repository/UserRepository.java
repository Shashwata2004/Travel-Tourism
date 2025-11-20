package com.travel.loginregistration.repository;

import com.travel.loginregistration.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

// Repository for accessing the users table using the User entity
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
}
