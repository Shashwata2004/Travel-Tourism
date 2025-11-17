/* Controls the welcome hero view by pulling the logged-in user’s name,
   showing quick navigation buttons, and steering admins toward their tools.
   Doubles as the friendly handshake screen after login, so it fetches profile
   data through DataCache and keeps admin logic separate from regular users. */
package com.travel.frontend.controller;

import com.travel.frontend.net.ApiClient;
import com.travel.frontend.cache.DataCache;
import com.travel.frontend.ui.Navigator;
import com.travel.frontend.admin.AdminSession;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

/** Shows the gradient welcome hero after login. */
public class WelcomeController {
    @FXML private Label usernameLabel;

    private final ApiClient api = ApiClient.get();

    /* On load, fetches the cached profile (or makes a new API call) on a
       background thread, then updates the label via Platform.runLater so we
       never freeze the hero animation. */
    @FXML private void initialize() {
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
    @FXML private void goDestinations() { showSoon(); }
    @FXML private void goHistory() { showSoon(); }
    @FXML private void goAbout() { showSoon(); }
    /* Checks whether the AdminSession already has a token; if yes, head
       straight to the dashboard, otherwise send the user to the shared login
       so they can prove admin rights. */
    @FXML private void goAdmin() {
        if (AdminSession.getToken() != null) {
            Navigator.goAdminDashboard();
        } else {
            Navigator.goLogin();
        }
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
}
