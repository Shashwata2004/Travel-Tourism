package com.urban.frontend.controller;

import com.urban.frontend.net.ApiClient;
import com.urban.frontend.ui.Navigator;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

/** Shows the gradient welcome hero after login. */
public class WelcomeController {
    @FXML private Label usernameLabel;

    private final ApiClient api = ApiClient.get();

    @FXML private void initialize() {
        // Fetch profile to display username
        new Thread(() -> {
            try {
                var p = api.getMyProfile();
                Platform.runLater(() -> usernameLabel.setText(p.username));
            } catch (Exception ignore) { }
        }).start();
    }

    @FXML private void goPersonal() { Navigator.goHome(); }
    @FXML private void goPackages() { Navigator.goPackages(); }
    @FXML private void goDestinations() { showSoon(); }
    @FXML private void goHistory() { showSoon(); }
    @FXML private void goAbout() { showSoon(); }

    private void showSoon() {
        javafx.scene.control.Alert a = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
        a.setHeaderText(null);
        a.setTitle("Coming soon");
        a.setContentText("This section will be available later.");
        a.showAndWait();
    }
}
