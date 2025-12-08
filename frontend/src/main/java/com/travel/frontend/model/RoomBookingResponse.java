package com.travel.frontend.model;

import java.math.BigDecimal;
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
    public int totalGuests;
    public BigDecimal totalPrice;
    public UUID userId;
    public String hotelName;
    public String roomName;
    public String customerName;
    public String idType;
    public String idNumber;
    public String transactionId;
    public String cardLast4;
    public String status;
    public Instant canceledAt;
    public String canceledBy;
}
