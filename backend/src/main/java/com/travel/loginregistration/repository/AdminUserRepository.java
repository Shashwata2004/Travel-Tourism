package com.travel.loginregistration.repository;

import com.travel.loginregistration.model.AdminUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/*
    repositries are used to interact with the database.
    service calls repository to perform database operations.
*/

// connecting AdminUser table of database with AdminUserRepository
public interface AdminUserRepository extends JpaRepository<AdminUser, UUID> {
    Optional<AdminUser> findByEmail(String email);
}

