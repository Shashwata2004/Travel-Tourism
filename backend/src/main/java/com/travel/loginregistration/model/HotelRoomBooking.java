package com.travel.loginregistration.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "hotel_room_bookings")
public class HotelRoomBooking {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "room_id")
    private UUID roomId;

    @Column(name = "check_in")
    private LocalDate checkIn;

    @Column(name = "check_out")
    private LocalDate checkOut;

    @Column(name = "rooms_booked")
    private Integer roomsBooked;

    @Column(name = "created_at")
    private java.time.Instant createdAt;

    @Column(name = "total_guests")
    private Integer totalGuests;

    @Column(name = "total_price")
    private BigDecimal totalPrice;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "hotel_name")
    private String hotelName;

    @Column(name = "room_name")
    private String roomName;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getRoomId() { return roomId; }
    public void setRoomId(UUID roomId) { this.roomId = roomId; }
    public LocalDate getCheckIn() { return checkIn; }
    public void setCheckIn(LocalDate checkIn) { this.checkIn = checkIn; }
    public LocalDate getCheckOut() { return checkOut; }
    public void setCheckOut(LocalDate checkOut) { this.checkOut = checkOut; }
    public Integer getRoomsBooked() { return roomsBooked; }
    public void setRoomsBooked(Integer roomsBooked) { this.roomsBooked = roomsBooked; }
    public java.time.Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(java.time.Instant createdAt) { this.createdAt = createdAt; }
    public Integer getTotalGuests() { return totalGuests; }
    public void setTotalGuests(Integer totalGuests) { this.totalGuests = totalGuests; }
    public BigDecimal getTotalPrice() { return totalPrice; }
    public void setTotalPrice(BigDecimal totalPrice) { this.totalPrice = totalPrice; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public String getHotelName() { return hotelName; }
    public void setHotelName(String hotelName) { this.hotelName = hotelName; }
    public String getRoomName() { return roomName; }
    public void setRoomName(String roomName) { this.roomName = roomName; }
}
