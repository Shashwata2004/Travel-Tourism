package com.travel.frontend.controller;

import com.travel.frontend.model.Profile;
import com.travel.frontend.net.ApiClient;
import com.travel.frontend.ui.Navigator;
import com.travel.frontend.cache.DataCache;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;

public class HomeController {
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

    private final ApiClient api = ApiClient.get();
    private Profile profile;

    @FXML
    private void initialize() {
        statusLabel.setText("Loading profile...");
        new Thread(() -> {
            try {
                Profile p = com.travel.frontend.cache.DataCache.getOrLoad("myProfile", api::getMyProfile);
                this.profile = p;
                Platform.runLater(() -> fillForm(p));
            } catch (Exception e) {
                Platform.runLater(() -> statusLabel.setText(e.getMessage()));
            }
        }).start();
    }

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
                DataCache.put("myProfile", newP);
                Platform.runLater(() -> {
                    fillForm(newP);
                    statusLabel.setText("✅ Updated");
                });
            } catch (ApiClient.ApiException e) {
                Platform.runLater(() -> statusLabel.setText(e.getMessage()));
            }
        }).start();
    }

    @FXML
    private void onLogout() {
        com.travel.frontend.cache.DataCache.clear();
        Navigator.goLogin();
    }

    // Navbar actions
    @FXML private void goPackages() { Navigator.goPackages(); }
    @FXML private void goDestinations() { /* placeholder */ }
    @FXML private void goHistory() { /* placeholder */ }
    @FXML private void goAbout() { /* placeholder */ }

    private static String nullToEmpty(String s) { return s == null ? "" : s; }
    private static String emptyToNull(String s) { return s == null || s.isBlank() ? null : s; }
}

