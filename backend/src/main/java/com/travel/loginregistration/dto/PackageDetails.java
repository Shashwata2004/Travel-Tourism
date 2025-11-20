package com.travel.loginregistration.dto;

import java.math.BigDecimal;
import java.util.UUID;

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
    public String overview;
    public String locationPoints;
    public String timing;
}

