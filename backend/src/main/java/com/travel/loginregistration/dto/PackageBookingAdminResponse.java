package com.travel.loginregistration.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public class PackageBookingAdminResponse {
    public UUID packageId;
    public String packageName;
    public LocalDate bookingDeadline;
    public long totalPersons;
    public List<PackageBookingAdminView> items;
}
