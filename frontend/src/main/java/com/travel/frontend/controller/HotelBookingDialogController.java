package com.travel.frontend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.travel.frontend.net.ApiClient;
import com.travel.frontend.model.RoomBookingResponse;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.effect.GaussianBlur;
import javafx.stage.Modality;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.geometry.Pos;

import java.math.BigDecimal;
import java.net.URL;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.travel.frontend.controller.HistoryController.markDirty;
public class HotelBookingDialogController {
    @FXML private Label checkInLabel;
    @FXML private Label checkOutLabel;
    @FXML private Label guestsLabel;
    @FXML private Label totalPriceLabel;
    @FXML private TextField nameField;
    @FXML private TextField cardField;
    @FXML private TextField expiryField;
    @FXML private TextField cvvField;
    @FXML private StackPane paymentOverlay;
    @FXML private StackPane paymentModalHolder;
    @FXML private Region paymentBackdrop;
    @FXML private BorderPane cardRoot;
    @FXML private Button payButton;
    @FXML private Label statusLabel;

    private final ApiClient api = ApiClient.get();
    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private UUID hotelId;
    private LocalDate checkIn;
    private LocalDate checkOut;
    private int guests;
    private BigDecimal totalPrice = BigDecimal.ZERO;
    private List<RoomSelection> selections = new ArrayList<>();
    private Runnable onSuccess;
    private com.travel.frontend.model.Profile profile;

    public record RoomSelection(UUID roomId, String name, int count, BigDecimal totalPrice) {}

    public static void open(UUID hotelId,
                            LocalDate checkIn,
                            LocalDate checkOut,
                            int guests,
                            BigDecimal totalPrice,
                            List<RoomSelection> selections,
                            Runnable onSuccess) {
        try {
            URL url = HotelBookingDialogController.class.getResource("/fxml/hotel_booking_dialog.fxml");
            FXMLLoader loader = new FXMLLoader(url);
            Parent root = loader.load();
            HotelBookingDialogController c = loader.getController();
            c.seed(hotelId, checkIn, checkOut, guests, totalPrice, selections, onSuccess);

            Stage stage = new Stage();
            stage.setTitle("Confirm your booking");
            double screenW = Screen.getPrimary().getBounds().getWidth();
            double screenH = Screen.getPrimary().getBounds().getHeight();
            stage.setScene(new Scene(root, Math.max(420, screenW * 0.3), Math.max(540, screenH * 0.9)));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.show();
        } catch (Exception e) {
            throw new RuntimeException("Unable to open booking dialog: " + e.getMessage(), e);
        }
    }

    private void seed(UUID hotelId,
                      LocalDate checkIn,
                      LocalDate checkOut,
                      int guests,
                      BigDecimal totalPrice,
                      List<RoomSelection> selections,
                      Runnable onSuccess) {
        this.hotelId = hotelId;
        this.checkIn = checkIn;
        this.checkOut = checkOut;
        this.guests = guests;
        this.totalPrice = totalPrice == null ? BigDecimal.ZERO : totalPrice;
        this.selections = selections == null ? List.of() : selections;
        this.onSuccess = onSuccess;

        checkInLabel.setText(checkIn == null ? "-" : checkIn.toString());
        checkOutLabel.setText(checkOut == null ? "-" : checkOut.toString());
        guestsLabel.setText(guests + (guests == 1 ? " adult" : " guests"));
        totalPriceLabel.setText("BDT " + this.totalPrice);
        if (payButton != null) {
            payButton.setText("Pay BDT " + this.totalPrice);
        }
    }

    @FXML
    private void initialize() {
        if (statusLabel != null) statusLabel.setText("");
        if (payButton != null) payButton.setDisable(false);
        if (payButton != null) {
            payButton.setOnMouseEntered(e -> {
                payButton.setScaleX(1.03);
                payButton.setScaleY(1.03);
            });
            payButton.setOnMouseExited(e -> {
                payButton.setScaleX(1.0);
                payButton.setScaleY(1.0);
            });
            payButton.setOnMousePressed(e -> {
                payButton.setScaleX(0.98);
                payButton.setScaleY(0.98);
            });
            payButton.setOnMouseReleased(e -> {
                payButton.setScaleX(1.03);
                payButton.setScaleY(1.03);
            });
        }
    }

    @FXML
    private void onClose() {
        Stage s = (Stage) checkInLabel.getScene().getWindow();
        s.close();
    }

    @FXML
    private void onPay() {
        if (checkIn == null || checkOut == null) {
            showStatus("Select dates first.", true);
            return;
        }
        if (selections == null || selections.isEmpty()) {
            showStatus("No rooms selected.", true);
            return;
        }
        if (!validatePayment()) {
            return;
        }

        payButton.setDisable(true);
        showStatus("Processing payment...", false);
        PauseTransition wait = new PauseTransition(Duration.seconds(2));
        wait.setOnFinished(e -> submitBookings());
        wait.play();
    }

    private boolean validatePayment() {
        clearFieldStyles();
        String name = nameField.getText() == null ? "" : nameField.getText().trim();
        String card = cardField.getText() == null ? "" : cardField.getText().replaceAll("\\s+", "");
        String expiry = expiryField.getText() == null ? "" : expiryField.getText().trim();
        String cvv = cvvField.getText() == null ? "" : cvvField.getText().trim();

        if (!name.matches("[A-Za-z]+(?: [A-Za-z]+)+")) {
            markError(nameField);
            showStatus("Enter full name (letters only).", true);
            return false;
        }
        if (!card.matches("\\d{13,19}")) {
            markError(cardField);
            showStatus("Enter a valid card number (13-19 digits).", true);
            return false;
        }
        if (!expiry.matches("\\d{2}/\\d{2}") && !expiry.matches("\\d{2}/\\d{4}") || !isFutureExpiry(expiry)) {
            markError(expiryField);
            showStatus("Enter a valid expiry in MM/YY or MM/YYYY.", true);
            return false;
        }
        if (!cvv.matches("\\d{3,4}")) {
            markError(cvvField);
            showStatus("Enter a valid CVV (3-4 digits).", true);
            return false;
        }
        return true;
    }

    private boolean luhn(String digits) {
        int sum = 0;
        boolean alt = false;
        for (int i = digits.length() - 1; i >= 0; i--) {
            int n = digits.charAt(i) - '0';
            if (alt) {
                n *= 2;
                if (n > 9) n -= 9;
            }
            sum += n;
            alt = !alt;
        }
        return sum % 10 == 0;
    }

    private boolean isFutureExpiry(String mmYY) {
        String[] parts = mmYY.split("/");
        try {
            int m = Integer.parseInt(parts[0]);
            int yPart = Integer.parseInt(parts[1]);
            int y = (parts[1].length() == 2) ? 2000 + yPart : yPart;
            if (m < 1 || m > 12) return false;
            java.time.YearMonth exp = java.time.YearMonth.of(y, m);
            return exp.isAfter(java.time.YearMonth.now());
        } catch (Exception e) {
            return false;
        }
    }

    private void markError(TextField field) {
        if (field != null) field.getStyleClass().add("errorField");
    }

    private void clearFieldStyles() {
        if (nameField != null) nameField.getStyleClass().remove("errorField");
        if (cardField != null) cardField.getStyleClass().remove("errorField");
        if (expiryField != null) expiryField.getStyleClass().remove("errorField");
        if (cvvField != null) cvvField.getStyleClass().remove("errorField");
    }

    private void submitBookings() {
        com.travel.frontend.model.Profile prof = profile;
        if (prof == null) {
            try {
                prof = com.travel.frontend.cache.DataCache.peek("myProfile:v2");
                if (prof == null) {
                    prof = api.getMyProfile();
                    com.travel.frontend.cache.DataCache.put("myProfile:v2", prof);
                }
                profile = prof;
            } catch (Exception ignore) { }
        }
        final com.travel.frontend.model.Profile finalProf = prof;
        new Thread(() -> {
            try {
                String lastTxn = null;
                for (RoomSelection sel : selections) {
                    Map<String, Object> body = new HashMap<>();
                    body.put("checkIn", checkIn.toString());
                    body.put("checkOut", checkOut.toString());
                    body.put("rooms", sel.count());
                    body.put("totalGuests", guests);
                    body.put("totalPrice", sel.totalPrice());
                    if (finalProf != null) {
                        body.put("customerName", finalProf.fullName);
                        body.put("idType", finalProf.idType);
                        body.put("idNumber", finalProf.idNumber);
                        if (finalProf.userId != null && !finalProf.userId.isBlank()) {
                            body.put("userId", finalProf.userId);
                        }
                    }
                    String json = mapper.writeValueAsString(body);
                    var res = api.rawPostJson("/hotels/" + hotelId + "/rooms/" + sel.roomId() + "/book", json, true);
                    if (res.statusCode() != 200) {
                        throw new RuntimeException("Booking failed: " + res.body());
                    } else {
                        try {
                            RoomBookingResponse r = mapper.readValue(res.body(), RoomBookingResponse.class);
                            if (r != null && r.transactionId != null) lastTxn = r.transactionId;
                        } catch (Exception ignored) {}
                    }
                }
                String finalLastTxn = lastTxn;
                Platform.runLater(() -> {
                    String msg = (finalLastTxn != null)
                            ? "Payment successful! Transaction ID: " + finalLastTxn
                            : "Payment successful! Booking stored.";
                    showStatus(msg, false);
                    markDirty();
                    if (onSuccess != null) {
                        onSuccess.run();
                    }
                    showTxnModal(finalLastTxn == null ? "—" : finalLastTxn);
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    showStatus("Booking failed: " + ex.getMessage(), true);
                    payButton.setDisable(false);
                });
            }
        }).start();
    }

    private void showStatus(String msg, boolean error) {
        if (statusLabel == null) return;
        statusLabel.setText(msg);
        statusLabel.setStyle(error ? "-fx-text-fill:#dc2626;" : "-fx-text-fill:#10b981;");
    }

    private void showTxnModal(String txn) {
        if (paymentOverlay == null || paymentModalHolder == null || cardRoot == null) {
            new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION, "Booking confirmed!\nTransaction ID: " + txn, javafx.scene.control.ButtonType.OK).showAndWait();
            onClose();
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
        closeBtn.setOnAction(e -> { closePaymentModal(); onClose(); });
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
        ok.setOnAction(e -> { closePaymentModal(); onClose(); });

        content.getChildren().addAll(iconWrap, txBox, ok);
        modal.getChildren().addAll(header, content);
        paymentModalHolder.getChildren().setAll(modal);
        modal.setOpacity(0);
        modal.setScaleX(0.9);
        modal.setScaleY(0.9);
        Timeline tl = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(modal.opacityProperty(), 0),
                        new KeyValue(modal.scaleXProperty(), 0.9),
                        new KeyValue(modal.scaleYProperty(), 0.9)),
                new KeyFrame(Duration.millis(240),
                        new KeyValue(modal.opacityProperty(), 1, Interpolator.EASE_OUT),
                        new KeyValue(modal.scaleXProperty(), 1, Interpolator.EASE_OUT),
                        new KeyValue(modal.scaleYProperty(), 1, Interpolator.EASE_OUT))
        );
        tl.play();
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
