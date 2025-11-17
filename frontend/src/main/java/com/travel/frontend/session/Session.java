/* Stores the signed-in person’s token and email in one place so any screen
   can check whether someone is logged in or clear their info when logging out.
   Keeps the fields volatile so background threads and the JavaFX thread stay
   in sync without extra locking. */
package com.travel.frontend.session;

public final class Session {
    // Keep fields volatile for FX thread-safety
    private static volatile String token;
    private static volatile String email;

    private Session() { }

    /* ===== New API (optional) ===== */
    /* Updates both token and email at once, which is handy right after the
       login HTTP call succeeds and we already have both pieces of data. */
    public static void set(String jwt, String userEmail) {
        token = jwt;
        email = userEmail;
    }

    /* Gives controllers a quick way to show the signed-in address without
       fetching profile data again. */
    public static String getEmail() {
        return email;
    }

    /* ===== Old API (kept for compatibility) ===== */
    /* Older entry point that only set the token; kept to avoid touching every
       caller at once while we gradually move over to set(String, String). */
    public static void setToken(String jwt) {             // old name
        token = jwt;
    }

    /* Older getter that a few helper classes still use to place the Bearer
       token on HTTP requests. */
    public static String getToken() {                     // old name
        return token;
    }

    /* Lightweight check used throughout the UI to block access when no token
       is present; relies on a simple null/blank test rather than heavy logic. */
    public static boolean isAuthenticated() {             // unchanged
        return token != null && !token.isBlank();
    }

    /* Clears everything, typically during logout or when a token expires, so
       no sensitive strings remain in memory longer than needed. */
    public static void clear() {                          // unchanged
        token = null;
        email = null;
    }
}
