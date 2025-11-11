package com.urban.frontend.session;

public final class Session {
    // Keep fields volatile for FX thread-safety
    private static volatile String token;
    private static volatile String email;

    private Session() { }

    /* ===== New API (optional) ===== */
    public static void set(String jwt, String userEmail) {
        token = jwt;
        email = userEmail;
    }

    public static String getEmail() {
        return email;
    }

    /* ===== Old API (kept for compatibility) ===== */
    public static void setToken(String jwt) {             // old name
        token = jwt;
    }

    public static String getToken() {                     // old name
        return token;
    }

    public static boolean isAuthenticated() {             // unchanged
        return token != null && !token.isBlank();
    }

    public static void clear() {                          // unchanged
        token = null;
        email = null;
    }
}
