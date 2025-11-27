package com.travel.loginregistration.model;

import jakarta.persistence.*;
import java.util.UUID;
import java.math.BigDecimal;

@Entity
@Table(name = "hotels")
public class Hotel {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "destination_id")
    private UUID destinationId;

    private String name;
    private BigDecimal rating; // up to 5.0
    private String location;
    private String nearby;       // sentences separated by periods
    private String facilities;   // comma-separated list
    private String description;
    @Column(name = "real_price")
    private BigDecimal realPrice;
    @Column(name = "current_price")
    private BigDecimal currentPrice;

    @Column(name = "rooms_count")
    private Integer roomsCount;
    @Column(name = "floors_count")
    private Integer floorsCount;

    private String image1;
    private String image2;
    private String image3;
    private String image4;
    private String image5;
    private String gallery; // extra image URLs comma-separated

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getDestinationId() { return destinationId; }
    public void setDestinationId(UUID destinationId) { this.destinationId = destinationId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public BigDecimal getRating() { return rating; }
    public void setRating(BigDecimal rating) { this.rating = rating; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public String getNearby() { return nearby; }
    public void setNearby(String nearby) { this.nearby = nearby; }
    public String getFacilities() { return facilities; }
    public void setFacilities(String facilities) { this.facilities = facilities; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public BigDecimal getRealPrice() { return realPrice; }
    public void setRealPrice(BigDecimal realPrice) { this.realPrice = realPrice; }
    public BigDecimal getCurrentPrice() { return currentPrice; }
    public void setCurrentPrice(BigDecimal currentPrice) { this.currentPrice = currentPrice; }
    public Integer getRoomsCount() { return roomsCount; }
    public void setRoomsCount(Integer roomsCount) { this.roomsCount = roomsCount; }
    public Integer getFloorsCount() { return floorsCount; }
    public void setFloorsCount(Integer floorsCount) { this.floorsCount = floorsCount; }
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
    public String getGallery() { return gallery; }
    public void setGallery(String gallery) { this.gallery = gallery; }
}
