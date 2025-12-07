/* Loads the list of travel packages from the server, shows each one as a card,
   and lets people open detailed views or hop to other sections from the navbar.
   Combines DataCache, background threads, and JavaFX UI building so the page
   feels smooth even while waiting on HTTP responses. */
package com.travel.frontend.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.travel.frontend.net.ApiClient;
import com.travel.frontend.cache.DataCache;
import com.travel.frontend.cache.FileCache;
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
import javafx.scene.shape.SVGPath;
import javafx.scene.paint.Color;
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
    private static final String CACHE_VERSION = "v2";

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
                String key = "packages:list:" + CACHE_VERSION;
                List<PackageCard> cached = DataCache.peek(key);
                if (cached == null) {
                    final List<PackageCard>[] holder = new List[1];
                    cached = FileCache.getOrLoad("packages:list:" + CACHE_VERSION,
                            new TypeReference<List<PackageCard>>(){},
                            () -> {
                                try {
                                    var res = api.rawGet("/packages", true);
                                    if (res.statusCode() != 200) {
                                        throw new ApiClient.ApiException("Failed to load packages");
                                    }
                                    holder[0] = mapper.readValue(res.body(), new TypeReference<List<PackageCard>>(){});
                                    return holder[0];
                                } catch (Exception ex) {
                                    throw new RuntimeException(ex);
                                }
                            });
                    DataCache.put(key, cached);
                }
                final List<PackageCard> items = cached;
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
        SVGPath calendarIcon = createSvgIcon("M5 4c-.552 0-1 .448-1 1v1H3v2h18V6h-1V5c0-.552-.448-1-1-1h-2V3h-2v2H9V3H7v2H5zm0 4v9c0 1.103.897 2 2 2h10c1.103 0 2-.897 2-2V8H5zm3 3h2v2H8v-2zm4 0h2v2h-2v-2z",
                16, "#5f7d9d");
        Label durationText = new Label("3 Days, 2 Nights");
        durationText.getStyleClass().add("cardMeta");
        duration.getChildren().addAll(calendarIcon, durationText);

        HBox group = new HBox();
        group.getStyleClass().addAll("metaItem", "cardMeta");
        SVGPath peopleIcon = createSvgIcon("M8 3.5a3 3 0 1 1 0 6 3 3 0 0 1 0-6zm8 1.5a2.5 2.5 0 1 1 0 5 2.5 2.5 0 0 1 0-5zM8 11c-3.59 0-6.5 2.3-6.5 5v2.5h9V16c0-.59.15-1.17.44-1.69A6.87 6.87 0 0 0 8 11zm5.54 1.16c-.37.16-.71.35-1.02.57-.36.25-.6.47-.74.61-.2.2-.3.33-.34.39-.07.1-.14.22-.18.34-.15.39-.24.81-.24 1.23V18.5H20V17c0-1.82-1.63-3.18-2.8-3.8a5.44 5.44 0 0 0-3.66-.99z",
                16, "#5f7d9d");
        String groupLabel = (p.groupSize == null || p.groupSize.isBlank()) ? "2-6 People" : p.groupSize;
        Label groupText = new Label(groupLabel);
        groupText.getStyleClass().add("cardMeta");
        group.getChildren().addAll(peopleIcon, groupText);

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
        public String groupSize;
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

    private SVGPath createSvgIcon(String pathData, double size, String colorHex) {
        SVGPath icon = new SVGPath();
        icon.setContent(pathData);
        icon.setFill(Color.web(colorHex));
        icon.setScaleX(size / 24.0);
        icon.setScaleY(size / 24.0);
        return icon;
    }

    @FXML private void goPersonal() { Navigator.goHome(); }
    @FXML private void goDestinations() { Navigator.goDestinations(); }
    @FXML private void goHistory() { Navigator.goHistory(); }
    @FXML private void goAbout() { /* placeholder */ }
    @FXML private void onLogout() { com.travel.frontend.cache.DataCache.clear(); Navigator.goLogin(); }

    @FXML
    private void reloadPackages() {
        String key = "packages:list:" + CACHE_VERSION;
        DataCache.remove(key);
        FileCache.remove("packages:list:" + CACHE_VERSION);
        loadPackages();
    }

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
