package com.travel.frontend.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
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
    public LocalDate bookingDeadline;
    public String transactionId;
    public String status;
    public Instant canceledAt;
    public String canceledBy;

    public UUID getId() { return id; }
    public UUID getPackageId() { return packageId; }
    public String getPackageName() { return packageName; }
    public String getUserEmail() { return userEmail; }
    public String getCustomerName() { return customerName; }
    public String getIdType() { return idType; }
    public String getIdNumber() { return idNumber; }
    public int getTotalPersons() { return totalPersons; }
    public BigDecimal getPriceTotal() { return priceTotal; }
    public Instant getCreatedAt() { return createdAt; }
    public LocalDate getBookingDeadline() { return bookingDeadline; }
    public String getTransactionId() { return transactionId; }
    public String getStatus() { return status; }
    public Instant getCanceledAt() { return canceledAt; }
    public String getCanceledBy() { return canceledBy; }

    public void setId(UUID id) { this.id = id; }
    public void setPackageId(UUID packageId) { this.packageId = packageId; }
    public void setPackageName(String packageName) { this.packageName = packageName; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public void setIdType(String idType) { this.idType = idType; }
    public void setIdNumber(String idNumber) { this.idNumber = idNumber; }
    public void setTotalPersons(int totalPersons) { this.totalPersons = totalPersons; }
    public void setPriceTotal(BigDecimal priceTotal) { this.priceTotal = priceTotal; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public void setBookingDeadline(LocalDate bookingDeadline) { this.bookingDeadline = bookingDeadline; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }
    public void setStatus(String status) { this.status = status; }
    public void setCanceledAt(Instant canceledAt) { this.canceledAt = canceledAt; }
    public void setCanceledBy(String canceledBy) { this.canceledBy = canceledBy; }
}
