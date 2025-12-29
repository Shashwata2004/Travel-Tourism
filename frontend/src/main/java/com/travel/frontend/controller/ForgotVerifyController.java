package com.travel.frontend.controller;

import com.travel.frontend.net.ApiClient;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.ParallelTransition;
import javafx.util.Duration;

import java.io.IOException;
import java.net.URL;
import java.util.function.BiConsumer;

public class ForgotVerifyController {
    @FXML private ToggleGroup idTypeGroup;
    @FXML private ToggleButton idNid;
    @FXML private ToggleButton idBirth;
    @FXML private ToggleButton idPassport;
    @FXML private TextField idNumberField;
    @FXML private Button verifyButton;
    @FXML private Button cancelButton;
    @FXML private Button closeButton;
    @FXML private Label errorLabel;

    private Pane host;
    private StackPane overlay;
    private String email;
    private BiConsumer<String, String> onVerified;
    private final ApiClient api = ApiClient.get();

    public static void show(Pane host, String email, BiConsumer<String, String> onVerified) {
        show(host, email, onVerified, null);
    }

    public static void show(Pane host, String email, BiConsumer<String, String> onVerified, Runnable onCancel) {
        try {
            FXMLLoader loader = new FXMLLoader(ForgotVerifyController.class.getResource("/fxml/forgot_verify.fxml"));
            Parent modal = loader.load();
            ensureStyles(modal);
            ForgotVerifyController c = loader.getController();
            c.host = host;
            c.email = email == null ? "" : email.trim();
            c.onVerified = onVerified;
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
        if (verifyButton != null) verifyButton.setOnAction(e -> onVerify());

        applyButtonStyles();

        if (idNumberField != null) {
            idNumberField.textProperty().addListener((obs, o, n) -> updateVerifyState());
        }
        if (idTypeGroup != null) {
            idTypeGroup.selectedToggleProperty().addListener((obs, o, n) -> updateVerifyState());
        }
        updateVerifyState();
    }

    private void applyButtonStyles() {
        if (cancelButton != null) {
            cancelButton.getStyleClass().removeAll("fp-btn", "ghost", "primary");
            cancelButton.getStyleClass().addAll("fp-btn", "ghost");
        }
        if (verifyButton != null) {
            verifyButton.getStyleClass().removeAll("fp-btn", "ghost", "primary");
            verifyButton.getStyleClass().addAll("fp-btn", "primary");
        }
    }

    private static void ensureStyles(Parent modal) {
        if (modal == null) return;
        URL url = ForgotVerifyController.class.getResource("/css/forgot_password.css");
        if (url == null) return;
        String css = url.toExternalForm();
        if (!modal.getStylesheets().contains(css)) {
            modal.getStylesheets().add(css);
        }
    }

    private void updateVerifyState() {
        if (verifyButton == null) return;
        boolean hasType = idTypeGroup != null && idTypeGroup.getSelectedToggle() != null;
        boolean hasNumber = idNumberField != null && !idNumberField.getText().trim().isEmpty();
        verifyButton.setDisable(!(hasType && hasNumber));
    }

    private void onVerify() {
        String normalizedEmail = email == null ? "" : email.trim();
        if (normalizedEmail.isEmpty()) {
            showError("Please enter your email first.");
            return;
        }
        String idType = selectedIdType();
        if (idType == null) {
            showError("Please select an ID type.");
            return;
        }
        String idNumber = idNumberField == null ? "" : idNumberField.getText().trim();
        if (idNumber.isEmpty()) {
            showError("Please enter your ID number.");
            return;
        }
        if (verifyButton != null) verifyButton.setDisable(true);
        new Thread(() -> {
            try {
                api.verifyIdentityForReset(normalizedEmail, idType, idNumber);
                Platform.runLater(() -> {
                    close(null);
                    if (onVerified != null) onVerified.accept(idType, idNumber);
                });
            } catch (ApiClient.ApiException e) {
                Platform.runLater(() -> {
                    showError(e.getMessage());
                    if (verifyButton != null) verifyButton.setDisable(false);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    showError("Verification failed. Please try again.");
                    if (verifyButton != null) verifyButton.setDisable(false);
                });
            }
        }).start();
    }

    private String selectedIdType() {
        if (idNid != null && idNid.isSelected()) return "NID";
        if (idBirth != null && idBirth.isSelected()) return "BIRTH_CERTIFICATE";
        if (idPassport != null && idPassport.isSelected()) return "PASSPORT";
        return null;
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
