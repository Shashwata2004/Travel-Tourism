/* Centralized router that keeps the main window alive and swaps in each FXML
   screen so navigation buttons can simply call methods like goLogin().
   Owns the shared Stage + Scene combo, which gives us quick fades between
   pages without creating new windows. */
package com.travel.frontend.ui;

import java.net.URL;
import java.util.Objects;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.Node;
import javafx.scene.control.ToggleButton;
import javafx.stage.Stage;

import com.travel.frontend.ui.ThemeManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.StackPane;
import javafx.animation.RotateTransition;
import javafx.util.Duration;

public final class Navigator {
    private static Stage STAGE;
    private static StackPane ROOT_WRAPPER;
    private static ToggleButton THEME_TOGGLE;
    private static StackPane LAST_ROOT;

    private Navigator() {}

    /* Called once during app startup so we can capture the Stage that JavaFX
       hands us. Uses Objects.requireNonNull to fail fast if the platform is
       misconfigured. */
    public static void init(Stage stage) {
        STAGE = Objects.requireNonNull(stage, "Primary Stage must not be null");
        STAGE.setTitle("Travel & Tourism — Auth");
        STAGE.setResizable(true);
    }

    /* Generic loader that finds an FXML file on the classpath, creates or reuses
       the shared Scene, and applies the login.css stylesheet. Keeps navigation
       code tiny while relying on FXMLLoader to build the UI tree. */
    public static void load(String fxmlFileName) {
        ensureStage();
        final String fxmlPath = "/fxml/" + fxmlFileName;

        try {
            URL fxmlUrl = Navigator.class.getResource(fxmlPath);
            if (fxmlUrl == null) {
                throw new IllegalStateException("FXML not found on classpath: " + fxmlPath);
            }

            Parent root = FXMLLoader.load(fxmlUrl);
            boolean isAdmin = fxmlFileName.startsWith("admin_");
            if (isAdmin && ThemeManager.isDark()) {
                ThemeManager.setTheme(ThemeManager.Theme.LIGHT);
            }

            Scene scene = STAGE.getScene();
            if (scene == null) {
                ROOT_WRAPPER = new StackPane();
                ROOT_WRAPPER.getChildren().add(root);
                ROOT_WRAPPER.setPickOnBounds(true);
                LAST_ROOT = ROOT_WRAPPER;

                scene = new Scene(ROOT_WRAPPER, 920, 600);

                // Attach base stylesheet (login.css). Safe if missing.
                URL cssUrl = Navigator.class.getResource("/css/login.css");
                if (cssUrl != null) {
                    scene.getStylesheets().add(cssUrl.toExternalForm());
                }
                URL toggleCss = Navigator.class.getResource("/css/theme_toggle.css");
                if (toggleCss != null) scene.getStylesheets().add(toggleCss.toExternalForm());

                createThemeToggle();
                ROOT_WRAPPER.getChildren().add(THEME_TOGGLE);
                StackPane.setAlignment(THEME_TOGGLE, Pos.BOTTOM_RIGHT);
                StackPane.setMargin(THEME_TOGGLE, new Insets(0, 18, 18, 0));

                STAGE.setScene(scene);
            } else {
                // Replace content inside wrapper while keeping overlay toggle
                if (ROOT_WRAPPER != null) {
                if (!ROOT_WRAPPER.getChildren().isEmpty()) {
                    ROOT_WRAPPER.getChildren().set(0, root);
                } else {
                    ROOT_WRAPPER.getChildren().add(0, root);
                }
                LAST_ROOT = ROOT_WRAPPER;
            } else {
                scene.setRoot(root);
            }
            }
            if (THEME_TOGGLE != null) {
                THEME_TOGGLE.setVisible(!isAdmin);
                THEME_TOGGLE.setManaged(!isAdmin);
            }
            ThemeManager.apply(scene);
            syncThemeToggles(scene);
            System.out.println("[Navigator] Loaded " + fxmlFileName + " with theme=" + ThemeManager.getTheme());

            STAGE.centerOnScreen();
            STAGE.show();

        } catch (Exception e) {
            throw new RuntimeException("Failed to load " + fxmlFileName + ": " + e.getMessage(), e);
        }
    }

    // Convenience routes (ensure matching FXML files exist in src/main/resources/fxml)
    public static void goLogin()    { load("login.fxml"); }
    public static void goRegister() { load("register.fxml"); }
    public static void goHome()  { load("home.fxml"); }
    public static void goPackages() { load("packages.fxml"); }
    public static void goDestinations() { load("destinations.fxml"); }
    public static void goHotelSearch() { load("hotel_search.fxml"); }
    public static void goHotelDetails() { load("hotel_details.fxml"); }
    public static void goWelcome() { load("welcome.fxml"); }
    // Admin login view removed; reuse main login screen
    public static void goAdminLogin() { load("login.fxml"); }
    public static void goAdminDashboard() { load("admin_dashboard.fxml"); }
    public static void goAdminDestinations() { load("admin_destinations.fxml"); }
    public static void goAdminHotels() { load("admin_hotels.fxml"); }
    public static void goAdminRooms() { load("admin_rooms.fxml"); }
    public static void goAdminRoomBookings() { load("admin_room_bookings.fxml"); }
    public static void goAdminAllPackageBookings() { load("admin_all_package_bookings.fxml"); }
    public static void goAdminAllRoomBookings() { load("admin_all_room_bookings.fxml"); }
    public static void goHistory() { load("history.fxml"); }

    public static void applyTheme() {
        if (STAGE != null && STAGE.getScene() != null) {
            com.travel.frontend.ui.ThemeManager.apply(STAGE.getScene());
            syncThemeToggles(STAGE.getScene());
        }
    }

    private static void createThemeToggle() {
        if (THEME_TOGGLE != null) return;
        THEME_TOGGLE = new ToggleButton();
        THEME_TOGGLE.getStyleClass().add("themeFab");
        THEME_TOGGLE.setSelected(ThemeManager.isDark());
        updateThemeIcon();
        THEME_TOGGLE.setOnAction(e -> {
            ThemeManager.setTheme(THEME_TOGGLE.isSelected() ? ThemeManager.Theme.DARK : ThemeManager.Theme.LIGHT);
            applyTheme();
            playToggleAnimation();
            updateThemeIcon();
            System.out.println("[Navigator] Overlay theme toggle -> " + ThemeManager.getTheme());
        });
    }

    private static void playToggleAnimation() {
        if (THEME_TOGGLE == null) return;
        RotateTransition rt = new RotateTransition(Duration.millis(360), THEME_TOGGLE);
        rt.setByAngle(360);
        rt.setCycleCount(1);
        rt.play();
    }

    private static void updateThemeIcon() {
        if (THEME_TOGGLE == null) return;
        THEME_TOGGLE.setText(ThemeManager.isDark() ? "☀" : "☾");
    }
    public static void goAdminPackageBookings() { load("admin_package_bookings.fxml"); }

    // Allows other controllers to retrieve the current root wrapper for overlays.
    public static StackPane peekRoot() { return LAST_ROOT; }

    private static void syncThemeToggles(Scene scene) {
        if (THEME_TOGGLE != null) {
            THEME_TOGGLE.setSelected(ThemeManager.isDark());
            updateThemeIcon();
        }
        if (scene != null) {
            Parent root = scene.getRoot();
            if (root != null) {
                for (Node node : root.lookupAll("#themeToggle")) {
                    if (node instanceof ToggleButton tb) {
                        tb.setSelected(ThemeManager.isDark());
                    }
                }
            }
        }
    }

    /* Sanity check before loading anything: reminds developers to call init(...)
       before touching navigation, otherwise JavaFX would throw null pointers. */
    private static void ensureStage() {
        if (STAGE == null) {
            throw new IllegalStateException("Navigator.init(primaryStage) must be called before navigation.");
        }
    }
}
