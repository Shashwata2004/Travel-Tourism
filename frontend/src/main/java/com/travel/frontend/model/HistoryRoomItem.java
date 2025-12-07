package com.travel.frontend.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public class HistoryRoomItem {
    public UUID id;
    public String hotelName;
    public String roomName;
    public java.util.UUID roomId;
    public java.util.UUID hotelId;
    public LocalDate checkIn;
    public LocalDate checkOut;
    public Integer totalGuests;
    public Integer roomsBooked;
    public BigDecimal totalPrice;
    public Instant createdAt;
    public String status;
    public String transactionId;
    public String cardLast4;
    public Instant canceledAt;
}
