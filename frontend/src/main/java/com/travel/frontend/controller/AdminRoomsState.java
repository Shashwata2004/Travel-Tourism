package com.travel.frontend.controller;

import java.util.UUID;

public final class AdminRoomsState {
    private static UUID hotelId;
    private static String hotelName;

    private AdminRoomsState() {}

    public static void set(UUID id, String name) {
        hotelId = id;
        hotelName = name;
    }

    public static UUID getHotelId() { return hotelId; }
    public static String getHotelName() { return hotelName; }

    public static void clear() { hotelId = null; hotelName = null; }
}
