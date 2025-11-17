package com.travel.loginregistration.dto;

public class ProfileUpdateRequest {
    // Base (allowed to update except email)
    private String username;
    private String location;

    // Profile
    private String fullName;
    private String idType;   // NID | BIRTH_CERTIFICATE | PASSPORT
    private String idNumber;
    private String gender;   // MALE | FEMALE

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

