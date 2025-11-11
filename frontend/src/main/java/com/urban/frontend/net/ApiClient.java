package com.urban.frontend.net;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.urban.frontend.model.Profile;
import com.urban.frontend.session.Session;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public final class ApiClient {

    private static final ApiClient INSTANCE = new ApiClient();
    public static ApiClient get() { return INSTANCE; }

    private final HttpClient http = HttpClient.newHttpClient();
    private static final ObjectMapper mapper = new ObjectMapper();

    // Change base URL if your backend runs elsewhere
    private static final String BASE = "http://localhost:8080/api";

    private ApiClient() {}

    // --- Auth ---
    public String login(String email, String password) throws ApiException {
        String json = """
            {"email":"%s","password":"%s"}
            """.formatted(escape(email), escape(password));

        HttpResponse<String> res = post("/auth/login", json, false);
        if (res.statusCode() == 200) {
            String token = res.body();
            Session.setToken(token);
            return token;
        }
        throw error(res, "Login failed");
    }

    // Placeholder for future admin login (UI can gate its usage)
    public String adminLogin(String email, String password) throws ApiException {
        String json = """
            {"email":"%s","password":"%s"}
            """.formatted(escape(email), escape(password));

        HttpResponse<String> res = post("/auth/login", json, false); // will be replaced with /admin/login later
        if (res.statusCode() == 200) {
            String token = res.body();
            Session.setToken(token);
            return token;
        }
        throw error(res, "Admin login failed");
    }

    public String register(String email, String username, String password, String location) throws ApiException {
        String json = """
            {"email":"%s","username":"%s","password":"%s","location":"%s"}
            """.formatted(escape(email), escape(username), escape(password), escape(location));

        HttpResponse<String> res = post("/auth/register", json, false);
        if (res.statusCode() == 200) {
            return res.body();
        }
        throw error(res, "Registration failed");
    }

    // --- Profile ---
    public Profile getMyProfile() throws ApiException {
        HttpResponse<String> res = get("/profile/me", true);
        if (res.statusCode() == 200) {
            try {
                return mapper.readValue(res.body(), Profile.class);
            } catch (Exception e) {
                throw new ApiException("Invalid profile response", e);
            }
        }
        throw error(res, "Load profile failed");
    }

    public Profile updateMyProfile(Profile p) throws ApiException {
        try {
            String json = mapper.writeValueAsString(p);
            HttpResponse<String> res = put("/profile/me", json, true);
            if (res.statusCode() == 200) {
                return mapper.readValue(res.body(), Profile.class);
            }
            throw error(res, "Update profile failed");
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ApiException("Serialization error: " + e.getMessage(), e);
        }
    }

    // --- Packages & Booking (raw helpers used by controllers) ---
    public HttpResponse<String> rawGet(String path, boolean withAuth) throws ApiException {
        return get(path, withAuth);
    }
    public HttpResponse<String> rawPostJson(String path, String body, boolean withAuth) throws ApiException {
        return post(path, body, withAuth);
    }

    // --- Low-level helpers ---
    private HttpResponse<String> post(String path, String body, boolean withAuth) throws ApiException {
        try {
            HttpRequest.Builder b = HttpRequest.newBuilder()
                    .uri(URI.create(BASE + path))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body));

            if (withAuth && Session.isAuthenticated()) {
                b.header("Authorization", "Bearer " + Session.getToken());
            }

            return http.send(b.build(), HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            throw new ApiException("Network error: " + e.getMessage(), e);
        }
    }

    private HttpResponse<String> get(String path, boolean withAuth) throws ApiException {
        try {
            HttpRequest.Builder b = HttpRequest.newBuilder()
                    .uri(URI.create(BASE + path))
                    .GET();
            if (withAuth && Session.isAuthenticated()) {
                b.header("Authorization", "Bearer " + Session.getToken());
            }
            return http.send(b.build(), HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            throw new ApiException("Network error: " + e.getMessage(), e);
        }
    }

    private HttpResponse<String> put(String path, String body, boolean withAuth) throws ApiException {
        try {
            HttpRequest.Builder b = HttpRequest.newBuilder()
                    .uri(URI.create(BASE + path))
                    .header("Content-Type", "application/json")
                    .PUT(HttpRequest.BodyPublishers.ofString(body));
            if (withAuth && Session.isAuthenticated()) {
                b.header("Authorization", "Bearer " + Session.getToken());
            }
            return http.send(b.build(), HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            throw new ApiException("Network error: " + e.getMessage(), e);
        }
    }

    private static String escape(String s) {
        return s == null ? "" : s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static ApiException error(HttpResponse<String> res, String prefix) {
        String msg = res.body() == null || res.body().isBlank()
                ? (prefix + " (HTTP " + res.statusCode() + ")")
                : (prefix + ": " + res.body());
        return new ApiException(msg);
    }

    // --- Exception wrapper ---
    public static class ApiException extends Exception {
        public ApiException(String msg) { super(msg); }
        public ApiException(String msg, Throwable t) { super(msg, t); }
    }
}
