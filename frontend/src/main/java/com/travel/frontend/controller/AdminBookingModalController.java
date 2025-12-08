package com.travel.frontend.controller;

import com.travel.frontend.model.PackageBookingAdminView;
import com.travel.frontend.model.RoomBookingAdminView;
import com.travel.frontend.net.ApiClient;
import com.travel.frontend.controller.AdminCancelSuccessController;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.geometry.Pos;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class AdminBookingModalController {
    @FXML private Label titleLabel;
    @FXML private Label subtitleLabel;

    @FXML private Label customerNameLabel;
    @FXML private Label emailLabel;
    @FXML private Label idTypeLabel;
    @FXML private Label idNumberLabel;

    @FXML private Label primaryLabel1;
    @FXML private Label primaryValue1;
    @FXML private Label primaryLabel2;
    @FXML private Label primaryValue2;
    @FXML private Label primaryLabel3;
    @FXML private Label primaryValue3;
    @FXML private Label primaryLabel4;
    @FXML private Label primaryValue4;
    @FXML private Label bookingIdLabel;

    @FXML private Label txnLabel;
    @FXML private Label bookedAtLabel;
    @FXML private Label canceledAtLabel;
    @FXML private Label canceledByLabel;

    @FXML private Button headerCloseButton;
    @FXML private Button cancelActionButton;
    @FXML private Button closeButton;
    @FXML private VBox alertBox;

    private static final DateTimeFormatter DF = DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a", Locale.ENGLISH);
    private static final DateTimeFormatter DF_DATE = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH);

    private Pane host;
    private StackPane overlay;
    private PackageBookingAdminView packageView;
    private RoomBookingAdminView roomView;
    private final ApiClient api = ApiClient.get();

    public static void showPackage(Pane host, PackageBookingAdminView v) {
        try {
            FXMLLoader loader = new FXMLLoader(AdminBookingModalController.class.getResource("/fxml/admin_booking_modal.fxml"));
            Parent modal = loader.load();
            AdminBookingModalController c = loader.getController();
            c.host = host;
            c.packageView = v;
            c.populatePackage(v);
            c.showOverlay(modal);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void showRoom(Pane host, RoomBookingAdminView v) {
        try {
            FXMLLoader loader = new FXMLLoader(AdminBookingModalController.class.getResource("/fxml/admin_booking_modal.fxml"));
            Parent modal = loader.load();
            AdminBookingModalController c = loader.getController();
            c.host = host;
            c.roomView = v;
            c.populateRoom(v);
            c.showOverlay(modal);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void showOverlay(Parent modal) {
        if (host == null) throw new IllegalStateException("Host pane is required for modal overlay");
        overlay = (StackPane) modal; // modal root is the overlay defined in FXML
        overlay.setPickOnBounds(true);
        if (host.getChildren().contains(overlay)) return;
        host.getChildren().add(overlay);
    }

    private void populatePackage(PackageBookingAdminView v) {
        if (titleLabel != null) titleLabel.setText("Booking Details");
        if (subtitleLabel != null) subtitleLabel.setText("Complete booking information");
        if (customerNameLabel != null) customerNameLabel.setText(ns(v.customerName));
        if (emailLabel != null) emailLabel.setText(ns(v.userEmail));
        if (idTypeLabel != null) idTypeLabel.setText(ns(v.idType));
        if (idNumberLabel != null) idNumberLabel.setText(ns(v.idNumber));

        setPrimary("Package", ns(v.packageName),
                "Persons", String.valueOf(v.totalPersons),
                "Booked At", formatInstant(v.createdAt),
                "Status", ns(v.status));
        if (bookingIdLabel != null) bookingIdLabel.setText(v.id == null ? "—" : v.id.toString());

        if (txnLabel != null) txnLabel.setText(ns(v.transactionId));
        if (bookedAtLabel != null) bookedAtLabel.setText(formatInstant(v.createdAt));
        boolean isCanceled = isCanceled(v.status, v.canceledAt);
        if (canceledAtLabel != null) canceledAtLabel.setText(isCanceled ? formatInstant(v.canceledAt) : "—");
        if (canceledByLabel != null) canceledByLabel.setText(normalizeCanceledBy(v.canceledBy, isCanceled));

        toggleAlert(isCanceled, v.canceledAt);
        applyStatusText(isCanceled);

        boolean past = v.bookingDeadline != null && !v.bookingDeadline.isAfter(LocalDate.now());
        if (cancelActionButton != null) {
            cancelActionButton.setDisable(isCanceled || v.id == null || past);
            cancelActionButton.setText(isCanceled ? "Canceled" : (past ? "Past booking" : "Cancel Booking"));
            cancelActionButton.setOnAction(e -> triggerCancelPackage());
        }
        wireCloseButtons();
    }

    private void populateRoom(RoomBookingAdminView v) {
        if (titleLabel != null) titleLabel.setText("Booking Details");
        if (subtitleLabel != null) subtitleLabel.setText("Complete booking information");
        if (customerNameLabel != null) customerNameLabel.setText(ns(v.customerName));
        if (emailLabel != null) emailLabel.setText(ns(v.userEmail));
        if (idTypeLabel != null) idTypeLabel.setText(ns(v.idType));
        if (idNumberLabel != null) idNumberLabel.setText(ns(v.idNumber));

        setPrimary("Destination", ns(v.destinationName),
                "Hotel", ns(v.hotelName),
                "Room / Persons", ns(v.roomName) + " · " + (v.totalGuests == null ? "-" : v.totalGuests) + " guests",
                "Check-in / out", fmtDate(v.checkIn) + " - " + fmtDate(v.checkOut));
        if (bookingIdLabel != null) bookingIdLabel.setText(v.id == null ? "—" : v.id.toString());

        if (txnLabel != null) txnLabel.setText(ns(v.transactionId));
        if (bookedAtLabel != null) bookedAtLabel.setText(formatInstant(v.createdAt));
        boolean isCanceled = isCanceled(v.status, v.canceledAt);
        if (canceledAtLabel != null) canceledAtLabel.setText(isCanceled ? formatInstant(v.canceledAt) : "—");
        if (canceledByLabel != null) canceledByLabel.setText(normalizeCanceledBy(v.canceledBy, isCanceled));

        toggleAlert(isCanceled, v.canceledAt);
        applyStatusText(isCanceled);

        boolean past = v.checkIn != null && !v.checkIn.isAfter(LocalDate.now());
        if (cancelActionButton != null) {
            cancelActionButton.setDisable(isCanceled || v.id == null || past);
            cancelActionButton.setText(isCanceled ? "Canceled" : (past ? "Past booking" : "Cancel Booking"));
            cancelActionButton.setOnAction(e -> triggerCancelRoom());
        }
        wireCloseButtons();
    }

    private void setPrimary(String l1, String v1, String l2, String v2, String l3, String v3, String l4, String v4) {
        if (primaryLabel1 != null) primaryLabel1.setText(l1);
        if (primaryValue1 != null) primaryValue1.setText(v1);
        if (primaryLabel2 != null) primaryLabel2.setText(l2);
        if (primaryValue2 != null) primaryValue2.setText(v2);
        if (primaryLabel3 != null) primaryLabel3.setText(l3);
        if (primaryValue3 != null) primaryValue3.setText(v3);
        if (primaryLabel4 != null) primaryLabel4.setText(l4);
        if (primaryValue4 != null) primaryValue4.setText(v4);
    }

    private String formatInstant(Instant i) {
        if (i == null) return "—";
        return DF.format(i.atZone(ZoneId.systemDefault()));
    }

    private boolean isCanceled(String status, Instant canceledAt) {
        if (canceledAt != null) return true;
        if (status == null) return false;
        String s = status.trim().toLowerCase(Locale.ROOT);
        return s.equals("canceled") || s.equals("cancelled") || s.contains("canceled");
    }

    private String ns(String s) { return s == null ? "—" : s; }
    private String normalizeCanceledBy(String s, boolean isCanceled) {
        if (!isCanceled) return "—";
        if (s == null || s.isBlank()) return "USER";
        return s;
    }

    private String fmtDate(java.time.LocalDate d) {
        return d == null ? "—" : d.format(DF_DATE);
    }

    private void toggleAlert(boolean canceled, Instant canceledAt) {
        if (alertBox != null) {
            alertBox.setVisible(canceled);
            alertBox.setManaged(canceled);
            if (canceled && alertBox.getChildren().size() >= 2) {
                if (alertBox.getChildren().get(1) instanceof Label l) {
                    l.setText("This booking has been canceled on " + formatInstant(canceledAt));
                }
            }
        }
    }

    private void applyStatusText(boolean canceled) {
        String canceledCls = "canceled-text";
        String activeCls = "active-text";
        for (Label l : new Label[]{customerNameLabel, emailLabel, idTypeLabel, idNumberLabel,
                primaryValue1, primaryValue2, primaryValue3, primaryValue4,
                txnLabel, bookedAtLabel, canceledAtLabel, canceledByLabel}) {
            if (l == null) continue;
            l.getStyleClass().remove(canceledCls);
            l.getStyleClass().remove(activeCls);
            l.getStyleClass().add(canceled ? canceledCls : activeCls);
        }
    }

    private void wireCloseButtons() {
        if (closeButton != null) closeButton.setOnAction(e -> close());
        if (headerCloseButton != null) headerCloseButton.setOnAction(e -> close());
    }

    private void triggerCancelPackage() {
        if (packageView == null || packageView.id == null) return;
        if (cancelActionButton != null) {
            cancelActionButton.setDisable(true);
            cancelActionButton.setText("Canceling...");
        }
        new Thread(() -> {
            try {
                var res = api.adminCancelPackageBooking(packageView.id);
                packageView.status = res.status;
                packageView.canceledAt = res.canceledAt;
                packageView.canceledBy = res.canceledBy;
                Platform.runLater(() -> {
                    populatePackage(packageView);
                    AdminCancelSuccessController.show(host, AdminCancelSuccessController.Mode.PACKAGE, packageView, null);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    if (cancelActionButton != null) {
                        cancelActionButton.setDisable(false);
                        cancelActionButton.setText("Cancel Booking");
                    }
                    showError(e.getMessage());
                });
            }
        }).start();
    }

    private void triggerCancelRoom() {
        if (roomView == null || roomView.id == null) return;
        if (cancelActionButton != null) {
            cancelActionButton.setDisable(true);
            cancelActionButton.setText("Canceling...");
        }
        new Thread(() -> {
            try {
                var res = api.adminCancelRoomBooking(roomView.id);
                roomView.status = res.status;
                roomView.canceledAt = res.canceledAt;
                roomView.canceledBy = res.canceledBy;
                Platform.runLater(() -> {
                    populateRoom(roomView);
                    AdminCancelSuccessController.show(host, AdminCancelSuccessController.Mode.ROOM, null, roomView);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    if (cancelActionButton != null) {
                        cancelActionButton.setDisable(false);
                        cancelActionButton.setText("Cancel Booking");
                    }
                    showError(e.getMessage());
                });
            }
        }).start();
    }

    private void showError(String msg) {
        javafx.scene.control.Alert a = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
        a.setHeaderText("Cancel failed");
        a.setContentText(msg);
        a.showAndWait();
    }

    private void close() {
        if (host != null && overlay != null) {
            host.getChildren().remove(overlay);
        }
    }
}
