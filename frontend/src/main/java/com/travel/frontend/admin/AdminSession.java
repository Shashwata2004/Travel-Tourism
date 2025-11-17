/* Holds on to the admin socket token so the dashboard and network helper know
   whether the privileged channel is still signed in. Mirrors the user Session
   class but keeps things separate so admin-only sockets never leak into the
   regular API calls. */
package com.travel.frontend.admin;

public final class AdminSession {
    private static volatile String token;

    private AdminSession() {}

    /* Saves the latest token received from the AdminSocketClient AUTH call so
       future requests can reuse it without prompting the admin again. */
    public static void setToken(String t) { token = t; }
    /* Allows controllers to quickly confirm whether an admin is still logged
       in before showing staff-only screens. */
    public static String getToken() { return token; }
    /* Wipes the stored token, usually after logout or a socket failure, so the
       dashboard doesn’t accidentally keep elevated access. */
    public static void clear() { token = null; }
}
