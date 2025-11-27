package com.travel.loginregistration.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public class HotelSummary {
    public UUID id;
    public String name;
    public BigDecimal rating;
    public String location;
    public String image;
    public List<String> facilities;
    public BigDecimal realPrice;
    public BigDecimal currentPrice;
    public Integer availableRooms;
}
