package com.travel.frontend.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Profile {
    public String userId;
    public String email;
    public String username;
    public String location;

    public String fullName;
    public String idType;    // NID | BIRTH_CERTIFICATE | PASSPORT
    public String idNumber;
    public String gender;    // MALE | FEMALE
}

