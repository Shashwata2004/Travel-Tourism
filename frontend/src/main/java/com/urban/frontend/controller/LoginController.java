package com.urban.frontend.controller;

import com.urban.frontend.net.ApiClient;
import com.urban.frontend.session.Session;
import com.urban.frontend.ui.Navigator;

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

    @FXML
    private void onLogin() {
        statusLabel.setText("");
        String email = emailField.getText() == null ? "" : emailField.getText().trim();
        String pass  = passwordField.getText() == null ? "" : passwordField.getText();

        if (email.isEmpty()) { statusLabel.setText("Please enter your email."); return; }
        if (pass.isEmpty())  { statusLabel.setText("Please enter your password."); return; }

        statusLabel.setText("Signing in...");
        // Decide mode
        boolean adminSelected = adminMode != null && adminMode.isSelected();

        new Thread(() -> {
            try {
                String jwt;
                if (adminSelected) {
                    Platform.runLater(() -> {
                        statusLabel.setText("");
                        showInfo("Admin login", "Admin login will be enabled later.");
                    });
                    return;
                } else {
                    jwt = api.login(email, pass);
                }

                Session.setToken(jwt);
                Platform.runLater(() -> {
                    statusLabel.setText("✅ Logged in!");
                    Navigator.goWelcome();
                });
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
