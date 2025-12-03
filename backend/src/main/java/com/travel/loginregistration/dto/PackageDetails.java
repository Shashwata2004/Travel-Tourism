package com.travel.loginregistration.dto;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.List;
import java.time.LocalDate;

/*
 * Simple DTO representing travel package details.
 * This maps the JSON data sent to/from the frontend.
 */

public class PackageDetails {
    public UUID id;
    public String name;
    public String location;
    public BigDecimal basePrice;
    public String destImageUrl;
    public String hotelImageUrl;
    public String image1;
    public String image2;
    public String image3;
    public String image4;
    public String image5;
    public String overview;
    public String locationPoints;
    public String timing;
    public List<ItineraryItem> itinerary;
    public String groupSize;
    public LocalDate bookingDeadline;
}
