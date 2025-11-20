package com.travel.loginregistration.model;

import jakarta.persistence.*;
import java.util.UUID;

/* 
 * java versiion of admin user table in database.
 * Used for admin authentication and authorization.
 * Stores admin user details like email, username, password hash, and role.
 * repository layer uses this model to perform CRUD operations on admin_users table.
 */


@Entity
@Table(name = "admin_users")
public class AdminUser {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    private String email;
    private String username;

    @Column(name = "password_hash")
    private String passwordHash;

    private String role = "ADMIN";

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}

