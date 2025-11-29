package com.travel.frontend.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.travel.frontend.cache.DataCache;
import com.travel.frontend.cache.FileCache;
import com.travel.frontend.net.ApiClient;
import com.travel.frontend.ui.Navigator;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.SVGPath;
import javafx.scene.paint.Color;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Builds the immersive destinations gallery: hero section with search and
 * animated cards that mirror the Tailwind mockup provided by the user.
 */
public class DestinationsController {

    @FXML private FlowPane cardsContainer;
    @FXML private TextField searchField;
    @FXML private HBox searchWrap;
    @FXML private Label emptyState;
    @FXML private NavbarController navbarController;
    @FXML private Pane orbLayer;
    @FXML private Circle orbA;
    @FXML private Circle orbB;
    @FXML private Circle orbC;

    private final ApiClient api = ApiClient.get();
    private final ObjectMapper mapper = new ObjectMapper();
    private final List<DestinationCard> allItems = new ArrayList<>();
    private Timeline searchGlow;
    private Timeline searchBorderPulse;
    private TranslateTransition searchLift;
    private DropShadow searchEffect;
    private static final String CACHE_VERSION = "v2";

    @FXML
    private void initialize() {
        if (navbarController != null) {
            navbarController.setActive(NavbarController.ActivePage.DESTINATIONS);
        }
        setupSearch();
        animateOrbs();
        loadDestinations();
    }

    @FXML
    private void reloadDestinations() {
        String key = "destinations:list:" + CACHE_VERSION;
        DataCache.remove(key);
        FileCache.remove(key);
        // also purge any legacy key to avoid stale reuse
        DataCache.remove("destinations:list");
        FileCache.remove("destinations:list");
        if (emptyState != null) {
            emptyState.setVisible(false);
            emptyState.setManaged(false);
        }
        loadDestinations();
    }

    private void setupSearch() {
        if (searchField != null) {
            searchField.textProperty().addListener((obs, old, val) -> applyFilter());
            searchField.focusedProperty().addListener((obs, was, focused) -> handleSearchFocus(focused));
        }
        if (searchWrap != null) {
            searchWrap.setOnMouseClicked(e -> {
                if (searchField != null) {
                    searchField.requestFocus();
                }
            });
            setupSearchAnimation();
        }
    }

    private void loadDestinations() {
        new Thread(() -> {
            try {
                String key = "destinations:list:" + CACHE_VERSION;
                List<DestinationCard> cached = DataCache.peek(key);
                if (cached == null) {
                    final List<DestinationCard>[] holder = new List[1];
                    cached = FileCache.getOrLoad(key,
                            new TypeReference<List<DestinationCard>>() {},
                            () -> {
                                try {
                                    var res = api.rawGet("/destinations", true);
                                    if (res.statusCode() != 200) {
                                        throw new ApiClient.ApiException("Failed to load destinations");
                                    }
                                    holder[0] = mapper.readValue(res.body(), new TypeReference<List<DestinationCard>>() {});
                                    return holder[0];
                                } catch (Exception ex) {
                                    throw new RuntimeException(ex);
                                }
                            });
                    DataCache.put(key, cached);
                }
                final List<DestinationCard> items = cached;
                Platform.runLater(() -> {
                    allItems.clear();
                    allItems.addAll(items);
                    applyFilter();
                });
            } catch (Exception e) {
                Platform.runLater(() -> showError(e.getMessage()));
            }
        }).start();
    }

    private void applyFilter() {
        String query = searchField != null ? searchField.getText() : "";
        List<DestinationCard> filtered;
        if (query == null || query.isBlank()) {
            filtered = List.copyOf(allItems);
        } else {
            String q = query.toLowerCase(Locale.ROOT);
            filtered = allItems.stream()
                    .filter(item -> matches(item, q))
                    .collect(Collectors.toList());
        }
        render(filtered);
    }

    private boolean matches(DestinationCard item, String query) {
        return contains(item.name, query)
                || contains(item.region, query)
                || contains(item.tags, query);
    }

    private boolean contains(String text, String needle) {
        return text != null && text.toLowerCase(Locale.ROOT).contains(needle);
    }

    private void render(List<DestinationCard> items) {
        cardsContainer.getChildren().clear();
        boolean empty = items.isEmpty();
        emptyState.setManaged(empty);
        emptyState.setVisible(empty);
        if (empty) {
            return;
        }

        for (int i = 0; i < items.size(); i++) {
            DestinationCard card = items.get(i);
            Node node = createCard(card);
            cardsContainer.getChildren().add(node);
            animateCardIn(node, i);
        }
    }

    private Node createCard(DestinationCard d) {
        double cardWidth = 400;
        double imageHeight = 310;

        VBox wrapper = new VBox();
        wrapper.getStyleClass().add("dest-card");
        wrapper.setPrefWidth(cardWidth);
        wrapper.setMaxWidth(cardWidth);

        StackPane imageWrap = new StackPane();
        imageWrap.getStyleClass().add("destImageWrap");
        imageWrap.setPrefHeight(imageHeight);
        imageWrap.setMinHeight(imageHeight);
        imageWrap.setMaxHeight(imageHeight);

        ImageView heroImage = new ImageView();
        heroImage.setFitWidth(cardWidth);
        heroImage.setFitHeight(imageHeight);
        heroImage.setPreserveRatio(false);
        heroImage.setSmooth(true);
        Image img = loadImage(d.imageUrl, cardWidth, imageHeight);
        if (img != null) heroImage.setImage(img);

        Rectangle clip = new Rectangle(cardWidth, imageHeight);
        clip.setArcWidth(30);
        clip.setArcHeight(30);
        imageWrap.setClip(clip);

        imageWrap.getChildren().add(heroImage);

        VBox overlay = new VBox(4);
        overlay.getStyleClass().add("overlayInfo");
        Label overlayName = new Label(d.name);
        overlayName.getStyleClass().add("overlayName");
        HBox overlayRegionRow = new HBox(5);
        overlayRegionRow.getStyleClass().add("overlayRegionRow");
        StackPane overlayRegionIcon = createPinIcon();
        overlayRegionIcon.getStyleClass().add("overlayRegionIcon");
        Label overlayRegion = new Label(d.region == null ? "" : d.region);
        overlayRegion.getStyleClass().add("overlayRegionText");
        overlayRegionRow.getChildren().addAll(overlayRegionIcon, overlayRegion);
        overlay.getChildren().addAll(overlayName, overlayRegionRow);
        StackPane.setAlignment(overlay, Pos.BOTTOM_LEFT);
        StackPane.setMargin(overlay, new Insets(0, 0, 18, 18));
        imageWrap.getChildren().add(overlay);

        if (d.packageAvailable) {
            Node chip = createPackageChip(d);
            StackPane.setAlignment(chip, Pos.TOP_RIGHT);
            StackPane.setMargin(chip, new Insets(12, 12, 0, 0));
            imageWrap.getChildren().add(chip);
        }

        VBox body = new VBox(8);
        body.getStyleClass().add("destCardBody");

        Label tags = new Label(formatTags(d.tags));
        tags.getStyleClass().add("tagRow");
        tags.setWrapText(true);
        tags.setMaxWidth(cardWidth - 60);

        HBox seasonRow = new HBox(6);
        seasonRow.getStyleClass().add("seasonRow");
        SVGPath seasonIcon = createSvgIcon("M5 4c-.552 0-1 .448-1 1v1H3v2h18V6h-1V5c0-.552-.448-1-1-1h-2V3h-2v2H9V3H7v2H5zm0 4v9c0 1.103.897 2 2 2h10c1.103 0 2-.897 2-2V8H5zm3 3h2v2H8v-2zm4 0h2v2h-2v-2z",
                16, "#5f7d9d");
        Label seasonText = new Label(d.bestSeason == null || d.bestSeason.isBlank()
                ? "Best season info coming soon"
                : "Best season: " + d.bestSeason);
        seasonText.getStyleClass().add("seasonText");
        seasonRow.getChildren().addAll(seasonIcon, seasonText);

        HBox footer = new HBox(12);
        footer.setAlignment(Pos.CENTER_LEFT);

        HBox hotelsBadge = new HBox(6);
        hotelsBadge.setAlignment(Pos.CENTER_LEFT);
        hotelsBadge.getStyleClass().add("hotelsBadge");
        SVGPath hotelIcon = createSvgIcon("M4 10.5V19h2v-2h12v2h2v-8.5c0-1.657-1.343-3-3-3h-2V5c0-.552-.448-1-1-1H8c-.552 0-1 .448-1 1v2.5H5c-.552 0-1 .448-1 1zm12-1.5c.552 0 1 .448 1 1V11h-2v-2h1zM7 7h6v1.5H7V7zm0 3.5h2v2H7v-2zm4 0h2v2h-2v-2zM7 15h10v-2h-2v1h-2v-1h-2v1H9v-1H7v2z",
                16, "#5f7d9d");
        Label hotelText = new Label(Math.max(d.hotelsCount, 0) + " hotels");
        hotelText.getStyleClass().add("hotelText");
        hotelsBadge.getChildren().addAll(hotelIcon, hotelText);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button explore = new Button("Explore Hotels");
        explore.getStyleClass().add("exploreBtn");
        explore.setPrefWidth(150);
        explore.setMaxWidth(Region.USE_PREF_SIZE);
        explore.setOnAction(e -> openHotelSearch(d));

        footer.getChildren().addAll(hotelsBadge, spacer, explore);

        body.getChildren().addAll(tags, seasonRow, footer);

        wrapper.getChildren().addAll(imageWrap, body);
        wrapper.setPadding(new Insets(0, 0, 0, 0));

        applyCardHover(wrapper, heroImage, explore);
        return wrapper;
    }

    private SVGPath createSvgIcon(String pathData, double size, String colorHex) {
        SVGPath icon = new SVGPath();
        icon.setContent(pathData);
        icon.setFill(Color.web(colorHex));
        double scale = size / 24.0;
        icon.setScaleX(scale);
        icon.setScaleY(scale);
        return icon;
    }

    private Node createPackageChip(DestinationCard d) {
        if (d.packageId != null) {
            Button chip = new Button("Package Available");
            chip.getStyleClass().addAll("packageChip", "packageChipButton");
            chip.setFocusTraversable(false);
            chip.setOnAction(e -> PackageDetailsController.open(d.packageId));
            return chip;
        }
        Label chip = new Label("Package Available");
        chip.getStyleClass().add("packageChip");
        return chip;
    }

    private Image loadImage(String url, double width, double height) {
        if (url == null || url.isBlank()) return null;
        try {
            return DataCache.getOrLoad("img:" + width + "x" + height + ":" + url,
                    () -> new Image(url, width, height, true, true, true));
        } catch (Exception e) {
            return null;
        }
    }

    private void showError(String message) {
        cardsContainer.getChildren().setAll(new Label("Failed to load destinations: " + message));
    }

    private void openHotelSearch(DestinationCard destination) {
        // Clear any cached hotel list for this destination so availability reloads fresh
        if (destination != null && destination.id != null) {
            String vKey = "hotels:list:" + CACHE_VERSION + ":" + destination.id;
            DataCache.remove(vKey);
            FileCache.remove("hotels_" + CACHE_VERSION + "_" + destination.id);
            // Legacy keys (fallback)
            DataCache.remove("hotels:list:" + destination.id);
            FileCache.remove("hotels_" + destination.id);
        }
        DataCache.put("hotel:selected", destination);
        Navigator.goHotelSearch();
    }

    private String formatTags(String tags) {
        if (tags == null || tags.isBlank()) return "Adventure • Culture • Escape";
        return java.util.Arrays.stream(tags.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.joining(" • "));
    }

    private void applyCardHover(Pane card, ImageView heroImage, Button cta) {
        // Match the package card hover: gentle lift + image zoom.
        card.setOnMouseEntered(e -> {
            TranslateTransition lift = new TranslateTransition(Duration.millis(250), card);
            lift.setToY(-10);
            lift.setInterpolator(Interpolator.EASE_BOTH);
            lift.play();

            if (heroImage != null) {
                ScaleTransition st = new ScaleTransition(Duration.millis(300), heroImage);
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

            if (heroImage != null) {
                ScaleTransition st = new ScaleTransition(Duration.millis(300), heroImage);
                st.setToX(1.0);
                st.setToY(1.0);
                st.setInterpolator(Interpolator.EASE_BOTH);
                st.play();
            }
        });

        if (cta != null) {
            cta.setOnMouseEntered(e -> scaleButton(cta, 1.03));
            cta.setOnMouseExited(e -> scaleButton(cta, 1.0));
            cta.setOnMousePressed(e -> scaleButton(cta, 0.95));
            cta.setOnMouseReleased(e -> scaleButton(cta, 1.03));
        }
    }

    private void scaleButton(Button btn, double scale) {
        ScaleTransition st = new ScaleTransition(Duration.millis(150), btn);
        st.setToX(scale);
        st.setToY(scale);
        st.setInterpolator(Interpolator.EASE_BOTH);
        st.play();
    }

    private void animateCardIn(Node card, int index) {
        if (card == null) return;
        card.setOpacity(0);
        card.setTranslateY(26);
        TranslateTransition rise = new TranslateTransition(Duration.millis(420), card);
        rise.setFromY(26);
        rise.setToY(0);
        rise.setInterpolator(Interpolator.EASE_OUT);
        FadeTransition fade = new FadeTransition(Duration.millis(420), card);
        fade.setFromValue(0);
        fade.setToValue(1);
        ParallelTransition pt = new ParallelTransition(rise, fade);
        pt.setDelay(Duration.millis(index * 80.0));
        pt.play();
    }

    private void animateOrbs() {
        if (orbLayer == null) return;
        Random rnd = new Random();
        if (orbA != null) {
            TranslateTransition tt = new TranslateTransition(Duration.seconds(18), orbA);
            tt.setFromX(0); tt.setToX(80);
            tt.setFromY(0); tt.setToY(40);
            tt.setAutoReverse(true);
            tt.setCycleCount(TranslateTransition.INDEFINITE);
            tt.setInterpolator(Interpolator.EASE_BOTH);
            tt.play();
        }
        if (orbB != null) {
            TranslateTransition tt = new TranslateTransition(Duration.seconds(16), orbB);
            tt.setFromX(0); tt.setToX(-80);
            tt.setFromY(0); tt.setToY(-50);
            tt.setAutoReverse(true);
            tt.setCycleCount(TranslateTransition.INDEFINITE);
            tt.setInterpolator(Interpolator.EASE_BOTH);
            tt.play();
        }
        if (orbC != null) {
            TranslateTransition tt = new TranslateTransition(Duration.seconds(20), orbC);
            tt.setFromX(0); tt.setToX(90);
            tt.setFromY(0); tt.setToY(30);
            tt.setAutoReverse(true);
            tt.setCycleCount(TranslateTransition.INDEFINITE);
            tt.setInterpolator(Interpolator.EASE_BOTH);
            tt.play();
        }
    }

    private void setupSearchAnimation() {
        searchEffect = new DropShadow();
        searchEffect.setOffsetX(0);
        searchEffect.setOffsetY(0);
        searchEffect.setSpread(0.25);
        searchEffect.setRadius(24);
        searchEffect.setColor(javafx.scene.paint.Color.web("#4f46e5", 0.0));
        searchWrap.setEffect(searchEffect);

        searchGlow = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(searchEffect.colorProperty(), javafx.scene.paint.Color.web("#4f46e5", 0.45))),
                new KeyFrame(Duration.seconds(1.2), new KeyValue(searchEffect.colorProperty(), javafx.scene.paint.Color.web("#22d3ee", 0.45))),
                new KeyFrame(Duration.seconds(2.4), new KeyValue(searchEffect.colorProperty(), javafx.scene.paint.Color.web("#a855f7", 0.45)))
        );
        searchGlow.setCycleCount(Animation.INDEFINITE);
        searchGlow.setAutoReverse(true);

        searchBorderPulse = new Timeline(
                new KeyFrame(Duration.ZERO, e -> setSearchBorderColor("#4f46e5")),
                new KeyFrame(Duration.seconds(0.8), e -> setSearchBorderColor("#22d3ee")),
                new KeyFrame(Duration.seconds(1.6), e -> setSearchBorderColor("#a855f7"))
        );
        searchBorderPulse.setCycleCount(Animation.INDEFINITE);

        searchLift = new TranslateTransition(Duration.millis(220), searchWrap);
        searchLift.setInterpolator(Interpolator.EASE_BOTH);
    }

    private void handleSearchFocus(boolean focused) {
        if (searchGlow == null || searchEffect == null) return;
        if (focused) {
            searchGlow.playFromStart();
            if (searchBorderPulse != null) {
                searchBorderPulse.playFromStart();
            }
            if (searchLift != null) {
                searchLift.stop();
                searchLift.setToY(-8);
                searchLift.playFromStart();
            }
        } else {
            searchGlow.pause();
            searchEffect.setColor(javafx.scene.paint.Color.web("#4f46e5", 0.0));
            if (searchBorderPulse != null) {
                searchBorderPulse.stop();
                setSearchBorderColor("transparent");
            }
            if (searchLift != null) {
                searchLift.stop();
                searchWrap.setTranslateY(0);
            }
        }
    }

    private void setSearchBorderColor(String color) {
        if (searchWrap != null) {
            searchWrap.setStyle("-fx-border-color: " + color + ";");
        }
    }

    /**
     * Builds the reusable SVG pin icon used beside region names and
     * in the hero "Explore Bangladesh" badge. Styling is provided via
     * the .pinIcon and .pinCenter rules in destinations.css.
     */
    private StackPane createPinIcon() {
        SVGPath outline = new SVGPath();
        outline.setContent("M12 2c-3.3137 0-6 2.6863-6 6 0 4.5 6 10 6 10s6-5.5 6-10c0-3.3137-2.6863-6-6-6zm0 8.2c-1.215 0-2.2-.985-2.2-2.2 0-1.215.985-2.2 2.2-2.2 1.215 0 2.2.985 2.2 2.2 0 1.215-.985 2.2-2.2 2.2z");
        outline.getStyleClass().add("pinIcon");

        Circle center = new Circle(2);
        center.getStyleClass().add("pinCenter");

        StackPane pin = new StackPane(outline, center);
        return pin;
    }

    public static class DestinationCard {
        public UUID id;
        public String name;
        public String region;
        public String tags;
        public String bestSeason;
        public String imageUrl;
        public int hotelsCount;
        public boolean packageAvailable;
        public boolean active;
        public UUID packageId;
    }
}
