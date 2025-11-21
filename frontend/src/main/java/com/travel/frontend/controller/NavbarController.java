package com.travel.frontend.controller;

import com.travel.frontend.cache.DataCache;
import com.travel.frontend.ui.Navigator;
import javafx.animation.*;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

import java.util.List;

public class NavbarController {

    @FXML private Button personalBtn;
    @FXML private Button packagesBtn;
    @FXML private Button destinationsBtn;
    @FXML private Button historyBtn;
    @FXML private Button aboutBtn;
    @FXML private Button logoutBtn;
    @FXML private Label brandSubtitle;
    @FXML private HBox navActions;
    @FXML private StackPane brandMark;
    @FXML private Label brandIcon;
    @FXML private Pane navbarRoot;

    public enum ActivePage { PERSONAL, PACKAGES, DESTINATIONS, HISTORY, ABOUT }

    @FXML
    private void initialize() {
        playEntryAnimation();
        setupLogoHover();
        setupNavHover();
        setupLogoutHover();

        personalBtn.setOnAction(e -> Navigator.goHome());
        packagesBtn.setOnAction(e -> Navigator.goPackages());
        destinationsBtn.setOnAction(e -> { /* no-op until implemented */ });
        historyBtn.setOnAction(e -> { /* no-op until implemented */ });
        aboutBtn.setOnAction(e -> { /* no-op until implemented */ });
        logoutBtn.setOnAction(e -> {
            DataCache.clear();
            Navigator.goLogin();
        });
    }

    public void setActive(ActivePage page) {
        clearActive();
        switch (page) {
            case PERSONAL -> {
                personalBtn.getStyleClass().add("activeNav");
                brandSubtitle.setText("Personal Info");
            }
            case PACKAGES -> {
                packagesBtn.getStyleClass().add("activeNav");
                brandSubtitle.setText("Packages");
            }
            case DESTINATIONS -> {
                destinationsBtn.getStyleClass().add("activeNav");
                brandSubtitle.setText("Destinations");
            }
            case HISTORY -> {
                historyBtn.getStyleClass().add("activeNav");
                brandSubtitle.setText("History");
            }
            case ABOUT -> {
                aboutBtn.getStyleClass().add("activeNav");
                brandSubtitle.setText("About");
            }
        }
    }

    private void clearActive() {
        personalBtn.getStyleClass().remove("activeNav");
        packagesBtn.getStyleClass().remove("activeNav");
        destinationsBtn.getStyleClass().remove("activeNav");
        historyBtn.getStyleClass().remove("activeNav");
        aboutBtn.getStyleClass().remove("activeNav");
    }

    private void playEntryAnimation() {
        if (navbarRoot != null) {
            navbarRoot.setOpacity(0);
            navbarRoot.setTranslateY(-30);
            FadeTransition fade = new FadeTransition(Duration.millis(600), navbarRoot);
            fade.setFromValue(0);
            fade.setToValue(1);
            TranslateTransition slide = new TranslateTransition(Duration.millis(600), navbarRoot);
            slide.setFromY(-30);
            slide.setToY(0);
            slide.setInterpolator(Interpolator.EASE_OUT);
            new ParallelTransition(fade, slide).play();
        }
        if (navActions != null) {
            List<Button> items = List.of(personalBtn, packagesBtn, destinationsBtn, historyBtn, aboutBtn);
            SequentialTransition seq = new SequentialTransition();
            double delayStep = 100;
            for (int i = 0; i < items.size(); i++) {
                Button b = items.get(i);
                if (b == null) continue;
                b.setOpacity(0);
                b.setTranslateY(-12);
                FadeTransition f = new FadeTransition(Duration.millis(260), b);
                f.setFromValue(0); f.setToValue(1);
                TranslateTransition t = new TranslateTransition(Duration.millis(260), b);
                t.setFromY(-12); t.setToY(0);
                t.setInterpolator(Interpolator.EASE_OUT);
                SequentialTransition entry = new SequentialTransition(
                        new PauseTransition(Duration.millis(i * delayStep)),
                        new ParallelTransition(f, t)
                );
                seq.getChildren().add(entry);
            }
            seq.play();
        }
    }

    private void setupLogoHover() {
        if (brandMark == null || brandIcon == null) return;
        brandMark.setOnMouseEntered(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(180), brandMark);
            st.setToX(1.05); st.setToY(1.05);
            st.play();
            RotateTransition rt = new RotateTransition(Duration.millis(600), brandIcon);
            rt.setByAngle(360);
            rt.setInterpolator(Interpolator.EASE_BOTH);
            rt.play();
        });
        brandMark.setOnMouseExited(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(180), brandMark);
            st.setToX(1.0); st.setToY(1.0);
            st.play();
        });
    }

    private void setupNavHover() {
        List<Button> items = List.of(personalBtn, packagesBtn, destinationsBtn, historyBtn, aboutBtn);
        for (Button b : items) {
            if (b == null) continue;
            b.setOnMouseEntered(e -> {
                ScaleTransition st = new ScaleTransition(Duration.millis(140), b);
                st.setToX(1.05); st.setToY(1.05);
                st.play();
            });
            b.setOnMouseExited(e -> {
                ScaleTransition st = new ScaleTransition(Duration.millis(140), b);
                st.setToX(1.0); st.setToY(1.0);
                st.play();
            });
            b.setOnMousePressed(e -> {
                ScaleTransition st = new ScaleTransition(Duration.millis(100), b);
                st.setToX(0.95); st.setToY(0.95);
                st.play();
            });
            b.setOnMouseReleased(e -> {
                ScaleTransition st = new ScaleTransition(Duration.millis(140), b);
                st.setToX(1.0); st.setToY(1.0);
                st.play();
            });
        }
    }

    private void setupLogoutHover() {
        if (logoutBtn == null) return;
        logoutBtn.setOnMouseEntered(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(150), logoutBtn);
            st.setToX(1.05); st.setToY(1.05);
            st.play();
        });
        logoutBtn.setOnMouseExited(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(150), logoutBtn);
            st.setToX(1.0); st.setToY(1.0);
            st.play();
        });
        logoutBtn.setOnMousePressed(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(100), logoutBtn);
            st.setToX(0.95); st.setToY(0.95);
            st.play();
        });
        logoutBtn.setOnMouseReleased(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(150), logoutBtn);
            st.setToX(1.0); st.setToY(1.0);
            st.play();
        });
    }
}
