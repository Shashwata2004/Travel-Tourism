package com.travel.loginregistration.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public class HistoryRoomItem {
    public UUID id;
    public UUID roomId;
    public UUID hotelId;
    public String hotelName;
    public String roomName;
    public LocalDate checkIn;
    public LocalDate checkOut;
    public Integer totalGuests;
    public Integer roomsBooked;
    public BigDecimal totalPrice;
    public Instant createdAt;
    public String status; // placeholder for future
    public String transactionId;
    public String cardLast4;
    public Instant canceledAt;
    public String canceledBy;
}
