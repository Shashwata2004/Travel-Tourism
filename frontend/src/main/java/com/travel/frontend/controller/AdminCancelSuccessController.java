package com.travel.frontend.controller;

import com.travel.frontend.model.PackageBookingAdminView;
import com.travel.frontend.model.RoomBookingAdminView;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class AdminCancelSuccessController {
    public enum Mode { PACKAGE, ROOM }

    @FXML private Label titleLabel;
    @FXML private Label subtitleLabel;
    @FXML private Label customerLabel;
    @FXML private Label itemLabel;
    @FXML private Label itemValue;
    @FXML private Label dateLabel;
    @FXML private Label dateValue;
    @FXML private Label txnValue;
    @FXML private Label refundLabel;
    @FXML private Button closeButton;
    @FXML private Button doneButton;
    @FXML private Button viewAllButton;

    private Pane host;
    private StackPane overlay;

    private static final DateTimeFormatter DF_DATE = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter DF_DATETIME = DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a", Locale.ENGLISH);

    public static void show(Pane host, Mode mode, PackageBookingAdminView pkg, RoomBookingAdminView room) {
        try {
            FXMLLoader loader = new FXMLLoader(AdminCancelSuccessController.class.getResource("/fxml/admin_cancel_success.fxml"));
            Parent modal = loader.load();
            AdminCancelSuccessController c = loader.getController();
            c.host = host;
            c.populate(mode, pkg, room);
            c.showOverlay(modal);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void showOverlay(Parent modal) {
        if (host == null) throw new IllegalStateException("Host pane is required for modal overlay");
        overlay = (StackPane) modal;
        overlay.setPickOnBounds(true);
        if (!host.getChildren().contains(overlay)) {
            host.getChildren().add(overlay);
        }
    }

    private void populate(Mode mode, PackageBookingAdminView pkg, RoomBookingAdminView room) {
        if (mode == Mode.ROOM && room != null) {
            titleLabel.setText("Booking Cancelled Successfully");
            subtitleLabel.setText("Admin action completed");
            customerLabel.setText(ns(room.customerName));
            itemLabel.setText("Room");
            itemValue.setText(ns(room.roomName));
            dateLabel.setText("Check-in Date");
            dateValue.setText(room.checkIn == null ? "—" : room.checkIn.format(DF_DATE));
            txnValue.setText(ns(room.transactionId));
            refundLabel.setText("Refund amount of " + (room.totalPrice == null ? "BDT —" : "BDT " + room.totalPrice) + " will be processed within 3–7 business days.");
        } else if (mode == Mode.PACKAGE && pkg != null) {
            titleLabel.setText("Booking Cancelled Successfully");
            subtitleLabel.setText("Admin action completed");
            customerLabel.setText(ns(pkg.customerName));
            itemLabel.setText("Package");
            itemValue.setText(ns(pkg.packageName));
            dateLabel.setText("Booked At");
            dateValue.setText(pkg.createdAt == null ? "—" : DF_DATETIME.format(pkg.createdAt.atZone(java.time.ZoneId.systemDefault())));
            txnValue.setText(ns(pkg.transactionId));
            refundLabel.setText("Refund amount of " + (pkg.priceTotal == null ? "BDT —" : "BDT " + pkg.priceTotal) + " will be processed within 3–7 business days.");
        }
        if (closeButton != null) closeButton.setOnAction(e -> close());
        if (doneButton != null) doneButton.setOnAction(e -> close());
        if (viewAllButton != null) viewAllButton.setOnAction(e -> close());
    }

    private String ns(String s) { return s == null ? "—" : s; }

    private void close() {
        if (host != null && overlay != null) {
            host.getChildren().remove(overlay);
        }
    }
}
