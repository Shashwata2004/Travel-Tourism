package com.travel.frontend.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public class HistoryPackageItem {
    public UUID id;
    public UUID packageId;
    public String packageName;
    public String location;
    public Integer totalPersons;
    public BigDecimal totalPrice;
    public Instant createdAt;
    public LocalDate bookingDeadline;
    public String status;
    public Integer durationDays;
    public String transactionId;
    public String cardLast4;
}
