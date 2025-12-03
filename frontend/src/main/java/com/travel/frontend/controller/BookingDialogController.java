/* Represents the pop-up booking flow: shows the selected package info, checks
   the user’s profile for eligibility, calculates pricing, and submits the
   booking request back to the server. Uses JavaFX dialogs, background threads,
   and the HTTP ApiClient to keep the flow smooth. */
package com.travel.frontend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BookingDialogController {
    @FXML private TextField usernameField;
    @FXML private TextField fullNameField;
    @FXML private TextField idNumberField;
    @FXML private Spinner<Integer> personsSpinner; // kept for value factory
    @FXML private Label personCountLabel;
    @FXML private Button upBtn;
    @FXML private Button downBtn;
    @FXML private TextField nameField;
    @FXML private TextField cardField;
    @FXML private TextField expiryField;
    @FXML private TextField cvvField;
    @FXML private Label priceLabel;
    @FXML private Label statusLabel;
    @FXML private Button payButton;
    @FXML private Label heroTitle;
    @FXML private Label heroSubtitle;

    private UUID packageId;
    private BigDecimal basePrice = BigDecimal.ZERO;
    private int minPersons = 1;
    private int maxPersons = 10;
    private final ApiClient api = ApiClient.get();
    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

    /* Static helper that loads the booking_dialog.fxml file, seeds the controller
       with the chosen package id + base price, and opens a modal Stage. */
    public static void open(UUID packageId, String packageName, String priceText, String groupSizeText) {
        try {
            URL url = BookingDialogController.class.getResource("/fxml/booking_dialog.fxml");
            FXMLLoader loader = new FXMLLoader(url);
            Parent root = loader.load();
            BookingDialogController c = loader.getController();
            c.packageId = packageId;
            c.basePrice = parsePrice(priceText);
            c.applyGroupBounds(groupSizeText);

            Stage s = new Stage();
            s.setTitle("Book: " + packageName);
            Scene scene = new Scene(root, 540, 360);
            URL css = BookingDialogController.class.getResource("/css/hotel_booking_dialog.css");
            if (css != null) {
                scene.getStylesheets().add(css.toExternalForm());
                System.out.println("[BookingDialog] Added stylesheet: " + css);
            } else {
                System.out.println("[BookingDialog] Stylesheet /css/hotel_booking_dialog.css not found on classpath");
            }
            System.out.println("[BookingDialog] Scene stylesheets: " + scene.getStylesheets());
            s.setScene(scene);
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
        personsSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(minPersons, maxPersons, minPersons));
        personsSpinner.valueProperty().addListener((obs, o, n) -> {
            refreshPersonCount();
            updatePrice();
        });
        applyButtonStyles();
        priceLabel.setText("BDT 0.00");
        if (heroTitle != null) heroTitle.setText("Complete Your Booking");
        if (heroSubtitle != null) heroSubtitle.setText("Fill in your details to confirm your reservation");
        // Prefill profile context for display and eligibility check
        new Thread(() -> {
            try {
                var p = api.getMyProfile();
                boolean eligible = p != null && p.idNumber != null && !p.idNumber.isBlank()
                        && p.idType != null && !p.idType.isBlank();
                Platform.runLater(() -> {
                    usernameField.setText(p.username);
                    fullNameField.setText(p.fullName == null ? "" : p.fullName);
                    idNumberField.setText(p.idNumber == null ? "" : p.idNumber);
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
        updatePrice();
    }

    /* Validates the number of travelers, crafts a booking JSON payload, and
       posts it via ApiClient.rawPostJson on a background Thread. */
    @FXML private void onConfirm() {
        int n = personsSpinner.getValue();
        if (n < minPersons || n > maxPersons) {
            showAlert("Please choose between " + minPersons + " and " + maxPersons + " persons.");
            return;
        }
        if (!validatePayment()) return;
        if (payButton != null) payButton.setDisable(true);
        if (statusLabel != null) statusLabel.setText("Processing...");

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
                    Platform.runLater(() -> {
                        showAlert("Booking failed: " + res.body());
                        if (payButton != null) payButton.setDisable(false);
                    });
                }
            } catch (Exception e) {
                Platform.runLater(() -> {
                    showAlert("Booking failed: " + e.getMessage());
                    if (payButton != null) payButton.setDisable(false);
                });
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

    private void applyGroupBounds(String groupSize) {
        if (groupSize == null || groupSize.isBlank()) return;
        Matcher m = Pattern.compile("(\\d+)").matcher(groupSize);
        int first = -1, second = -1;
        while (m.find()) {
            if (first == -1) first = Integer.parseInt(m.group(1));
            else { second = Integer.parseInt(m.group(1)); break; }
        }
        if (first > 0) minPersons = first;
        if (second > 0) maxPersons = second;
        if (maxPersons < minPersons) { maxPersons = minPersons; }
    }

    private void updatePrice() {
        int n = personsSpinner.getValue();
        priceLabel.setText("BDT " + basePrice.multiply(BigDecimal.valueOf(n)));
        refreshPersonCount();
    }

    private void refreshPersonCount() {
        if (personCountLabel != null) {
            personCountLabel.setText(String.valueOf(personsSpinner.getValue()));
        }
    }

    @FXML private void onIncrement() {
        int v = personsSpinner.getValue();
        if (v < maxPersons) personsSpinner.getValueFactory().setValue(v + 1);
    }

    @FXML private void onDecrement() {
        int v = personsSpinner.getValue();
        if (v > minPersons) personsSpinner.getValueFactory().setValue(v - 1);
    }

    private boolean validatePayment() {
        String name = nameField == null ? "" : nameField.getText().trim();
        String card = cardField == null ? "" : cardField.getText().replaceAll("\\s+", "");
        String expiry = expiryField == null ? "" : expiryField.getText().trim();
        String cvv = cvvField == null ? "" : cvvField.getText().trim();

        if (!name.matches("[A-Za-z]+(?: [A-Za-z]+)+")) {
            showAlert("Enter full cardholder name (letters only).");
            return false;
        }
        if (!card.matches("\\d{13,19}")) {
            showAlert("Enter a valid card number (13-19 digits).");
            return false;
        }
        if (!isFutureExpiry(expiry)) {
            showAlert("Enter a valid expiry in MM/YY or MM/YYYY.");
            return false;
        }
        if (!cvv.matches("\\d{3,4}")) {
            showAlert("Enter a valid CVV (3-4 digits).");
            return false;
        }
        return true;
    }

    private boolean isFutureExpiry(String mmYY) {
        String[] parts = mmYY.split("/");
        try {
            int m = Integer.parseInt(parts[0].trim());
            int yPart = Integer.parseInt(parts[1].trim());
            int y = (parts[1].trim().length() == 2) ? 2000 + yPart : yPart;
            if (m < 1 || m > 12) return false;
            java.time.YearMonth exp = java.time.YearMonth.of(y, m);
            return exp.isAfter(java.time.YearMonth.now());
        } catch (Exception e) {
            return false;
        }
    }

    private void applyButtonStyles() {
        if (payButton != null) {
            payButton.getStyleClass().clear();
            payButton.getStyleClass().addAll("pillBtn", "primary");
            payButton.setStyle("-fx-background-color: linear-gradient(to right, #5b21b6, #6d28d9);"
                    + "-fx-background-radius: 14; -fx-text-fill: white; -fx-font-weight: 800; -fx-padding: 16 20;");
            payButton.setOnMouseEntered(e -> { payButton.setScaleX(1.03); payButton.setScaleY(1.03); });
            payButton.setOnMouseExited(e -> { payButton.setScaleX(1.0); payButton.setScaleY(1.0); });
            payButton.setOnMousePressed(e -> { payButton.setScaleX(0.97); payButton.setScaleY(0.97); });
            payButton.setOnMouseReleased(e -> { payButton.setScaleX(1.03); payButton.setScaleY(1.03); });
        }
        if (upBtn != null) {
            upBtn.getStyleClass().addAll("arrowBtn", "upBtn");
            upBtn.setOnMouseEntered(e -> { upBtn.setScaleX(1.05); upBtn.setScaleY(1.05); });
            upBtn.setOnMouseExited(e -> { upBtn.setScaleX(1.0); upBtn.setScaleY(1.0); });
            upBtn.setOnMousePressed(e -> { upBtn.setScaleX(0.96); upBtn.setScaleY(0.96); });
            upBtn.setOnMouseReleased(e -> { upBtn.setScaleX(1.05); upBtn.setScaleY(1.05); });
        }
        if (downBtn != null) {
            downBtn.getStyleClass().addAll("arrowBtn", "downBtn");
            downBtn.setOnMouseEntered(e -> { downBtn.setScaleX(1.05); downBtn.setScaleY(1.05); });
            downBtn.setOnMouseExited(e -> { downBtn.setScaleX(1.0); downBtn.setScaleY(1.0); });
            downBtn.setOnMousePressed(e -> { downBtn.setScaleX(0.96); downBtn.setScaleY(0.96); });
            downBtn.setOnMouseReleased(e -> { downBtn.setScaleX(1.05); downBtn.setScaleY(1.05); });
        }
        System.out.println("[BookingDialog] payButton classes=" + (payButton != null ? payButton.getStyleClass() : "null"));
        System.out.println("[BookingDialog] upBtn classes=" + (upBtn != null ? upBtn.getStyleClass() : "null"));
        System.out.println("[BookingDialog] downBtn classes=" + (downBtn != null ? downBtn.getStyleClass() : "null"));
    }
}
