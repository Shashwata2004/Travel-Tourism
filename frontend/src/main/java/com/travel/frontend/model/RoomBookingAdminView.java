package com.travel.frontend.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public class RoomBookingAdminView {
    public UUID id;
    public UUID roomId;
    public String roomName;
    public String hotelName;
    public LocalDate checkIn;
    public LocalDate checkOut;
    public Integer roomsBooked;
    public Integer totalGuests;
    public BigDecimal totalPrice;
    public String customerName;
    public String idType;
    public String idNumber;
    public String userEmail;
    public Instant createdAt;
    public String transactionId;

    public UUID getId() { return id; }
    public UUID getRoomId() { return roomId; }
    public String getRoomName() { return roomName; }
    public String getHotelName() { return hotelName; }
    public LocalDate getCheckIn() { return checkIn; }
    public LocalDate getCheckOut() { return checkOut; }
    public Integer getRoomsBooked() { return roomsBooked; }
    public Integer getTotalGuests() { return totalGuests; }
    public BigDecimal getTotalPrice() { return totalPrice; }
    public String getCustomerName() { return customerName; }
    public String getIdType() { return idType; }
    public String getIdNumber() { return idNumber; }
    public String getUserEmail() { return userEmail; }
    public Instant getCreatedAt() { return createdAt; }
    public String getTransactionId() { return transactionId; }

    // setters for Jackson
    public void setId(UUID id) { this.id = id; }
    public void setRoomId(UUID roomId) { this.roomId = roomId; }
    public void setRoomName(String roomName) { this.roomName = roomName; }
    public void setHotelName(String hotelName) { this.hotelName = hotelName; }
    public void setCheckIn(LocalDate checkIn) { this.checkIn = checkIn; }
    public void setCheckOut(LocalDate checkOut) { this.checkOut = checkOut; }
    public void setRoomsBooked(Integer roomsBooked) { this.roomsBooked = roomsBooked; }
    public void setTotalGuests(Integer totalGuests) { this.totalGuests = totalGuests; }
    public void setTotalPrice(BigDecimal totalPrice) { this.totalPrice = totalPrice; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public void setIdType(String idType) { this.idType = idType; }
    public void setIdNumber(String idNumber) { this.idNumber = idNumber; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }
}
