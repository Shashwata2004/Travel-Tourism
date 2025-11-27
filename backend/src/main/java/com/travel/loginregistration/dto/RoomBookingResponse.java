package com.travel.loginregistration.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public class RoomBookingResponse {
    public UUID id;
    public UUID roomId;
    public LocalDate checkIn;
    public LocalDate checkOut;
    public int rooms;
    public Instant createdAt;
}
