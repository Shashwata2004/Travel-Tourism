package com.travel.frontend.admin;

/** Holds admin session token for the socket-based admin channel. */
public final class AdminSession {
    private static volatile String token;

    private AdminSession() {}

    public static void setToken(String t) { token = t; }
    public static String getToken() { return token; }
    public static void clear() { token = null; }
}

