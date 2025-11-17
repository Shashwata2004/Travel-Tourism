/* Keeps a short-lived in-memory copy of data we recently loaded so the app
   doesn’t ask the server for the same information over and over again.
   Uses a thread-friendly map plus a simple time-to-live so background tasks
   and JavaFX screens can share responses without clashing. */
package com.travel.frontend.cache;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class DataCache {
    private static final Map<String, Entry> CACHE = new ConcurrentHashMap<>();
    private static final long TTL_MS = Long.getLong("cache.ttl.ms", 60_000L);

    private DataCache() {}

    public interface SupplierX<T> { T get() throws Exception; }

    /* Either returns a warm value or falls back to the supplied loader, letting
       controllers hide the details of HTTP requests and reuse data safely. */
    public static <T> T getOrLoad(String key, SupplierX<T> loader) throws Exception {
        long now = System.currentTimeMillis();
        Entry e = CACHE.get(key);
        if (e != null && (now - e.ts) < TTL_MS) {
            @SuppressWarnings("unchecked")
            T v = (T) e.val;
            return v;
        }
        T v = loader.get();
        CACHE.put(key, new Entry(v, now));
        return v;
    }

    /* Allows controllers to push freshly saved data into the cache so other
       views see the update without another trip to the server. */
    public static void put(String key, Object value) {
        CACHE.put(key, new Entry(value, System.currentTimeMillis()));
    }

    /* Clears everything, usually during logout, to avoid showing stale personal
       information after someone leaves the app. */
    public static void clear() { CACHE.clear(); }

    private record Entry(Object val, long ts) {}
}
