package com.travel.loginregistration.dto;

import java.math.BigDecimal;
import java.util.UUID;

/*
 * Simple DTO representing a summary of a travel package.
 */

public class PackageSummary {
    public UUID id;
    public String name;
    public String location;
    public BigDecimal basePrice;
    public String destImageUrl;
    public String groupSize;
}

