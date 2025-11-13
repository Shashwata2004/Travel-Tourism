package com.travel.frontend.cache;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class DataCache {
    private static final Map<String, Entry> CACHE = new ConcurrentHashMap<>();
    private static final long TTL_MS = Long.getLong("cache.ttl.ms", 60_000L);

    private DataCache() {}

    public interface SupplierX<T> { T get() throws Exception; }

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

    public static void put(String key, Object value) {
        CACHE.put(key, new Entry(value, System.currentTimeMillis()));
    }

    public static void clear() { CACHE.clear(); }

    private record Entry(Object val, long ts) {}
}

