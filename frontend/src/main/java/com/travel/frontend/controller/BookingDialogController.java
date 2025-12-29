/* Represents the pop-up booking flow: shows the selected package info, checks
   the user’s profile for eligibility, calculates pricing, and submits the
   booking request back to the server. Uses JavaFX dialogs, background threads,
   and the HTTP ApiClient to keep the flow smooth. */
package com.travel.frontend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.travel.frontend.net.ApiClient;
import com.travel.frontend.model.BookingResponse;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.geometry.Pos;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.scene.effect.GaussianBlur;
import javafx.util.Duration;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.travel.frontend.controller.HistoryController.markDirty;

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
    @FXML private StackPane paymentOverlay;
    @FXML private StackPane paymentModalHolder;
    @FXML private Region paymentBackdrop;
    @FXML private BorderPane cardRoot;

    private UUID packageId;
    private BigDecimal basePrice = BigDecimal.ZERO;
    private int minPersons = 1;
    private int maxPersons = 10;
    private final ApiClient api = ApiClient.get();
    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

    /* Static helper that loads the booking_dialog.fxml file, seeds the controller
       with the chosen package id + base price, and opens a modal Stage. */
    public static void open(UUID packageId, String packageName, String priceText, String groupSizeText) {
        open(packageId, packageName, parsePrice(priceText), groupSizeText);
    }

    public static void open(UUID packageId, String packageName, BigDecimal basePrice, String groupSizeText) {
        try {
            URL url = BookingDialogController.class.getResource("/fxml/booking_dialog.fxml");
            FXMLLoader loader = new FXMLLoader(url);
            Parent root = loader.load();
            BookingDialogController c = loader.getController();
            c.packageId = packageId;
            c.basePrice = basePrice == null ? BigDecimal.ZERO : basePrice;
            c.applyGroupBounds(groupSizeText);
            c.reseedPrice();
            c.loadBasePriceFromApiIfMissing();

            Stage s = new Stage();
            s.setTitle("Book: " + packageName);
            Scene scene = new Scene(root, 540, 360);
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
        personsSpinner.getValueFactory().setValue(minPersons);
        personsSpinner.valueProperty().addListener((obs, o, n) -> {
            refreshPersonCount();
            updatePrice();
        });
        // Seed UI with the starting count
        refreshPersonCount();
        updatePrice();
        applyButtonStyles();
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
                        String txn = "—";
                        try {
                            BookingResponse response = mapper.readValue(res.body(), BookingResponse.class);
                            markDirty();
                            if (response != null && response.transactionId != null) {
                                txn = response.transactionId;
                            }
                        } catch (Exception ex) {
                            // ignore parse issues, still show confirmation below
                        }
                        showTxnModal(txn);
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

    private void showTxnModal(String txn) {
        if (paymentOverlay == null || paymentModalHolder == null || cardRoot == null) {
            new Alert(Alert.AlertType.INFORMATION, "Booking confirmed!\nTransaction ID: " + txn, ButtonType.OK).showAndWait();
            close();
            return;
        }
        cardRoot.setDisable(true);
        cardRoot.setEffect(new GaussianBlur(8));
        paymentOverlay.setVisible(true);
        paymentOverlay.setPickOnBounds(true);

        VBox modal = new VBox(14);
        modal.getStyleClass().add("modal");

        StackPane header = new StackPane();
        header.getStyleClass().add("modal-header");
        VBox hText = new VBox(4);
        hText.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label("Payment Successful!");
        title.getStyleClass().add("modal-title");
        Label desc = new Label("Your transaction has been completed successfully");
        desc.getStyleClass().add("modal-description");
        hText.getChildren().addAll(title, desc);
        Button closeBtn = new Button("✕");
        closeBtn.getStyleClass().add("modal-close-btn");
        closeBtn.setOnAction(e -> closePaymentModal());
        StackPane.setAlignment(closeBtn, Pos.TOP_RIGHT);

        StackPane iconWrap = new StackPane();
        iconWrap.getStyleClass().add("success-icon-wrap");
        javafx.scene.shape.SVGPath icon = new javafx.scene.shape.SVGPath();
        icon.setContent("M10 18l-6-6 1.4-1.4L10 15.2 20.6 4.6 22 6z");
        icon.getStyleClass().add("success-icon");
        StackPane circle = new StackPane(icon);
        circle.getStyleClass().add("success-icon-circle");
        iconWrap.getChildren().add(circle);

        // Animate tick pop
        Timeline pop = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(icon.scaleXProperty(), 0),
                        new KeyValue(icon.scaleYProperty(), 0),
                        new KeyValue(icon.opacityProperty(), 0),
                        new KeyValue(circle.scaleXProperty(), 0),
                        new KeyValue(circle.scaleYProperty(), 0)
                ),
                new KeyFrame(Duration.millis(450),
                        new KeyValue(icon.scaleXProperty(), 1.6, Interpolator.EASE_OUT),
                        new KeyValue(icon.scaleYProperty(), 1.6, Interpolator.EASE_OUT),
                        new KeyValue(icon.opacityProperty(), 1, Interpolator.EASE_OUT),
                        new KeyValue(circle.scaleXProperty(), 1, Interpolator.EASE_OUT),
                        new KeyValue(circle.scaleYProperty(), 1, Interpolator.EASE_OUT)
                )
        );
        pop.play();

        header.getChildren().addAll(hText, closeBtn);
        VBox content = new VBox(12);
        content.getStyleClass().add("modal-content");

        VBox txBox = new VBox(8);
        txBox.getStyleClass().add("transaction-box");
        Label txLabel = new Label("Transaction ID");
        txLabel.getStyleClass().add("transaction-label");
        HBox txRow = new HBox(10);
        txRow.setAlignment(Pos.CENTER_LEFT);
        Label txValue = new Label(txn);
        txValue.getStyleClass().add("transaction-id");
        Button copy = new Button("Copy");
        copy.getStyleClass().add("copy-btn");
        copy.setOnAction(e -> {
            ClipboardContent cc = new ClipboardContent();
            cc.putString(txn);
            Clipboard.getSystemClipboard().setContent(cc);
        });
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        txRow.getChildren().addAll(txValue, spacer, copy);
        txBox.getChildren().addAll(txLabel, txRow);

        Button ok = new Button("OK");
        ok.getStyleClass().add("ok-btn");
        ok.setMaxWidth(Double.MAX_VALUE);
        ok.setOnAction(e -> { closePaymentModal(); close(); });
        closeBtn.setOnAction(e -> { closePaymentModal(); close(); });

        content.getChildren().addAll(iconWrap, txBox, ok);
        modal.getChildren().addAll(header, content);
        paymentModalHolder.getChildren().setAll(modal);
        modal.setOpacity(0);
        modal.setScaleX(0.9);
        modal.setScaleY(0.9);
        Timeline tl = new Timeline(
                new javafx.animation.KeyFrame(Duration.ZERO,
                        new javafx.animation.KeyValue(modal.opacityProperty(), 0),
                        new javafx.animation.KeyValue(modal.scaleXProperty(), 0.9),
                        new javafx.animation.KeyValue(modal.scaleYProperty(), 0.9)),
                new javafx.animation.KeyFrame(Duration.millis(240),
                        new javafx.animation.KeyValue(modal.opacityProperty(), 1, javafx.animation.Interpolator.EASE_OUT),
                        new javafx.animation.KeyValue(modal.scaleXProperty(), 1, javafx.animation.Interpolator.EASE_OUT),
                        new javafx.animation.KeyValue(modal.scaleYProperty(), 1, javafx.animation.Interpolator.EASE_OUT))
        );
        tl.play();
    }

    private static BigDecimal parsePrice(String text) {
        try {
            String digits = text.replaceAll("[^0-9.]", "");
            return new BigDecimal(digits);
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    private void reseedPrice() {
        if (personsSpinner != null && personsSpinner.getValueFactory() != null) {
            personsSpinner.getValueFactory().setValue(minPersons);
        }
        refreshPersonCount();
        updatePrice();
    }

    private void loadBasePriceFromApiIfMissing() {
        if (packageId == null) return;
        if (basePrice != null && basePrice.signum() > 0) return;
        new Thread(() -> {
            try {
                var res = api.rawGet("/packages/" + packageId, true);
                if (res.statusCode() != 200) return;
                PackagePrice payload = mapper.readValue(res.body(), PackagePrice.class);
                if (payload == null || payload.basePrice == null || payload.basePrice.signum() <= 0) return;
                basePrice = payload.basePrice;
                Platform.runLater(this::reseedPrice);
            } catch (Exception ignore) { }
        }).start();
    }

    private void applyGroupBounds(String groupSize) {
        if (groupSize == null || groupSize.isBlank()) return;
        Matcher m = Pattern.compile("(\\d+)").matcher(groupSize);
        int first = -1, second = -1;
        while (m.find()) {
            if (first == -1) first = Integer.parseInt(m.group(1));
            else { second = Integer.parseInt(m.group(1)); break; }
        }
        // Always allow starting at 1, but cap the max to the provided upper bound if present
        minPersons = 1;
        if (second > 0) {
            maxPersons = second;
        } else if (first > 0) {
            maxPersons = first;
        }
        if (maxPersons < minPersons) { maxPersons = minPersons; }
    }

    private void updatePrice() {
        int n = personsSpinner.getValue();
        BigDecimal unit = basePrice == null ? BigDecimal.ZERO : basePrice;
        priceLabel.setText("BDT " + unit.multiply(BigDecimal.valueOf(n)));
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

    private static class PackagePrice {
        public BigDecimal basePrice;
    }

    private void closePaymentModal() {
        if (paymentOverlay != null) paymentOverlay.setVisible(false);
        if (paymentModalHolder != null) paymentModalHolder.getChildren().clear();
        if (cardRoot != null) {
            cardRoot.setDisable(false);
            cardRoot.setEffect(null);
        }
    }
}
