package com.travel.loginregistration.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public class RoomBookingAdminView {
    public UUID id;
    public UUID roomId;
    public String roomName;
    public String hotelName;
    public String userEmail;
    public LocalDate checkIn;
    public LocalDate checkOut;
    public Integer roomsBooked;
    public Integer totalGuests;
    public BigDecimal totalPrice;
    public String customerName;
    public String idType;
    public String idNumber;
    public Instant createdAt;
}
