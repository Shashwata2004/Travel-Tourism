package com.travel.loginregistration.dto;

import java.time.LocalDate;
import java.util.UUID;

public class RoomBookingRequest {
    public UUID roomId;
    public LocalDate checkIn;
    public LocalDate checkOut;
    public int rooms;
}
