package com.travel.loginregistration.model;

import jakarta.persistence.*;
import java.util.UUID;

/*
 * java model of app_users table in database.
 * Used for user authentication and profile management.
 * Stores user details like email, username, password hash, and location.
 * ProfileService interacts with this model to get and update user profile data.
 * ProfileController calls ProfileService to handle profile-related API requests from frontend.
 * repsitory layer uses this model to perform CRUD operations on app_users table.
 */

@Entity
@Table(name = "app_users") 
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    private String email;
    private String username;

    @Column(name = "password_hash")
    private String passwordHash;

    private String location;

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }
}
