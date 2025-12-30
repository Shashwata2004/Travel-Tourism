package com.travel.frontend.controller;

import java.awt.Desktop;
import java.net.URI;
import java.net.URL;

import com.travel.frontend.MainApp;

import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.RotateTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

public class AboutController {
    @FXML private StackPane aboutRoot;
    @FXML private Pane blobLayer;
    @FXML private Circle blobA;
    @FXML private Circle blobB;
    @FXML private Circle blobC;
    @FXML private NavbarController navbarController;

    @FXML private VBox cardShashwata;
    @FXML private VBox cardSwarlok;
    @FXML private VBox cardFahmid;
    @FXML private VBox cardSudipto;
    @FXML private StackPane imageWrapShashwata;
    @FXML private StackPane imageWrapSwarlok;
    @FXML private StackPane imageWrapFahmid;
    @FXML private StackPane imageWrapSudipto;
    @FXML private ImageView imgShashwata;
    @FXML private ImageView imgSwarlok;
    @FXML private ImageView imgFahmid;
    @FXML private ImageView imgSudipto;

    @FXML
    private void initialize() {
        if (navbarController != null) {
            navbarController.setActive(NavbarController.ActivePage.ABOUT);
        }
        ensureStyles();
        animateBlobs();
        applyImageClip(imgShashwata, 22);
        applyImageClip(imgSwarlok, 22);
        applyImageClip(imgFahmid, 22);
        applyImageClip(imgSudipto, 22);
        fixRotation(imgShashwata, -90);
        fixRotation(imgSwarlok, 0);
        fixRotation(imgFahmid, 0);
        fixRotation(imgSudipto, 0);
        setupHover(cardShashwata, imageWrapShashwata);
        setupHover(cardSwarlok, imageWrapSwarlok);
        setupHover(cardFahmid, imageWrapFahmid);
        setupHover(cardSudipto, imageWrapSudipto);
    }

    private void ensureStyles() {
        if (aboutRoot == null) return;
        URL url = AboutController.class.getResource("/css/about.css");
        if (url == null) return;
        String css = url.toExternalForm();
        aboutRoot.getStylesheets().removeIf(s -> s.contains("about.css"));
        aboutRoot.getStylesheets().add(css);
        aboutRoot.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene == null) return;
            newScene.getStylesheets().removeIf(s -> s.contains("about.css"));
            newScene.getStylesheets().add(css);
        });
    }

    @FXML
    private void openMail(ActionEvent event) {
        openLink(event);
    }

    @FXML
    private void openGithub(ActionEvent event) {
        openLink(event);
    }

    private void openLink(ActionEvent event) {
        if (event == null || event.getSource() == null) return;
        Object data = ((Node) event.getSource()).getUserData();
        if (data == null) return;
        String url = data.toString();
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop desktop = Desktop.getDesktop();
                URI uri = URI.create(url);
                if (url.startsWith("mailto:") && desktop.isSupported(Desktop.Action.MAIL)) {
                    desktop.mail(uri);
                } else if (desktop.isSupported(Desktop.Action.BROWSE)) {
                    desktop.browse(uri);
                }
                return;
            }
        } catch (Exception ignore) {
        }
        var host = MainApp.hostServices();
        if (host != null) {
            host.showDocument(url);
        }
    }

    private void setupHover(VBox card, StackPane imageWrap) {
        if (card == null) return;
        card.setOnMouseEntered(e -> animateCard(card, imageWrap, 1.03, -8, 4));
        card.setOnMouseExited(e -> animateCard(card, imageWrap, 1.0, 0, 0));
    }

    private void animateCard(Node card, Node imageWrap, double scale, double translateY, double rotate) {
        ScaleTransition st = new ScaleTransition(Duration.millis(220), card);
        st.setToX(scale);
        st.setToY(scale);
        TranslateTransition tt = new TranslateTransition(Duration.millis(220), card);
        tt.setToY(translateY);
        ParallelTransition pt = new ParallelTransition(st, tt);
        pt.setInterpolator(Interpolator.EASE_OUT);
        pt.play();
        if (imageWrap != null) {
            RotateTransition rt = new RotateTransition(Duration.millis(220), imageWrap);
            rt.setToAngle(rotate);
            rt.setInterpolator(Interpolator.EASE_OUT);
            rt.play();
        }
    }

    private void applyImageClip(ImageView view, double arc) {
        if (view == null) return;
        Rectangle clip = new Rectangle(view.getFitWidth(), view.getFitHeight());
        clip.setArcWidth(arc);
        clip.setArcHeight(arc);
        view.setClip(clip);
    }

    private void fixRotation(ImageView view, double angle) {
        if (view == null) return;
        view.setRotate(angle);
    }

    private void animateBlobs() {
        if (blobLayer == null) return;
        if (blobA != null) {
            TranslateTransition tt = new TranslateTransition(Duration.seconds(18), blobA);
            tt.setFromX(0);
            tt.setToX(80);
            tt.setFromY(0);
            tt.setToY(40);
            tt.setAutoReverse(true);
            tt.setCycleCount(TranslateTransition.INDEFINITE);
            tt.setInterpolator(Interpolator.EASE_BOTH);
            tt.play();
        }
        if (blobB != null) {
            TranslateTransition tt = new TranslateTransition(Duration.seconds(16), blobB);
            tt.setFromX(0);
            tt.setToX(-80);
            tt.setFromY(0);
            tt.setToY(-50);
            tt.setAutoReverse(true);
            tt.setCycleCount(TranslateTransition.INDEFINITE);
            tt.setInterpolator(Interpolator.EASE_BOTH);
            tt.play();
        }
        if (blobC != null) {
            TranslateTransition tt = new TranslateTransition(Duration.seconds(20), blobC);
            tt.setFromX(0);
            tt.setToX(90);
            tt.setFromY(0);
            tt.setToY(30);
            tt.setAutoReverse(true);
            tt.setCycleCount(TranslateTransition.INDEFINITE);
            tt.setInterpolator(Interpolator.EASE_BOTH);
            tt.play();
        }
    }
}
