package com.travel.frontend.cache;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.function.Supplier;

/**
 * Simple file-backed cache for admin data to reduce socket calls on slow networks.
 * Stores JSON per key under ~/.travel-admin-cache
 */
public final class FileCache {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final File ROOT = new File(System.getProperty("user.home"), ".travel-admin-cache");

    private FileCache() {}

    public static <T> T getOrLoad(String key, TypeReference<T> type, Supplier<T> loader) throws Exception {
        ensureRoot();
        File f = fileFor(key);
        T cached = null;
        if (f.exists()) {
            try {
                cached = MAPPER.readValue(f, type);
                return cached;
            } catch (IOException ignore) {
                // fall through
            }
        }
        try {
            T value = loader.get();
            try {
                MAPPER.writeValue(f, value);
            } catch (IOException ignore) {}
            return value;
        } catch (RuntimeException ex) {
            if (cached != null) return cached;
            throw ex;
        }
    }

    public static void put(String key, Object value) {
        ensureRoot();
        try {
            MAPPER.writeValue(fileFor(key), value);
        } catch (IOException ignore) {}
    }

    public static void remove(String key) {
        File f = fileFor(key);
        if (f.exists()) {
            try { Files.deleteIfExists(f.toPath()); } catch (IOException ignore) {}
        }
    }

    private static void ensureRoot() {
        if (!ROOT.exists()) ROOT.mkdirs();
    }

    private static File fileFor(String key) {
        String safe = key.replaceAll("[^a-zA-Z0-9._-]", "_");
        return new File(ROOT, safe + ".json");
    }
}
