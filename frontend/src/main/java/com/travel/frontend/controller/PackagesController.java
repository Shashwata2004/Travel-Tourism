/* Loads the list of travel packages from the server, shows each one as a card,
   and lets people open detailed views or hop to other sections from the navbar.
   Combines DataCache, background threads, and JavaFX UI building so the page
   feels smooth even while waiting on HTTP responses. */
package com.travel.frontend.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.travel.frontend.net.ApiClient;
import com.travel.frontend.cache.DataCache;
import com.travel.frontend.ui.Navigator;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Circle;
import javafx.util.Duration;
import javafx.animation.TranslateTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.Interpolator;

import java.math.BigDecimal;
import java.util.List;
import java.util.Random;
import java.util.UUID;

public class PackagesController {
    @FXML private FlowPane listContainer;
    @FXML private Pane blobLayer;
    @FXML private Circle blobA;
    @FXML private Circle blobB;
    @FXML private Circle blobC;
    @FXML private NavbarController navbarController;

    private final ApiClient api = ApiClient.get();
    private final ObjectMapper mapper = new ObjectMapper();

    /* JavaFX lifecycle hook that kicks off the first load so visitors instantly
       see featured trips once the screen renders. */
    @FXML
    private void initialize() {
        if (navbarController != null) navbarController.setActive(NavbarController.ActivePage.PACKAGES);
        animateBlobs();
        loadPackages();
    }

    /* Core data flow: runs the network request in a background Thread, caches
       the JSON response, and marshals back to the FX thread via Platform.runLater. */
    private void loadPackages() {
        new Thread(() -> {
            try {
                List<PackageCard> items = DataCache.getOrLoad("packages", () -> {
                    var res = api.rawGet("/packages", true);
                    if (res.statusCode() != 200) throw new ApiClient.ApiException("Failed to load packages");
                    return mapper.readValue(res.body(), new TypeReference<List<PackageCard>>(){});
                });
                Platform.runLater(() -> render(items));
            } catch (Exception e) {
                Platform.runLater(() -> {
                    listContainer.getChildren().setAll(new Label("Failed to load packages: " + e.getMessage()));
                });
            }
        }).start();
    }

    /* Rebuilds the list of cards. Called on the FX thread with the freshest
       list so UI updates are safe and flicker-free. */
    private void render(List<PackageCard> items) {
        listContainer.getChildren().clear();
        for (int i = 0; i < items.size(); i++) {
            PackageCard p = items.get(i);
            Node card = createCard(p);
            listContainer.getChildren().add(card);
            animateCardIn(card, i);
        }
    }

    /* Creates the visual card for one package using standard layout nodes and
       wires the “View Details” button to open the modal controller. */
    private Pane createCard(PackageCard p) {
        VBox card = new VBox();
        card.getStyleClass().add("pkg-card");
        card.setPrefWidth(340);

        StackPane imageWrap = new StackPane();
        imageWrap.getStyleClass().add("imageWrap");
        imageWrap.setPrefHeight(260);
        imageWrap.setBackground(Background.EMPTY);
        imageWrap.setPrefWidth(340);
        imageWrap.setMaxWidth(340);
        imageWrap.setMinWidth(340);

        ImageView view = new ImageView();
        view.getStyleClass().add("imageView");
        view.setFitHeight(260);
        view.setFitWidth(340);
        view.setPreserveRatio(false); // cover the frame without letterboxing
        view.setSmooth(true);
        Image img = loadCachedImage(p.destImageUrl, 360, 260);
        if (img != null) view.setImage(img);

        Rectangle clip = new Rectangle(340, 260);
        clip.setArcWidth(22);
        clip.setArcHeight(22);
        imageWrap.setClip(clip);

        StackPane.setAlignment(view, Pos.CENTER);
        imageWrap.getChildren().add(view);

        VBox body = new VBox(8);
        body.getStyleClass().add("cardBody");

        Label title = new Label(p.name);
        title.getStyleClass().add("cardTitle");

        HBox meta = new HBox();
        meta.getStyleClass().add("metaRow");
        HBox duration = new HBox();
        duration.getStyleClass().addAll("metaItem", "cardMeta");
        Label durationIcon = new Label("\uD83D\uDCC5");
        Label durationText = new Label("3 Days, 2 Nights");
        durationText.getStyleClass().add("cardMeta");
        duration.getChildren().addAll(durationIcon, durationText);

        HBox group = new HBox();
        group.getStyleClass().addAll("metaItem", "cardMeta");
        Label groupIcon = new Label("\uD83D\uDC65");
        Label groupText = new Label("2-6 People");
        groupText.getStyleClass().add("cardMeta");
        group.getChildren().addAll(groupIcon, groupText);

        meta.getChildren().addAll(duration, group);

        HBox priceRow = new HBox(6);
        priceRow.setAlignment(Pos.CENTER_LEFT);
        Label hint = new Label("Starting From");
        hint.getStyleClass().add("priceHint");
        Label price = new Label("BDT " + p.basePrice);
        price.getStyleClass().add("priceRow");
        priceRow.getChildren().addAll(hint, price);

        Button details = new Button("View Details");
        details.getStyleClass().add("primaryBtn");
        details.setMaxWidth(Double.MAX_VALUE);
        details.setOnAction(e -> PackageDetailsController.open(p.id));

        body.getChildren().addAll(title, meta, priceRow, details);

        card.getChildren().addAll(imageWrap, body);

        applyCardHover(card, view);
        return card;
    }

    /* Small record that mirrors what the /packages endpoint returns, making it
       easy to bind fields to labels without deeper models. */
    public static class PackageCard {
        public UUID id;
        public String name;
        public String location;
        public BigDecimal basePrice;
        public String destImageUrl;
    }

    private StackPane createImageNode(String url) {
        StackPane wrapper = new StackPane();
        wrapper.setMinSize(220, 140);
        wrapper.setPrefSize(220, 140);

        ImageView view = new ImageView();
        view.setFitWidth(220);
        view.setFitHeight(140);
        view.setPreserveRatio(true);
        view.setSmooth(true);

        Image img = loadCachedImage(url, 220, 140);
        if (img != null) {
            view.setImage(img);
        }

        wrapper.getChildren().add(view);
        return wrapper;
    }

    private Image loadCachedImage(String url, double width, double height) {
        if (url == null || url.isBlank()) return null;
        try {
            return DataCache.getOrLoad("img:" + width + "x" + height + ":" + url,
                    () -> new Image(url, width, height, true, true, true));
        } catch (Exception e) {
            return null;
        }
    }

    @FXML private void goPersonal() { Navigator.goHome(); }
    @FXML private void goDestinations() { Navigator.goDestinations(); }
    @FXML private void goHistory() { /* placeholder */ }
    @FXML private void goAbout() { /* placeholder */ }
    @FXML private void onLogout() { com.travel.frontend.cache.DataCache.clear(); Navigator.goLogin(); }

    private void applyCardHover(Pane card, ImageView view) {
        card.setOnMouseEntered(e -> {
            TranslateTransition lift = new TranslateTransition(Duration.millis(250), card);
            lift.setToY(-10);
            lift.setInterpolator(Interpolator.EASE_BOTH);
            lift.play();
            if (view != null) {
                ScaleTransition st = new ScaleTransition(Duration.millis(300), view);
                st.setToX(1.05);
                st.setToY(1.05);
                st.setInterpolator(Interpolator.EASE_BOTH);
                st.play();
            }
        });
        card.setOnMouseExited(e -> {
            TranslateTransition drop = new TranslateTransition(Duration.millis(250), card);
            drop.setToY(0);
            drop.setInterpolator(Interpolator.EASE_BOTH);
            drop.play();
            if (view != null) {
                ScaleTransition st = new ScaleTransition(Duration.millis(300), view);
                st.setToX(1.0);
                st.setToY(1.0);
                st.setInterpolator(Interpolator.EASE_BOTH);
                st.play();
            }
        });
    }

    private void animateCardIn(Node card, int index) {
        if (card == null) return;
        card.setOpacity(0);
        card.setTranslateY(26);
        TranslateTransition rise = new TranslateTransition(Duration.millis(420), card);
        rise.setFromY(26);
        rise.setToY(0);
        rise.setInterpolator(Interpolator.EASE_OUT);
        javafx.animation.FadeTransition fade = new javafx.animation.FadeTransition(Duration.millis(420), card);
        fade.setFromValue(0);
        fade.setToValue(1);
        javafx.animation.ParallelTransition pt = new javafx.animation.ParallelTransition(rise, fade);
        pt.setDelay(Duration.millis(index * 80.0));
        pt.play();
    }

    private void animateBlobs() {
        if (blobLayer == null) return;
        Random rnd = new Random();
        if (blobA != null) {
            TranslateTransition tt = new TranslateTransition(Duration.seconds(18), blobA);
            tt.setFromX(0); tt.setToX(80);
            tt.setFromY(0); tt.setToY(40);
            tt.setAutoReverse(true);
            tt.setCycleCount(TranslateTransition.INDEFINITE);
            tt.setInterpolator(Interpolator.EASE_BOTH);
            tt.play();
        }
        if (blobB != null) {
            TranslateTransition tt = new TranslateTransition(Duration.seconds(16), blobB);
            tt.setFromX(0); tt.setToX(-80);
            tt.setFromY(0); tt.setToY(-50);
            tt.setAutoReverse(true);
            tt.setCycleCount(TranslateTransition.INDEFINITE);
            tt.setInterpolator(Interpolator.EASE_BOTH);
            tt.play();
        }
        if (blobC != null) {
            TranslateTransition tt = new TranslateTransition(Duration.seconds(20), blobC);
            tt.setFromX(0); tt.setToX(90);
            tt.setFromY(0); tt.setToY(30);
            tt.setAutoReverse(true);
            tt.setCycleCount(TranslateTransition.INDEFINITE);
            tt.setInterpolator(Interpolator.EASE_BOTH);
            tt.play();
        }
    }
}
