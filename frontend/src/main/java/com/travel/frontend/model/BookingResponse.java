package com.travel.frontend.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class BookingResponse {
    public UUID id;
    public UUID packageId;
    public int totalPersons;
    public BigDecimal priceTotal;
    public Instant createdAt;
    public String transactionId;
    public String cardLast4;
    public String status;
    public Instant canceledAt;
}
