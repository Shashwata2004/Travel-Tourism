package com.travel.frontend.controller;

import com.travel.frontend.net.ApiClient;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.RotateTransition;
import javafx.util.Duration;

import java.io.IOException;
import java.net.URL;

public class ForgotResetController {
    @FXML private PasswordField newPasswordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private TextField newPasswordTextField;
    @FXML private TextField confirmPasswordTextField;
    @FXML private Button confirmButton;
    @FXML private Button cancelButton;
    @FXML private Button closeButton;
    @FXML private Button toggleNewBtn;
    @FXML private Button toggleConfirmBtn;
    @FXML private Label errorLabel;

    private Pane host;
    private StackPane overlay;
    private String email;
    private String idType;
    private String idNumber;
    private Runnable onSuccess;
    private final ApiClient api = ApiClient.get();

    public static void show(Pane host, String email, String idType, String idNumber, Runnable onSuccess) {
        show(host, email, idType, idNumber, onSuccess, null);
    }

    public static void show(Pane host, String email, String idType, String idNumber, Runnable onSuccess, Runnable onCancel) {
        try {
            FXMLLoader loader = new FXMLLoader(ForgotResetController.class.getResource("/fxml/forgot_reset.fxml"));
            Parent modal = loader.load();
            ensureStyles(modal);
            ForgotResetController c = loader.getController();
            c.host = host;
            c.email = email == null ? "" : email.trim();
            c.idType = idType;
            c.idNumber = idNumber;
            c.onSuccess = onSuccess;
            c.init(onCancel);
            c.showOverlay(modal);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void init(Runnable onCancel) {
        if (errorLabel != null) errorLabel.setText("");
        if (cancelButton != null) cancelButton.setOnAction(e -> close(onCancel));
        if (closeButton != null) closeButton.setOnAction(e -> close(onCancel));
        if (confirmButton != null) confirmButton.setOnAction(e -> onConfirm());
        if (toggleNewBtn != null) toggleNewBtn.setOnAction(e -> togglePasswordVisibility(newPasswordField, newPasswordTextField, toggleNewBtn));
        if (toggleConfirmBtn != null) toggleConfirmBtn.setOnAction(e -> togglePasswordVisibility(confirmPasswordField, confirmPasswordTextField, toggleConfirmBtn));

        applyButtonStyles();

        bindPasswordFields(newPasswordField, newPasswordTextField);
        bindPasswordFields(confirmPasswordField, confirmPasswordTextField);

        if (newPasswordField != null) {
            newPasswordField.textProperty().addListener((obs, o, n) -> updateConfirmState());
        }
        if (confirmPasswordField != null) {
            confirmPasswordField.textProperty().addListener((obs, o, n) -> updateConfirmState());
        }
        updateConfirmState();
    }

    private void bindPasswordFields(PasswordField hiddenField, TextField visibleField) {
        if (hiddenField == null || visibleField == null) return;
        visibleField.setVisible(false);
        visibleField.setManaged(false);
        visibleField.textProperty().bindBidirectional(hiddenField.textProperty());
    }

    private void togglePasswordVisibility(PasswordField hiddenField, TextField visibleField, Button toggleBtn) {
        if (hiddenField == null || visibleField == null) return;
        boolean show = !visibleField.isVisible();
        visibleField.setVisible(show);
        visibleField.setManaged(show);
        hiddenField.setVisible(!show);
        hiddenField.setManaged(!show);
        if (show) {
            visibleField.requestFocus();
            visibleField.positionCaret(visibleField.getText().length());
        } else {
            hiddenField.requestFocus();
            hiddenField.positionCaret(hiddenField.getText().length());
        }
        animateToggle(toggleBtn);
    }

    private void animateToggle(Button toggleBtn) {
        if (toggleBtn == null) return;
        RotateTransition rt = new RotateTransition(Duration.millis(140), toggleBtn);
        rt.setFromAngle(0);
        rt.setToAngle(20);
        rt.setAutoReverse(true);
        rt.setCycleCount(2);
        rt.play();
    }

    private void applyButtonStyles() {
        if (cancelButton != null) {
            cancelButton.getStyleClass().removeAll("fp-btn", "ghost", "primary");
            cancelButton.getStyleClass().addAll("fp-btn", "ghost");
        }
        if (confirmButton != null) {
            confirmButton.getStyleClass().removeAll("fp-btn", "ghost", "primary");
            confirmButton.getStyleClass().addAll("fp-btn", "primary");
        }
    }

    private static void ensureStyles(Parent modal) {
        if (modal == null) return;
        URL url = ForgotResetController.class.getResource("/css/forgot_password.css");
        if (url == null) return;
        String css = url.toExternalForm();
        if (!modal.getStylesheets().contains(css)) {
            modal.getStylesheets().add(css);
        }
    }

    private void updateConfirmState() {
        if (confirmButton == null) return;
        boolean filled = newPasswordField != null && confirmPasswordField != null
                && !newPasswordField.getText().trim().isEmpty()
                && !confirmPasswordField.getText().trim().isEmpty();
        confirmButton.setDisable(!filled);
    }

    private void onConfirm() {
        String pwd = newPasswordField == null ? "" : newPasswordField.getText();
        String confirm = confirmPasswordField == null ? "" : confirmPasswordField.getText();
        if (pwd.trim().isEmpty() || confirm.trim().isEmpty()) {
            showError("Password fields cannot be empty.");
            return;
        }
        if (!pwd.equals(confirm)) {
            showError("Passwords do not match.");
            return;
        }
        if (confirmButton != null) confirmButton.setDisable(true);
        new Thread(() -> {
            try {
                api.resetPassword(email, idType, idNumber, pwd);
                Platform.runLater(() -> {
                    close(null);
                    if (onSuccess != null) onSuccess.run();
                });
            } catch (ApiClient.ApiException e) {
                Platform.runLater(() -> {
                    showError(e.getMessage());
                    if (confirmButton != null) confirmButton.setDisable(false);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    showError("Password reset failed. Please try again.");
                    if (confirmButton != null) confirmButton.setDisable(false);
                });
            }
        }).start();
    }

    private void showError(String msg) {
        if (errorLabel != null) {
            errorLabel.setText(msg);
        }
    }

    private void showOverlay(Parent modal) {
        overlay = (StackPane) modal;
        overlay.setPickOnBounds(true);
        if (host != null && !host.getChildren().contains(overlay)) {
            host.getChildren().add(overlay);
            animate(modal);
        }
    }

    private void animate(Parent modal) {
        if (modal == null) return;
        modal.setOpacity(0);
        modal.setScaleX(0.96);
        modal.setScaleY(0.96);
        FadeTransition ft = new FadeTransition(Duration.millis(220), modal);
        ft.setFromValue(0);
        ft.setToValue(1);
        ScaleTransition st = new ScaleTransition(Duration.millis(220), modal);
        st.setFromX(0.96);
        st.setFromY(0.96);
        st.setToX(1);
        st.setToY(1);
        new ParallelTransition(ft, st).play();
    }

    private void close(Runnable onCancel) {
        if (host != null && overlay != null) {
            host.getChildren().remove(overlay);
        }
        if (onCancel != null) {
            onCancel.run();
        }
    }
}
