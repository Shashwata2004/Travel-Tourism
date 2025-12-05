/* Controls the welcome hero view by pulling the logged-in user’s name,
   showing quick navigation buttons, and steering admins toward their tools.
   Doubles as the friendly handshake screen after login, so it fetches profile
   data through DataCache and keeps admin logic separate from regular users. */
package com.travel.frontend.controller;

import com.travel.frontend.net.ApiClient;
import com.travel.frontend.cache.DataCache;
import com.travel.frontend.ui.Navigator;

import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import javafx.animation.Timeline;
import javafx.animation.Interpolator;
import javafx.scene.Node;
import javafx.scene.shape.Circle;
import java.util.Random;

/** Shows the gradient welcome hero after login. */
public class WelcomeController {
    @FXML private Label usernameLabel;
    @FXML private VBox contentBox;
    @FXML private Pane floatingDots;

    private final ApiClient api = ApiClient.get();

    /* On load, fetches the cached profile (or makes a new API call) on a
       background thread, then updates the label via Platform.runLater so we
       never freeze the hero animation. */
    @FXML private void initialize() {
        runEntranceAnimation();
        startFloatAnimations();
        // Fetch profile to display username
        new Thread(() -> {
            try {
                var p = DataCache.getOrLoad("myProfile", api::getMyProfile);
                Platform.runLater(() -> usernameLabel.setText(p.username));
            } catch (Exception ignore) { }
        }).start();
    }

    @FXML private void goPersonal() { Navigator.goHome(); }
    @FXML private void goPackages() { Navigator.goPackages(); }
    @FXML private void goDestinations() { Navigator.goDestinations(); }
    @FXML private void goHistory() { Navigator.goHistory(); }
    @FXML private void goAbout() { showSoon(); }
    /* Reusable “coming soon” helper that shows a JavaFX Alert so we don’t need
       separate placeholder screens yet. */
    private void showSoon() {
        javafx.scene.control.Alert a = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
        a.setHeaderText(null);
        a.setTitle("Coming soon");
        a.setContentText("This section will be available later.");
        a.showAndWait();
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
