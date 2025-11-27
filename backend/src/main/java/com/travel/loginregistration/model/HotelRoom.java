package com.travel.loginregistration.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "hotel_rooms")
public class HotelRoom {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "hotel_id")
    private UUID hotelId;

    private String name;
    private BigDecimal price;
    @Column(name = "max_guests")
    private Integer maxGuests;
    @Column(name = "available_rooms")
    private Integer availableRooms;
    private String description;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getHotelId() { return hotelId; }
    public void setHotelId(UUID hotelId) { this.hotelId = hotelId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public Integer getMaxGuests() { return maxGuests; }
    public void setMaxGuests(Integer maxGuests) { this.maxGuests = maxGuests; }
    public Integer getAvailableRooms() { return availableRooms; }
    public void setAvailableRooms(Integer availableRooms) { this.availableRooms = availableRooms; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}

