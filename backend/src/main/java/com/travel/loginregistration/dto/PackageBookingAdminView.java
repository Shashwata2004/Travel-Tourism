package com.travel.loginregistration.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class PackageBookingAdminView {
    public UUID id;
    public UUID packageId;
    public String packageName;
    public String userEmail;
    public String customerName;
    public String idType;
    public String idNumber;
    public int totalPersons;
    public BigDecimal priceTotal;
    public Instant createdAt;
    public String transactionId;
    public String status;
    public Instant canceledAt;
    public String canceledBy;
}
