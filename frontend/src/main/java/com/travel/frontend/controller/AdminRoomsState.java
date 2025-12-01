package com.travel.frontend.controller;

import java.util.UUID;

public final class AdminRoomsState {
    private static UUID hotelId;
    private static String hotelName;
    private static UUID roomId;
    private static String roomName;

    private AdminRoomsState() {}

    public static void set(UUID id, String name) {
        hotelId = id;
        hotelName = name;
    }

    public static UUID getHotelId() { return hotelId; }
    public static String getHotelName() { return hotelName; }

    public static void setRoom(UUID id, String name) {
        roomId = id;
        roomName = name;
    }

    public static UUID getRoomId() { return roomId; }
    public static String getRoomName() { return roomName; }

    public static void clearRoom() { roomId = null; roomName = null; }

    public static void clear() {
        hotelId = null;
        hotelName = null;
        roomId = null;
        roomName = null;
    }
}
