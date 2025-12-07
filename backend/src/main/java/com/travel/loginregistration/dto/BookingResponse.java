package com.travel.loginregistration.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/*
 * Simple DTO representing a booking response sent to the frontend.
    it is the java object form of json response.
 */

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
    public String canceledBy;
}
