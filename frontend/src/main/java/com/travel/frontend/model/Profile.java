/* Simple container for the personal details we show and edit on the profile
   screen, matching the fields the backend sends back in JSON. Uses Jackson’s
   annotations so extra backend fields are ignored, keeping the UI resilient
   as the API evolves. */
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
