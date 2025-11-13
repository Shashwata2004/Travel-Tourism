package com.travel.loginregistration.config;

import io.github.cdimascio.dotenv.Dotenv;

import java.util.Map;

/**
 * Loads variables from a .env file (if present) before Spring starts
 * and maps them to Spring property keys so the app can run with
 * just a .env file (no system-wide env needed).
 */
public final class EnvLoader {
    private EnvLoader() {}

    public static void load() {
        try {
            // Try default working directory first
            Dotenv base = Dotenv.configure()
                    .ignoreIfMalformed()
                    .ignoreIfMissing()
                    .load();

            // Also try the backend directory (useful when running from repo root)
            Dotenv backendDir = Dotenv.configure()
                    .directory("backend")
                    .ignoreIfMalformed()
                    .ignoreIfMissing()
                    .load();

            // Merge variables (backendDir overrides base if both present)
            Map<String, String> vars = base.entries().stream()
                    .collect(java.util.stream.Collectors.toMap(e -> e.getKey(), e -> e.getValue(), (a, b) -> b));
            backendDir.entries().forEach(e -> vars.put(e.getKey(), e.getValue()));

            // Map .env keys to Spring property keys
            map(vars, "SPRING_DATASOURCE_URL", "spring.datasource.url");
            map(vars, "SPRING_DATASOURCE_USERNAME", "spring.datasource.username");
            map(vars, "SPRING_DATASOURCE_PASSWORD", "spring.datasource.password");

            map(vars, "SPRING_FLYWAY_URL", "spring.flyway.url");
            map(vars, "SPRING_FLYWAY_USER", "spring.flyway.user");
            map(vars, "SPRING_FLYWAY_PASSWORD", "spring.flyway.password");

            map(vars, "APP_JWT_SECRET", "app.jwt.secret");
            map(vars, "APP_JWT_EXPIRATION_MS", "app.jwt.expiration-ms");
        } catch (Exception ignored) {
            // Fail-safe: never block app start because of .env
        }
    }

    private static void map(Map<String, String> vars, String envKey, String springKey) {
        String v = vars.get(envKey);
        if (v != null && !v.isBlank()) {
            System.setProperty(springKey, v);
        }
    }
}

