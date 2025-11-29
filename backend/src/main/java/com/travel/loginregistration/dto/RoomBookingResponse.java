package com.travel.loginregistration.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import java.math.BigDecimal;

public class RoomBookingResponse {
    public UUID id;
    public UUID roomId;
    public LocalDate checkIn;
    public LocalDate checkOut;
    public int rooms;
    public Instant createdAt;
    public int totalGuests;
    public BigDecimal totalPrice;
    public UUID userId;
    public String hotelName;
    public String roomName;
}
