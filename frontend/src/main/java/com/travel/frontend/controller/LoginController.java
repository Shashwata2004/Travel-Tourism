/* Powers the login form for both regular users and admins by checking basic
   input, calling the right backend channel, and switching to the proper view
   once someone signs in. Splits between the REST ApiClient and the socket-based
   admin client so each role keeps its own journey but shares the same UI. */
package com.travel.frontend.controller;

import com.travel.frontend.net.ApiClient;
import com.travel.frontend.session.Session;
import com.travel.frontend.ui.Navigator;
import com.travel.frontend.admin.AdminSocketClient;

import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

public class LoginController {

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label statusLabel;
    @FXML private RadioButton userMode;
    @FXML private RadioButton adminMode;
    @FXML private StackPane brandPane;
    @FXML private VBox brandCopy;
    @FXML private VBox cardBox;

    private final ApiClient api = ApiClient.get();
    private final AdminSocketClient adminClient = new AdminSocketClient();

    @FXML
    private void initialize() {
        runEntranceAnimation();
    }

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

    // Soft entrance animation in pure Java (no scripts)
    private void runEntranceAnimation() {
        if (brandPane == null || cardBox == null) return;

        // Delay start until layout is ready so width is accurate for the slide-in.
        Platform.runLater(() -> {
            double width = brandPane.getWidth();
            if (width <= 0) width = brandPane.getPrefWidth() > 0 ? brandPane.getPrefWidth() : 480;
            double startX = -width; // slide from fully offscreen left

            brandPane.setTranslateX(startX);
            brandPane.setOpacity(1.0); // keep image visible during slide
            if (brandCopy != null) {
                brandCopy.setOpacity(0.0);
                brandCopy.setTranslateY(6);
            }
            cardBox.setOpacity(0.0);
            cardBox.setTranslateY(20);

            TranslateTransition slideHero = new TranslateTransition(Duration.millis(650), brandPane);
            slideHero.setFromX(startX);
            slideHero.setToX(0);
            slideHero.setInterpolator(javafx.animation.Interpolator.EASE_BOTH);

            FadeTransition fadeCopy = brandCopy == null ? null : new FadeTransition(Duration.millis(450), brandCopy);
            if (fadeCopy != null) {
                fadeCopy.setFromValue(0.0);
                fadeCopy.setToValue(1.0);
            }
            TranslateTransition liftCopy = brandCopy == null ? null : new TranslateTransition(Duration.millis(450), brandCopy);
            if (liftCopy != null) {
                liftCopy.setFromY(6);
                liftCopy.setToY(0);
            }

            FadeTransition fadeCard = new FadeTransition(Duration.millis(500), cardBox);
            fadeCard.setFromValue(0.0);
            fadeCard.setToValue(1.0);
            TranslateTransition slideCard = new TranslateTransition(Duration.millis(500), cardBox);
            slideCard.setFromY(20);
            slideCard.setToY(0);

            SequentialTransition seq = new SequentialTransition(
                    slideHero,
                    new PauseTransition(Duration.millis(80)),
                    new ParallelTransition(
                            fadeCopy == null ? new PauseTransition(Duration.ZERO) : fadeCopy,
                            liftCopy == null ? new PauseTransition(Duration.ZERO) : liftCopy
                    ),
                    new PauseTransition(Duration.millis(80)),
                    new ParallelTransition(fadeCard, slideCard)
            );
            seq.play();
        });
    }
}
