package com.travel.loginregistration.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public class HotelDetails {
    public UUID id;
    public String name;
    public BigDecimal rating;
    public String location;
    public List<String> images;
    public List<String> nearby;
    public List<String> facilities;
    public String description;
    public Integer roomsCount;
    public Integer floorsCount;
    public List<RoomInfo> rooms;

    public static class RoomInfo {
        public UUID id;
        public String name;
        public String bedType;
        public Integer maxGuests;
        public Integer totalRooms;
        public Integer availableRooms;
        public String facilities;
        public BigDecimal realPrice;
        public BigDecimal currentPrice;
        public String image1;
        public String image2;
        public String image3;
        public String image4;
        public String description;
        public Integer remainingRooms;
    }
}
