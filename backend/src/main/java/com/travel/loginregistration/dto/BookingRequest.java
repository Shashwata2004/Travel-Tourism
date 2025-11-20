package com.travel.loginregistration.dto;

import java.util.UUID;

/* DTOs are data transfer object
 * it maps the json data received from frontend.
    it is the java object form of json request.
 */

/*
 * Simple DTO representing a booking request from the frontend.
 */

public class BookingRequest {
    public UUID packageId;
    public int totalPersons;
}

