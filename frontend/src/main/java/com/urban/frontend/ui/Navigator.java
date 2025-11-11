package com.urban.frontend.ui;

import java.net.URL;
import java.util.Objects;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public final class Navigator {
    private static Stage STAGE;

    private Navigator() {}

    /** Call once from MainApp.start(...) */
    public static void init(Stage stage) {
        STAGE = Objects.requireNonNull(stage, "Primary Stage must not be null");
        STAGE.setTitle("Travel & Tourism — Auth");
        STAGE.setResizable(true);
    }

    /** Generic loader that swaps the root of the single Scene (creating it if needed). */
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

    private static void ensureStage() {
        if (STAGE == null) {
            throw new IllegalStateException("Navigator.init(primaryStage) must be called before navigation.");
        }
    }
}
