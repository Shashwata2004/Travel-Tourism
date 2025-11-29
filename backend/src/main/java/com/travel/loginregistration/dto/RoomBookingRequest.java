package com.travel.loginregistration.dto;

import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.UUID;

public class RoomBookingRequest {
    public UUID roomId;
    public LocalDate checkIn;
    public LocalDate checkOut;
    public int rooms;
    public int totalGuests;
    public BigDecimal totalPrice;
    public UUID userId;
    public String hotelName;
    public String roomName;
    public String customerName;
    public String idType;
    public String idNumber;
}
