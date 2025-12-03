package com.travel.frontend.model;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public class PackageBookingAdminResponse {
    public UUID packageId;
    public String packageName;
    public LocalDate bookingDeadline;
    public long totalPersons;
    public List<PackageBookingAdminView> items;

    public UUID getPackageId() { return packageId; }
    public String getPackageName() { return packageName; }
    public LocalDate getBookingDeadline() { return bookingDeadline; }
    public long getTotalPersons() { return totalPersons; }
    public List<PackageBookingAdminView> getItems() { return items; }

    public void setPackageId(UUID packageId) { this.packageId = packageId; }
    public void setPackageName(String packageName) { this.packageName = packageName; }
    public void setBookingDeadline(LocalDate bookingDeadline) { this.bookingDeadline = bookingDeadline; }
    public void setTotalPersons(long totalPersons) { this.totalPersons = totalPersons; }
    public void setItems(List<PackageBookingAdminView> items) { this.items = items; }
}
