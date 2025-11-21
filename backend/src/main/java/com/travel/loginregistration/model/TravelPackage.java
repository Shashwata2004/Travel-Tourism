package com.travel.loginregistration.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;

/*
 * java model of travel_packages table in database.
 * Used to store travel package details like name, location, price, images, overview, location points, timing, and active status.
 * services interact with this model to retrieve and manage travel package data.
 * then controllers call those services to handle API requests from frontend.
 * repository layer uses this model to perform CRUD operations on travel_packages table.
 */

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

    @Column(name = "image1")
    private String image1;

    @Column(name = "image2")
    private String image2;

    @Column(name = "image3")
    private String image3;

    @Column(name = "image4")
    private String image4;

    @Column(name = "image5")
    private String image5;

    private String overview;

    @Column(name = "location_points")
    private String locationPoints;

    private String timing;
    @Column(name = "group_size")
    private String groupSize;
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
    public String getImage1() { return image1; }
    public void setImage1(String image1) { this.image1 = image1; }
    public String getImage2() { return image2; }
    public void setImage2(String image2) { this.image2 = image2; }
    public String getImage3() { return image3; }
    public void setImage3(String image3) { this.image3 = image3; }
    public String getImage4() { return image4; }
    public void setImage4(String image4) { this.image4 = image4; }
    public String getImage5() { return image5; }
    public void setImage5(String image5) { this.image5 = image5; }
    public String getOverview() { return overview; }
    public void setOverview(String overview) { this.overview = overview; }
    public String getLocationPoints() { return locationPoints; }
    public void setLocationPoints(String locationPoints) { this.locationPoints = locationPoints; }
    public String getTiming() { return timing; }
    public void setTiming(String timing) { this.timing = timing; }
    public String getGroupSize() { return groupSize; }
    public void setGroupSize(String groupSize) { this.groupSize = groupSize; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}

