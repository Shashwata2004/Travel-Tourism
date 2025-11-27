package com.travel.frontend.controller;

import java.util.UUID;

// Simple holder for selected destination when opening the hotel manager
public final class AdminHotelsState {
    private static UUID destinationId;
    private static String destinationName;

    private AdminHotelsState() {}

    public static void set(UUID id, String name) {
        destinationId = id;
        destinationName = name;
    }

    public static UUID getDestinationId() { return destinationId; }
    public static String getDestinationName() { return destinationName; }

    public static void clear() {
        destinationId = null;
        destinationName = null;
    }
}
