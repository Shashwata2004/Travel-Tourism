package com.travel.loginregistration.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/*
   java model of booking table in database.
    Used to store booking details made by users for travel packages.
    it is saved in database by BookingService when a user makes a booking.
    stores booking's user id, package id, total persons, total price, customer name, id number, and creation timestamp.
    repository layer uses this model to perform CRUD operations on bookings table.
*/

@Entity
@Table(name = "bookings")
public class Booking {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "package_id")
    private UUID packageId;

    @Column(name = "total_persons")
    private int totalPersons;

    @Column(name = "price_total")
    private BigDecimal priceTotal;

    @Column(name = "customer_name")
    private String customerName;

    @Column(name = "id_number")
    private String idNumber;

    @Column(name = "created_at")
    private Instant createdAt = Instant.now();

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public UUID getPackageId() { return packageId; }
    public void setPackageId(UUID packageId) { this.packageId = packageId; }
    public int getTotalPersons() { return totalPersons; }
    public void setTotalPersons(int totalPersons) { this.totalPersons = totalPersons; }
    public BigDecimal getPriceTotal() { return priceTotal; }
    public void setPriceTotal(BigDecimal priceTotal) { this.priceTotal = priceTotal; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public String getIdNumber() { return idNumber; }
    public void setIdNumber(String idNumber) { this.idNumber = idNumber; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}

