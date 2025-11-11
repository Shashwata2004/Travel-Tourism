package com.yourcompany.loginregistration.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class BookingResponse {
    public UUID id;
    public UUID packageId;
    public int totalPersons;
    public BigDecimal priceTotal;
    public Instant createdAt;
}

