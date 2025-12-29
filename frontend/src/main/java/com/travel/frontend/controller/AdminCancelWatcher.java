package com.travel.frontend.controller;

import com.travel.frontend.cache.DataCache;
import com.travel.frontend.admin.AdminSession;
import com.travel.frontend.model.HistoryPackageItem;
import com.travel.frontend.model.HistoryResponse;
import com.travel.frontend.model.HistoryRoomItem;
import com.travel.frontend.model.Profile;
import com.travel.frontend.net.ApiClient;
import com.travel.frontend.ui.Navigator;
import javafx.application.Platform;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Global watcher that surfaces admin-driven cancellations as modals on
 * whatever screen the user is currently on. It runs once per app launch,
 * then keeps polling periodically without blocking the UI thread.
 */
public final class AdminCancelWatcher {
    private static final ApiClient api = ApiClient.get();
    private static final ScheduledExecutorService POLLER = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "cancel-poller");
        t.setDaemon(true);
        return t;
    });
    private static final AtomicBoolean STARTED = new AtomicBoolean(false);
    private static final AtomicBoolean RUNNING = new AtomicBoolean(false);

    private AdminCancelWatcher() {}

    public static void start(Profile profile) {
        if (AdminSession.isAuthenticated()) return;
        if (!STARTED.compareAndSet(false, true)) return;
        new Thread(() -> init(profile)).start();
    }

    private static void init(Profile p) {
        try {
            Profile prof = p != null ? p : DataCache.getOrLoad("myProfile", api::getMyProfile);
            if (prof == null) return;
            checkOnce(prof);
            POLLER.scheduleAtFixedRate(() -> {
                if (!RUNNING.compareAndSet(false, true)) return;
                try {
                    checkOnce(prof);
                } finally {
                    RUNNING.set(false);
                }
            }, 15, 30, TimeUnit.SECONDS);
        } catch (Exception ignored) { }
    }

    private static void checkOnce(Profile p) {
        try {
            if (AdminSession.isAuthenticated()) return;
            HistoryResponse history = api.getHistory();
            String key = "cancelSeen:" + (p.email == null ? "anon" : p.email.toLowerCase());
            Instant seen = loadSeen(key);
            List<AdminCancel> queue = collectAdminCancels(history, seen);
            if (queue.isEmpty()) return;
            Pane host = hostPane();
            if (host == null) {
                System.out.println("[CancelNotice] Host pane missing; deferring notices");
                return;
            }
            Instant latest = queue.get(queue.size() - 1).canceledAt;
            System.out.println("[CancelNotice] Showing " + queue.size() + " admin cancel notice(s)");
            Platform.runLater(() -> showQueue(queue, 0, key, latest, host));
        } catch (Exception ignored) { }
    }

    private record AdminCancel(boolean isPackage, HistoryPackageItem pkg, HistoryRoomItem room, Instant canceledAt) { }

    private static List<AdminCancel> collectAdminCancels(HistoryResponse history, Instant seen) {
        List<AdminCancel> list = new ArrayList<>();
        if (history != null && history.packages != null) {
            for (HistoryPackageItem item : history.packages) {
                if (item.canceledAt != null && "ADMIN".equalsIgnoreCase(safe(item.canceledBy))) {
                    if (seen == null || item.canceledAt.isAfter(seen)) list.add(new AdminCancel(true, item, null, item.canceledAt));
                }
            }
        }
        if (history != null && history.rooms != null) {
            for (HistoryRoomItem item : history.rooms) {
                if (item.canceledAt != null && "ADMIN".equalsIgnoreCase(safe(item.canceledBy))) {
                    if (seen == null || item.canceledAt.isAfter(seen)) list.add(new AdminCancel(false, null, item, item.canceledAt));
                }
            }
        }
        list.sort(Comparator.comparing(a -> a.canceledAt));
        return list;
    }

    private static void showQueue(List<AdminCancel> queue, int idx, String cacheKey, Instant latest, Pane host) {
        if (idx >= queue.size()) {
            saveSeen(cacheKey, latest);
            return;
        }
        AdminCancel entry = queue.get(idx);
        Runnable next = () -> showQueue(queue, idx + 1, cacheKey, latest, host);
        if (entry.isPackage && entry.pkg != null) {
            CancelNoticeController.show(host, entry.pkg, next);
        } else if (!entry.isPackage && entry.room != null) {
            CancelNoticeController.show(host, entry.room, next);
        } else {
            next.run();
        }
    }

    private static Pane hostPane() {
        StackPane peek = Navigator.peekRoot();
        return peek != null ? peek : null;
    }

    private static Instant loadSeen(String key) {
        try {
            Object mem = DataCache.peek(key);
            if (mem instanceof Instant i) return i;
        } catch (Exception ignored) { }
        try {
            Path dir = Paths.get(System.getProperty("user.home"), ".travel-notices");
            Path file = dir.resolve(sanitize(key) + ".txt");
            if (Files.exists(file)) {
                String s = Files.readString(file).trim();
                if (!s.isEmpty()) {
                    try {
                        return Instant.parse(s);
                    } catch (Exception ignored) {
                        try {
                            long epoch = Long.parseLong(s);
                            if (epoch > 9999999999L) {
                                return Instant.ofEpochMilli(epoch);
                            } else {
                                return Instant.ofEpochSecond(epoch);
                            }
                        } catch (Exception ignoredToo) { }
                    }
                }
            }
        } catch (Exception ignored) { }
        return null;
    }

    private static void saveSeen(String key, Instant value) {
        if (value == null) return;
        DataCache.put(key, value);
        try {
            Path dir = Paths.get(System.getProperty("user.home"), ".travel-notices");
            Files.createDirectories(dir);
            Path file = dir.resolve(sanitize(key) + ".txt");
            Files.writeString(file, value.toString());
        } catch (Exception ignored) { }
    }

    private static String sanitize(String key) {
        return key == null ? "notice" : key.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private static String safe(String s) { return s == null ? "" : s; }
}
