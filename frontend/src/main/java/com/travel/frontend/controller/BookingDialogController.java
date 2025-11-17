/* Represents the pop-up booking flow: shows the selected package info, checks
   the user’s profile for eligibility, calculates pricing, and submits the
   booking request back to the server. Uses JavaFX dialogs, background threads,
   and the HTTP ApiClient to keep the flow smooth. */
package com.travel.frontend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.travel.frontend.net.ApiClient;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BookingDialogController {
    @FXML private Label usernameLabel;
    @FXML private Label nameLabel;
    @FXML private Label idNumberLabel;
    @FXML private Spinner<Integer> personsSpinner;
    @FXML private Label priceLabel;

    private UUID packageId;
    private BigDecimal basePrice = BigDecimal.ZERO;
    private final ApiClient api = ApiClient.get();
    private final ObjectMapper mapper = new ObjectMapper();

    /* Static helper that loads the booking_dialog.fxml file, seeds the controller
       with the chosen package id + base price, and opens a modal Stage. */
    public static void open(UUID packageId, String packageName, String priceText) {
        try {
            URL url = BookingDialogController.class.getResource("/fxml/booking_dialog.fxml");
            FXMLLoader loader = new FXMLLoader(url);
            Parent root = loader.load();
            BookingDialogController c = loader.getController();
            c.packageId = packageId;
            c.basePrice = parsePrice(priceText);

            Stage s = new Stage();
            s.setTitle("Book: " + packageName);
            s.setScene(new Scene(root, 540, 360));
            s.initModality(Modality.APPLICATION_MODAL);
            s.show();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /* Sets up the spinner defaults, resets price text, then fetches the user’s
       profile on a worker Thread to ensure they filled in ID information first. */
    @FXML
    private void initialize() {
        personsSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 100, 0));
        priceLabel.setText("BDT 0.00");
        // Prefill profile context for display and eligibility check
        new Thread(() -> {
            try {
                var p = api.getMyProfile();
                boolean eligible = p != null && p.idNumber != null && !p.idNumber.isBlank()
                        && p.idType != null && !p.idType.isBlank();
                Platform.runLater(() -> {
                    usernameLabel.setText(p.username);
                    nameLabel.setText(p.fullName == null ? "" : p.fullName);
                    idNumberLabel.setText(p.idNumber == null ? "" : p.idNumber);
                    if (!eligible) {
                        showAlert("You must complete Personal Information (ID Type and ID Number) before booking.");
                        close();
                    }
                });
            } catch (Exception ignore) { }
        }).start();
    }

    /* Reads the spinner count and multiplies it with the base price using
       BigDecimal so money math stays accurate. */
    @FXML private void onViewPrice() {
        int n = personsSpinner.getValue();
        priceLabel.setText("BDT " + basePrice.multiply(BigDecimal.valueOf(n)));
    }

    /* Validates the number of travelers, crafts a booking JSON payload, and
       posts it via ApiClient.rawPostJson on a background Thread. */
    @FXML private void onConfirm() {
        int n = personsSpinner.getValue();
        if (n <= 0) { showAlert("Please select at least 1 person."); return; }

        new Thread(() -> {
            try {
                Map<String, Object> body = new HashMap<>();
                body.put("packageId", packageId.toString());
                body.put("totalPersons", n);
                var res = api.rawPostJson("/bookings", mapper.writeValueAsString(body), true);
                if (res.statusCode() == 200) {
                    Platform.runLater(() -> {
                        showAlert("Booking confirmed! Check history later.");
                        close();
                    });
                } else {
                    Platform.runLater(() -> showAlert("Booking failed: " + res.body()));
                }
            } catch (Exception e) {
                Platform.runLater(() -> showAlert("Booking failed: " + e.getMessage()));
            }
        }).start();
    }

    @FXML private void onCancel() { close(); }

    private void close() { ((Stage) personsSpinner.getScene().getWindow()).close(); }

    private void showAlert(String m) {
        new Alert(Alert.AlertType.INFORMATION, m, ButtonType.OK).showAndWait();
    }

    private static BigDecimal parsePrice(String text) {
        try {
            String digits = text.replaceAll("[^0-9.]", "");
            return new BigDecimal(digits);
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }
}
