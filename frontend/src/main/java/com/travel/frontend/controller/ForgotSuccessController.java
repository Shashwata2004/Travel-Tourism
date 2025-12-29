package com.travel.frontend.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.ParallelTransition;
import javafx.util.Duration;

import java.io.IOException;

public class ForgotSuccessController {
    @FXML private Button continueButton;

    private Pane host;
    private StackPane overlay;
    private Runnable onContinue;

    public static void show(Pane host, Runnable onContinue) {
        try {
            FXMLLoader loader = new FXMLLoader(ForgotSuccessController.class.getResource("/fxml/forgot_success.fxml"));
            Parent modal = loader.load();
            ForgotSuccessController c = loader.getController();
            c.host = host;
            c.onContinue = onContinue;
            c.init();
            c.showOverlay(modal);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void init() {
        if (continueButton != null) {
            continueButton.setOnAction(e -> {
                close();
                if (onContinue != null) onContinue.run();
            });
        }
    }

    private void showOverlay(Parent modal) {
        overlay = (StackPane) modal;
        overlay.setPickOnBounds(true);
        if (host != null && !host.getChildren().contains(overlay)) {
            host.getChildren().add(overlay);
            animate(modal);
        }
    }

    private void animate(Parent modal) {
        if (modal == null) return;
        modal.setOpacity(0);
        modal.setScaleX(0.96);
        modal.setScaleY(0.96);
        FadeTransition ft = new FadeTransition(Duration.millis(220), modal);
        ft.setFromValue(0);
        ft.setToValue(1);
        ScaleTransition st = new ScaleTransition(Duration.millis(220), modal);
        st.setFromX(0.96);
        st.setFromY(0.96);
        st.setToX(1);
        st.setToY(1);
        new ParallelTransition(ft, st).play();
    }

    private void close() {
        if (host != null && overlay != null) {
            host.getChildren().remove(overlay);
        }
    }
}
