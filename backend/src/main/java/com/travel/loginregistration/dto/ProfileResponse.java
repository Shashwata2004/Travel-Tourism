package com.travel.loginregistration.dto;

import java.util.UUID;

/*
 * Simple DTO representing a user profile response.
 * This maps the JSON data sent to/from the frontend.
 */

public class ProfileResponse {
    private UUID userId;
    private String email;       // immutable
    private String username;
    private String location;

    private String fullName;
    private String idType;      // NID, BIRTH_CERTIFICATE, PASSPORT
    private String idNumber;
    private String gender;      // MALE, FEMALE

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getIdType() { return idType; }
    public void setIdType(String idType) { this.idType = idType; }
    public String getIdNumber() { return idNumber; }
    public void setIdNumber(String idNumber) { this.idNumber = idNumber; }
    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }
}

