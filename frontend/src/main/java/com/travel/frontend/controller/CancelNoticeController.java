package com.travel.frontend.controller;

import com.travel.frontend.model.HistoryPackageItem;
import com.travel.frontend.model.HistoryRoomItem;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.Node;
import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.ParallelTransition;
import javafx.util.Duration;

import java.io.IOException;

public class CancelNoticeController {
    @FXML private Label titleLabel;
    @FXML private Label subtitleLabel;
    @FXML private Label warningLabel;
    @FXML private Label txnLabel;
    @FXML private Button closeButton;
    @FXML private Button closeActionButton;
    @FXML private Button supportButton;

    private Pane host;
    private StackPane overlay;
    private Runnable afterClose;

    public enum Type { PACKAGE, ROOM }

    public static void show(Pane host, HistoryPackageItem pkg) {
        showInternal(host, Type.PACKAGE, pkg, null, null);
    }

    public static void show(Pane host, HistoryPackageItem pkg, Runnable afterClose) {
        showInternal(host, Type.PACKAGE, pkg, null, afterClose);
    }

    public static void show(Pane host, HistoryRoomItem room) {
        showInternal(host, Type.ROOM, null, room, null);
    }

    public static void show(Pane host, HistoryRoomItem room, Runnable afterClose) {
        showInternal(host, Type.ROOM, null, room, afterClose);
    }

    private static void showInternal(Pane host, Type type, HistoryPackageItem pkg, HistoryRoomItem room, Runnable after) {
        try {
            FXMLLoader loader = new FXMLLoader(CancelNoticeController.class.getResource("/fxml/cancel_notice.fxml"));
            Parent modal = loader.load();
            CancelNoticeController c = loader.getController();
            c.host = host;
            c.afterClose = after;
            c.populate(type, pkg, room);
            c.showOverlay(modal);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void showOverlay(Parent modal) {
        overlay = (StackPane) modal;
        overlay.setPickOnBounds(true);
        if (host != null && !host.getChildren().contains(overlay)) {
            host.getChildren().add(overlay);
            // play simple pop-and-fade animation on the modal content
            Node content = overlay.lookup(".modal-root");
            if (content == null && !overlay.getChildren().isEmpty()) {
                content = overlay.getChildren().get(0);
            }
            if (content != null) {
                content.setOpacity(0);
                content.setScaleX(0.96);
                content.setScaleY(0.96);
                FadeTransition ft = new FadeTransition(Duration.millis(220), content);
                ft.setFromValue(0);
                ft.setToValue(1);
                ScaleTransition st = new ScaleTransition(Duration.millis(220), content);
                st.setFromX(0.96);
                st.setFromY(0.96);
                st.setToX(1);
                st.setToY(1);
                ParallelTransition pt = new ParallelTransition(ft, st);
                pt.play();
            }
        }
    }

    private void populate(Type type, HistoryPackageItem pkg, HistoryRoomItem room) {
        if (type == Type.PACKAGE && pkg != null) {
            titleLabel.setText("Booking Cancelled");
            subtitleLabel.setText("By our support team");
            warningLabel.setText("Your booking for the package \"" + safe(pkg.packageName) + "\" has been cancelled by our support team due to verification or policy issues.");
            txnLabel.setText(pkg.transactionId == null ? "—" : pkg.transactionId);
        } else if (type == Type.ROOM && room != null) {
            titleLabel.setText("Booking Cancelled");
            subtitleLabel.setText("By our support team");
            warningLabel.setText("Your booking for the room \"" + safe(room.roomName) + "\" at \"" + safe(room.hotelName) + "\" has been cancelled by our support team due to verification or policy issues.");
            txnLabel.setText(room.transactionId == null ? "—" : room.transactionId);
        }
        if (closeButton != null) closeButton.setOnAction(e -> close());
        if (closeActionButton != null) closeActionButton.setOnAction(e -> close());
        if (supportButton != null) supportButton.setOnAction(e -> close());
    }

    private String safe(String s) { return s == null ? "—" : s; }

    private void close() {
        if (host != null && overlay != null) {
            host.getChildren().remove(overlay);
        }
        if (afterClose != null) {
            afterClose.run();
        }
    }
}
