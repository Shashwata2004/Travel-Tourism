/* Controls the welcome hero view by pulling the logged-in user’s name,
   showing quick navigation buttons, and steering admins toward their tools.
   Doubles as the friendly handshake screen after login, so it fetches profile
   data through DataCache and keeps admin logic separate from regular users. */
package com.travel.frontend.controller;

import com.travel.frontend.net.ApiClient;
import com.travel.frontend.cache.DataCache;
import com.travel.frontend.session.Session;
import com.travel.frontend.admin.AdminSession;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import com.travel.frontend.ui.Navigator;
import com.travel.frontend.model.HistoryResponse;
import com.travel.frontend.model.HistoryPackageItem;
import com.travel.frontend.model.HistoryRoomItem;
import com.travel.frontend.model.Profile;

import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;
import javafx.animation.Timeline;
import javafx.animation.Interpolator;
import javafx.scene.Node;
import javafx.scene.shape.Circle;
import java.util.Random;
import java.time.Instant;

/** Shows the gradient welcome hero after login. */
public class WelcomeController {
    @FXML private Label usernameLabel;
    @FXML private VBox contentBox;
    @FXML private Pane floatingDots;
    @FXML private StackPane rootPane;

    private final ApiClient api = ApiClient.get();
    private Profile profile;

    /* On load, fetches the cached profile (or makes a new API call) on a
       background thread, then updates the label via Platform.runLater so we
       never freeze the hero animation. */
    @FXML private void initialize() {
        runEntranceAnimation();
        startFloatAnimations();
        // Fetch profile to display username
        new Thread(() -> {
            try {
                Profile p = DataCache.getOrLoad("myProfile", api::getMyProfile);
                this.profile = p;
                Platform.runLater(() -> usernameLabel.setText(p.username));
                AdminCancelWatcher.start(p);
            } catch (Exception ignore) { }
        }).start();
    }

    @FXML private void goPersonal() { Navigator.goHome(); }
    @FXML private void goPackages() { Navigator.goPackages(); }
    @FXML private void goDestinations() { Navigator.goDestinations(); }
    @FXML private void goHistory() { Navigator.goHistory(); }
    @FXML private void goAbout() { Navigator.goAbout(); }
    @FXML private void goLogout() {
        DataCache.clear();
        Session.clear();
        AdminSession.clear();
        Navigator.goLogin();
    }
    /* Reusable “coming soon” helper that shows a JavaFX Alert so we don’t need
       separate placeholder screens yet. */
    private void showSoon() {
        javafx.scene.control.Alert a = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
        a.setHeaderText(null);
        a.setTitle("Coming soon");
        a.setContentText("This section will be available later.");
        a.showAndWait();
    }

    private void checkAdminCancellations(Profile p) {
        if (p == null || rootPane == null) return;
        new Thread(() -> {
            try {
                HistoryResponse history = api.getHistory();
                String key = "cancelSeen:" + (p.email == null ? "anon" : p.email.toLowerCase());
                Instant seen = loadSeen(key);
                var queue = collectAdminCancels(history, seen);
                if (queue.isEmpty()) return;
                final java.util.List<AdminCancel> queueFinal = queue;
                final Pane hostFinal = hostPane();
                final Instant latestFinal = queue.get(queue.size() - 1).canceledAt;
                System.out.println("[CancelNotice] Showing " + queueFinal.size() + " admin cancel notice(s)");
                // Persist immediately so repeated app opens don't re-show the same notice
                saveSeen(key, latestFinal);
                Platform.runLater(() -> showQueue(queueFinal, 0, key, latestFinal, hostFinal));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private String safe(String s) { return s == null ? "" : s; }

    private static class AdminCancel {
        final boolean isPackage;
        final HistoryPackageItem pkg;
        final HistoryRoomItem room;
        final Instant canceledAt;
        AdminCancel(HistoryPackageItem p) { this.isPackage = true; this.pkg = p; this.room = null; this.canceledAt = p.canceledAt; }
        AdminCancel(HistoryRoomItem r) { this.isPackage = false; this.pkg = null; this.room = r; this.canceledAt = r.canceledAt; }
    }

    private java.util.List<AdminCancel> collectAdminCancels(HistoryResponse history, Instant seen) {
        java.util.List<AdminCancel> list = new java.util.ArrayList<>();
        if (history != null && history.packages != null) {
            for (HistoryPackageItem item : history.packages) {
                if (item.canceledAt != null && "ADMIN".equalsIgnoreCase(safe(item.canceledBy))) {
                    if (seen == null || item.canceledAt.isAfter(seen)) list.add(new AdminCancel(item));
                }
            }
        }
        if (history != null && history.rooms != null) {
            for (HistoryRoomItem item : history.rooms) {
                if (item.canceledAt != null && "ADMIN".equalsIgnoreCase(safe(item.canceledBy))) {
                    if (seen == null || item.canceledAt.isAfter(seen)) list.add(new AdminCancel(item));
                }
            }
        }
        list.sort(java.util.Comparator.comparing(a -> a.canceledAt));
        return list;
    }

    private void showQueue(java.util.List<AdminCancel> queue, int idx, String cacheKey, Instant latest, Pane host) {
        if (host == null) {
            System.out.println("[CancelNotice] Host pane missing; deferring notices");
            return;
        }
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

    private Pane hostPane() {
        if (rootPane != null && rootPane.getScene() != null) return rootPane;
        try {
            if (rootPane != null && rootPane.getScene() != null && rootPane.getScene().getRoot() instanceof Pane p) {
                return p;
            }
        } catch (Exception ignored) { }
        // fallback to last known host
        Pane cached = com.travel.frontend.ui.Navigator.peekRoot();
        return cached instanceof StackPane ? cached : cached;
    }

    private Instant loadSeen(String key) {
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
                            // if stored as millis vs seconds
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

    private void saveSeen(String key, Instant value) {
        if (value == null) return;
        DataCache.put(key, value);
        try {
            Path dir = Paths.get(System.getProperty("user.home"), ".travel-notices");
            Files.createDirectories(dir);
            Path file = dir.resolve(sanitize(key) + ".txt");
            Files.writeString(file, value.toString());
        } catch (Exception ignored) { }
    }

    private String sanitize(String key) {
        return key == null ? "notice" : key.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private void runEntranceAnimation() {
        if (contentBox == null) return;
        contentBox.setOpacity(0.0);
        contentBox.setTranslateY(24);

        FadeTransition fade = new FadeTransition(Duration.millis(600), contentBox);
        fade.setFromValue(0.0);
        fade.setToValue(1.0);
        TranslateTransition slide = new TranslateTransition(Duration.millis(600), contentBox);
        slide.setFromY(24);
        slide.setToY(0);

        new ParallelTransition(fade, slide).play();
    }

    private void startFloatAnimations() {
        Platform.runLater(() -> {
            Random rnd = new Random();

            if (floatingDots != null) {
                for (Node n : floatingDots.getChildren()) {
                    if (n instanceof Circle) {
                        TranslateTransition tt = new TranslateTransition(Duration.millis(1600 + rnd.nextInt(700)), n);
                        double amp = 6 + rnd.nextDouble() * 8;
                        tt.setFromY(-amp);
                        tt.setToY(amp);
                        tt.setAutoReverse(true);
                        tt.setCycleCount(Timeline.INDEFINITE);
                        tt.setDelay(Duration.millis(rnd.nextInt(400)));
                        tt.setInterpolator(Interpolator.EASE_BOTH);
                        tt.play();
                    }
                }
            }

            if (contentBox != null) {
                for (Node n : contentBox.lookupAll(".heroAction")) {
                    TranslateTransition tt = new TranslateTransition(Duration.millis(2000 + rnd.nextInt(600)), n);
                    tt.setFromY(0);
                    tt.setToY(-6);
                    tt.setAutoReverse(true);
                    tt.setCycleCount(Timeline.INDEFINITE);
                    tt.setDelay(Duration.millis(rnd.nextInt(300)));
                    tt.setInterpolator(Interpolator.EASE_BOTH);
                    tt.play();
                }
            }
        });
    }
}
