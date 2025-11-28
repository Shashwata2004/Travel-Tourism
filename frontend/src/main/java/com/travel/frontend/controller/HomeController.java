/* Drives the personal information page by loading the user’s profile, letting
   them update key fields, and offering shortcuts to other sections.
   Fetches data through ApiClient + DataCache on worker threads so the form
   feels snappy even while hitting the network. */
package com.travel.frontend.controller;

import com.travel.frontend.model.Profile;
import com.travel.frontend.net.ApiClient;
import com.travel.frontend.ui.Navigator;
import com.travel.frontend.cache.DataCache;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.animation.TranslateTransition;
import javafx.animation.Interpolator;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

public class HomeController {
    @FXML private NavbarController navbarController;
    @FXML private TextField emailField;
    @FXML private TextField usernameField;
    @FXML private TextField fullNameField;
    @FXML private TextField idNumberField;
    @FXML private TextField locationField;

    @FXML private RadioButton idNid;
    @FXML private RadioButton idBirth;
    @FXML private RadioButton idPassport;

    @FXML private RadioButton genderMale;
    @FXML private RadioButton genderFemale;

    @FXML private Label statusLabel;
    @FXML private Pane blobLayer;
    @FXML private Circle blobA;
    @FXML private Circle blobB;
    @FXML private Circle blobC;

    private final ApiClient api = ApiClient.get();
    private Profile profile;
    private static final String CACHE_VERSION = "v2";

    /* Runs once the FXML is ready: loads cached profile (or calls the API) on
       a background Thread and fills the form via Platform.runLater. */
    @FXML
    private void initialize() {
        if (navbarController != null) navbarController.setActive(NavbarController.ActivePage.PERSONAL);
        statusLabel.setText("Loading profile...");
        animateBlobs();
        new Thread(() -> {
            try {
                Profile p = com.travel.frontend.cache.DataCache.getOrLoad("myProfile:" + CACHE_VERSION, api::getMyProfile);
                this.profile = p;
                Platform.runLater(() -> fillForm(p));
            } catch (Exception e) {
                Platform.runLater(() -> statusLabel.setText(e.getMessage()));
            }
        }).start();

        
    }

    /* Copies profile data into the form controls. Handles radio button mapping
       so enum strings like “PASSPORT” turn into the right toggle selections. */
    private void fillForm(Profile p) {
        emailField.setText(p.email);
        usernameField.setText(p.username);
        fullNameField.setText(nullToEmpty(p.fullName));
        idNumberField.setText(nullToEmpty(p.idNumber));
        locationField.setText(nullToEmpty(p.location));

        if (p.idType != null) {
            switch (p.idType) {
                case "NID": idNid.setSelected(true); break;
                case "BIRTH_CERTIFICATE": idBirth.setSelected(true); break;
                case "PASSPORT": idPassport.setSelected(true); break;
                default: break;
            }
        }
        if (p.gender != null) {
            switch (p.gender) {
                case "MALE": genderMale.setSelected(true); break;
                case "FEMALE": genderFemale.setSelected(true); break;
                default: break;
            }
        }
        statusLabel.setText("");
    }

    /* Sends updated personal info to the server using ApiClient.updateMyProfile
       and stores the response back in DataCache so other screens stay in sync. */
    @FXML
    private void onUpdate() {
        statusLabel.setText("Updating...");
        Profile upd = new Profile();
        upd.username = usernameField.getText();
        upd.location = locationField.getText();
        upd.fullName = emptyToNull(fullNameField.getText());
        upd.idNumber = emptyToNull(idNumberField.getText());
        upd.idType = idNid.isSelected() ? "NID" : (idBirth.isSelected() ? "BIRTH_CERTIFICATE" : (idPassport.isSelected() ? "PASSPORT" : null));
        upd.gender = genderMale.isSelected() ? "MALE" : (genderFemale.isSelected() ? "FEMALE" : null);

        new Thread(() -> {
            try {
                Profile newP = api.updateMyProfile(upd);
                this.profile = newP;
                DataCache.put("myProfile:" + CACHE_VERSION, newP);
                Platform.runLater(() -> {
                    fillForm(newP);
                    statusLabel.setText("✅ Updated");
                });
            } catch (ApiClient.ApiException e) {
                Platform.runLater(() -> statusLabel.setText(e.getMessage()));
            }
        }).start();
    }

    /* Navbar logout action: clears cached data and returns to the login screen. */
    @FXML
    private void onLogout() {
        com.travel.frontend.cache.DataCache.clear();
        Navigator.goLogin();
    }

    // Navbar actions
    @FXML private void goPackages() { Navigator.goPackages(); }
    @FXML private void goDestinations() { Navigator.goDestinations(); }
    @FXML private void goHistory() { /* placeholder */ }
    @FXML private void goAbout() { /* placeholder */ }

    private static String nullToEmpty(String s) { return s == null ? "" : s; }
    private static String emptyToNull(String s) { return s == null || s.isBlank() ? null : s; }

    private void animateBlobs() {
        if (blobLayer == null) return;
        if (blobA != null) {
            TranslateTransition tt = new TranslateTransition(Duration.seconds(18), blobA);
            tt.setFromX(0); tt.setToX(80);
            tt.setFromY(0); tt.setToY(40);
            tt.setAutoReverse(true);
            tt.setCycleCount(TranslateTransition.INDEFINITE);
            tt.setInterpolator(Interpolator.EASE_BOTH);
            tt.play();
        }
        if (blobB != null) {
            TranslateTransition tt = new TranslateTransition(Duration.seconds(16), blobB);
            tt.setFromX(0); tt.setToX(-80);
            tt.setFromY(0); tt.setToY(-50);
            tt.setAutoReverse(true);
            tt.setCycleCount(TranslateTransition.INDEFINITE);
            tt.setInterpolator(Interpolator.EASE_BOTH);
            tt.play();
        }
        if (blobC != null) {
            TranslateTransition tt = new TranslateTransition(Duration.seconds(20), blobC);
            tt.setFromX(0); tt.setToX(90);
            tt.setFromY(0); tt.setToY(30);
            tt.setAutoReverse(true);
            tt.setCycleCount(TranslateTransition.INDEFINITE);
            tt.setInterpolator(Interpolator.EASE_BOTH);
            tt.play();
        }
    }
}
