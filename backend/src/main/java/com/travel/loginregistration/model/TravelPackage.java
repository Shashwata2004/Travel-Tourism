package com.travel.loginregistration.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "travel_packages")
public class TravelPackage {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    private String name;
    private String location;

    @Column(name = "base_price")
    private BigDecimal basePrice;

    @Column(name = "dest_image_url")
    private String destImageUrl;

    @Column(name = "hotel_image_url")
    private String hotelImageUrl;

    private String overview;

    @Column(name = "location_points")
    private String locationPoints;

    private String timing;
    private boolean active = true;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public BigDecimal getBasePrice() { return basePrice; }
    public void setBasePrice(BigDecimal basePrice) { this.basePrice = basePrice; }
    public String getDestImageUrl() { return destImageUrl; }
    public void setDestImageUrl(String destImageUrl) { this.destImageUrl = destImageUrl; }
    public String getHotelImageUrl() { return hotelImageUrl; }
    public void setHotelImageUrl(String hotelImageUrl) { this.hotelImageUrl = hotelImageUrl; }
    public String getOverview() { return overview; }
    public void setOverview(String overview) { this.overview = overview; }
    public String getLocationPoints() { return locationPoints; }
    public void setLocationPoints(String locationPoints) { this.locationPoints = locationPoints; }
    public String getTiming() { return timing; }
    public void setTiming(String timing) { this.timing = timing; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}

