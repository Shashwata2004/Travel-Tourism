package com.travel.loginregistration.model;

import jakarta.persistence.*;
import java.util.UUID;

/*
 * java model of user_profiles table in database.
 * Used to store additional profile details for users.
 * It links to the User model via userId.
 * ProfileService uses this model to get and update user profile information.
 * ProfileController calls ProfileService to handle profile-related API requests from frontend.
 * repsitory layer uses this model to perform CRUD operations on user_profiles table.
 */

@Entity
@Table(name = "user_profiles")
public class UserProfile {
    @Id
    @Column(name = "user_id")
    private UUID userId;

    @OneToOne
    @JoinColumn(name = "user_id", referencedColumnName = "id", insertable = false, updatable = false)
    private User user;

    @Column(name = "full_name")
    private String fullName;

    @Column(name = "id_type")
    private String idType; // NID | BIRTH_CERTIFICATE | PASSPORT

    @Column(name = "id_number")
    private String idNumber;

    private String gender; // MALE | FEMALE

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getIdType() { return idType; }
    public void setIdType(String idType) { this.idType = idType; }
    public String getIdNumber() { return idNumber; }
    public void setIdNumber(String idNumber) { this.idNumber = idNumber; }
    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }
}

