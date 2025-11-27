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
    @Column(name = "total_rooms")
    private Integer totalRooms;
    @Column(name = "bed_type")
    private String bedType;
    private String facilities;
    @Column(name = "real_price")
    private BigDecimal realPrice;
    @Column(name = "current_price")
    private BigDecimal currentPrice;
    private String image1;
    private String image2;
    private String image3;
    private String image4;
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
    public Integer getTotalRooms() { return totalRooms; }
    public void setTotalRooms(Integer totalRooms) { this.totalRooms = totalRooms; }
    public String getBedType() { return bedType; }
    public void setBedType(String bedType) { this.bedType = bedType; }
    public String getFacilities() { return facilities; }
    public void setFacilities(String facilities) { this.facilities = facilities; }
    public BigDecimal getRealPrice() { return realPrice; }
    public void setRealPrice(BigDecimal realPrice) { this.realPrice = realPrice; }
    public BigDecimal getCurrentPrice() { return currentPrice; }
    public void setCurrentPrice(BigDecimal currentPrice) { this.currentPrice = currentPrice; }
    public String getImage1() { return image1; }
    public void setImage1(String image1) { this.image1 = image1; }
    public String getImage2() { return image2; }
    public void setImage2(String image2) { this.image2 = image2; }
    public String getImage3() { return image3; }
    public void setImage3(String image3) { this.image3 = image3; }
    public String getImage4() { return image4; }
    public void setImage4(String image4) { this.image4 = image4; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
