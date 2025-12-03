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
import javafx.stage.Stage;

public final class Navigator {
    private static Stage STAGE;

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

            Scene scene = STAGE.getScene();
            if (scene == null) {
                scene = new Scene(root, 920, 600);

                // Attach your only stylesheet (login.css). Safe if missing.
                URL cssUrl = Navigator.class.getResource("/css/login.css");
                if (cssUrl != null) {
                    scene.getStylesheets().add(cssUrl.toExternalForm());
                }

                STAGE.setScene(scene);
            } else {
                scene.setRoot(root);
            }

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
    public static void goAdminPackageBookings() { load("admin_package_bookings.fxml"); }

    /* Sanity check before loading anything: reminds developers to call init(...)
       before touching navigation, otherwise JavaFX would throw null pointers. */
    private static void ensureStage() {
        if (STAGE == null) {
            throw new IllegalStateException("Navigator.init(primaryStage) must be called before navigation.");
        }
    }
}
