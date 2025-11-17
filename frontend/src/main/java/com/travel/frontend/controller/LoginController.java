/* Powers the login form for both regular users and admins by checking basic
   input, calling the right backend channel, and switching to the proper view
   once someone signs in. Splits between the REST ApiClient and the socket-based
   admin client so each role keeps its own journey but shares the same UI. */
package com.travel.frontend.controller;

import com.travel.frontend.net.ApiClient;
import com.travel.frontend.session.Session;
import com.travel.frontend.ui.Navigator;
import com.travel.frontend.admin.AdminSocketClient;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;

public class LoginController {

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label statusLabel;
    @FXML private RadioButton userMode;
    @FXML private RadioButton adminMode;

    private final ApiClient api = ApiClient.get();
    private final AdminSocketClient adminClient = new AdminSocketClient();

    /* Runs when the user presses “Sign in.” Validates form fields, decides
       whether to use the REST login or the AdminSocketClient, then runs the
       network work on a background Thread while updating the UI via Platform.runLater. */
    @FXML
    private void onLogin() {
        statusLabel.setText("");
        String email = emailField.getText() == null ? "" : emailField.getText().trim();
        String pass  = passwordField.getText() == null ? "" : passwordField.getText();

        if (email.isEmpty()) { statusLabel.setText("Please enter your email."); return; }
        if (pass.isEmpty())  { statusLabel.setText("Please enter your password."); return; }

        statusLabel.setText("Signing in...");
        boolean adminSelected = adminMode != null && adminMode.isSelected();

        new Thread(() -> {
            try {
                if (adminSelected) {
                    boolean ok = adminClient.auth(email, pass);
                    Platform.runLater(() -> {
                        if (ok) {
                            statusLabel.setText("Admin logged in");
                            Navigator.goAdminDashboard();
                        } else {
                            statusLabel.setText("Invalid admin credentials");
                            showError("Admin login failed", "Invalid admin credentials");
                        }
                    });
                    return;
                } else {
                    String jwt = api.login(email, pass);
                    Session.setToken(jwt);
                    Platform.runLater(() -> {
                        statusLabel.setText("Logged in!");
                        Navigator.goWelcome();
                    });
                }
            } catch (ApiClient.ApiException e) {
                Platform.runLater(() -> {
                    statusLabel.setText(e.getMessage());
                    showError("Login failed", e.getMessage());
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    statusLabel.setText("Unexpected error. Please try again.");
                    showError("Unexpected error", "Something went wrong. Please try again.");
                });
            }
        }).start();
    }

    @FXML
    private void goToRegister() {
        Navigator.goRegister();
    }

    @FXML
    private void onForgotPassword() {
        showInfo("Coming soon", "Password reset flow is not implemented yet.");
    }

    private void showInfo(String title, String msg) {
        Alert a = new Alert(AlertType.INFORMATION);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }

    private void showError(String title, String msg) {
        Alert a = new Alert(AlertType.ERROR);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }
}
