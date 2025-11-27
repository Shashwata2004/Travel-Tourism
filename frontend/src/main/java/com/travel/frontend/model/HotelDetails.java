package com.travel.frontend.model;

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
}
