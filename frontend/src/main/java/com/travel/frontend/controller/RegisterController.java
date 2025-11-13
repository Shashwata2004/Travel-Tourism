package com.travel.frontend.controller;

import com.travel.frontend.net.ApiClient;
import com.travel.frontend.ui.Navigator;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class RegisterController {

    @FXML private TextField emailField;
    @FXML private TextField usernameField;
    @FXML private TextField locationField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmField;
    @FXML private Label statusLabel;

    private final ApiClient api = ApiClient.get();

    // Called by: <Button onAction="#onRegister"/>
    @FXML
    private void onRegister() {
        statusLabel.setText("");

        String email    = safe(emailField.getText());
        String username = safe(usernameField.getText());
        String location = safe(locationField.getText());
        String pass     = passwordField.getText() == null ? "" : passwordField.getText();
        String pass2    = confirmField.getText() == null ? "" : confirmField.getText();

        if (email.isEmpty())    { statusLabel.setText("Email is required."); return; }
        if (username.isEmpty()) { statusLabel.setText("Username is required."); return; }
        if (pass.isEmpty())     { statusLabel.setText("Password is required."); return; }
        if (!pass.equals(pass2)){ statusLabel.setText("Passwords do not match."); return; }

        statusLabel.setText("Creating your account...");
        new Thread(() -> {
            try {
                api.register(email, username, pass, location); // 200 -> "User registered successfully!"
                Platform.runLater(() -> {
                    statusLabel.setText("✅ Registered!");
                    showInfo("Registration successful", "Your account was created. You can now log in.");
                    Navigator.goLogin(); // optionally auto-navigate after success
                });
            } catch (ApiClient.ApiException e) {
                Platform.runLater(() -> {
                    statusLabel.setText(e.getMessage());
                    showError("Registration failed", e.getMessage());
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    statusLabel.setText("Unexpected error. Please try again.");
                    showError("Unexpected error", "Something went wrong. Please try again.");
                });
            }
        }).start();
    }

    // Called by: <Hyperlink onAction="#onGoLogin"/>
    @FXML
    private void onGoLogin() {
        Navigator.goLogin();
    }

    private String safe(String s) { return s == null ? "" : s.trim(); }

    private void showInfo(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }

    private void showError(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }
}
