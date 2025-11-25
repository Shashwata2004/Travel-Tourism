package com.travel.frontend.admin;

/**
 * Simple holder for the authenticated admin session token that is shared
 * between the login screen, admin dashboard, and admin socket client.
 */
public final class AdminSession {

    private static String token;

    private AdminSession() {
        // Utility class
    }

    public static void setToken(String value) {
        token = value;
    }

    public static String getToken() {
        return token;
    }

    public static void clear() {
        token = null;
    }

    public static boolean isAuthenticated() {
        return token != null && !token.isBlank();
    }
}
