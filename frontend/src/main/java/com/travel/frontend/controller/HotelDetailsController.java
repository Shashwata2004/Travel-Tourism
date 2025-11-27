package com.travel.frontend.controller;

import com.travel.frontend.cache.DataCache;
import com.travel.frontend.cache.FileCache;
import com.travel.frontend.model.HotelDetails;
import com.travel.frontend.net.ApiClient;
import com.travel.frontend.ui.Navigator;
import com.fasterxml.jackson.core.type.TypeReference;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.GridPane;
import javafx.scene.control.Separator;
import javafx.util.Duration;
import javafx.scene.shape.SVGPath;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class HotelDetailsController {
    @FXML private ImageView mainImage;
    @FXML private HBox thumbnails;
    @FXML private Label imageCounter;
    @FXML private Label nameLabel;
    @FXML private Label locationLabel;
    @FXML private Label ratingLabel;
    @FXML private GridPane nearbyGrid;
    @FXML private TilePane facilitiesGrid;
    @FXML private Button backButton;
    @FXML private Pane orbLayer;
    @FXML private javafx.scene.shape.Circle orbA;
    @FXML private javafx.scene.shape.Circle orbB;
    @FXML private javafx.scene.shape.Circle orbC;

    private final ApiClient api = ApiClient.get();
    private final List<String> images = new ArrayList<>();
    private int currentIndex = 0;
    private Timeline slideshow;
    private Timeline orbAnim;

    @FXML
    private void initialize() {
        if (backButton != null) backButton.setOnAction(e -> Navigator.goHotelSearch());
        animateOrbs();
        loadData();
    }

    @FXML
    private void goBack() {
        Navigator.goHotelSearch();
    }

    private void animateOrbs() {
        if (orbA == null || orbB == null || orbC == null) return;
        orbAnim = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new javafx.animation.KeyValue(orbA.centerXProperty(), 200),
                        new javafx.animation.KeyValue(orbA.centerYProperty(), 200),
                        new javafx.animation.KeyValue(orbB.centerXProperty(), 900),
                        new javafx.animation.KeyValue(orbB.centerYProperty(), 180),
                        new javafx.animation.KeyValue(orbC.centerXProperty(), 500),
                        new javafx.animation.KeyValue(orbC.centerYProperty(), 520)
                ),
                new KeyFrame(Duration.seconds(12),
                        new javafx.animation.KeyValue(orbA.centerXProperty(), 260),
                        new javafx.animation.KeyValue(orbA.centerYProperty(), 240),
                        new javafx.animation.KeyValue(orbB.centerXProperty(), 840),
                        new javafx.animation.KeyValue(orbB.centerYProperty(), 240),
                        new javafx.animation.KeyValue(orbC.centerXProperty(), 540),
                        new javafx.animation.KeyValue(orbC.centerYProperty(), 460)
                )
        );
        orbAnim.setAutoReverse(true);
        orbAnim.setCycleCount(Timeline.INDEFINITE);
        orbAnim.play();
    }

    private void loadData() {
        Object raw = DataCache.peek("hotel:selectedId");
        UUID id = null;
        if (raw instanceof UUID u) {
            id = u;
        } else if (raw instanceof String s) {
            try { id = UUID.fromString(s); } catch (Exception ignore) {}
        }
        if (id == null) return;
        final UUID finalId = id;
        HotelDetails cached = DataCache.peek("hotel:details:" + finalId);
        if (cached != null) {
            apply(cached);
        }
        new Thread(() -> {
            try {
                HotelDetails details = FileCache.getOrLoad("hotel_details_" + finalId, new TypeReference<HotelDetails>(){}, () -> {
                    try {
                        return api.getHotelDetails(finalId);
                    } catch (ApiClient.ApiException e) {
                        throw new RuntimeException(e);
                    }
                });
                DataCache.put("hotel:details:" + finalId, details);
                javafx.application.Platform.runLater(() -> apply(details));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void apply(HotelDetails d) {
        nameLabel.setText(d.name == null ? "" : d.name);
        locationLabel.setText(d.location == null ? "" : d.location);
        ratingLabel.setText(d.rating == null ? "N/A" : String.format("%.1f/5.0", d.rating.doubleValue()));
        images.clear();
        if (d.images != null) images.addAll(d.images);
        buildThumbnails();
        showImage(0);
        buildNearby(cleanNearby(d.nearby));
        buildFacilities(d.facilities);
        startSlideshow();
    }

    private List<String> cleanNearby(List<String> nearby) {
        if (nearby == null) return List.of();
        return nearby.stream()
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .filter(s -> !s.matches("\\d+"))
                .toList();
    }

    private void buildThumbnails() {
        thumbnails.getChildren().clear();
        for (int i = 0; i < images.size(); i++) {
            int idx = i;
            ImageView iv = new ImageView(new Image(images.get(i), 120, 80, true, true, true));
            iv.getStyleClass().add("thumb");
            iv.setOnMouseClicked(e -> showImage(idx));
            thumbnails.getChildren().add(iv);
        }
    }

    private void showImage(int idx) {
        if (images.isEmpty()) return;
        currentIndex = ((idx % images.size()) + images.size()) % images.size();
        mainImage.setImage(new Image(images.get(currentIndex), 1200, 800, true, true, true));
        imageCounter.setText((currentIndex + 1) + " / " + images.size());
        highlightThumb();
    }

    private void highlightThumb() {
        for (int i = 0; i < thumbnails.getChildren().size(); i++) {
            thumbnails.getChildren().get(i).pseudoClassStateChanged(javafx.css.PseudoClass.getPseudoClass("active"), i == currentIndex);
        }
    }

    private void startSlideshow() {
        if (slideshow != null) slideshow.stop();
        slideshow = new Timeline(new KeyFrame(Duration.seconds(4), e -> showImage(currentIndex + 1)));
        slideshow.setCycleCount(Timeline.INDEFINITE);
        slideshow.play();
    }

    private void buildNearby(List<String> nearby) {
        nearbyGrid.getChildren().clear();
        if (nearby == null) return;
        int col = 0;
        int row = 0;
        for (String n : nearby) {
            HBox pill = new HBox(10);
            pill.getStyleClass().add("nearbyPill");
            Label dot = new Label("•");
            dot.getStyleClass().add("nearbyDot");
            Label text = new Label(n);
            text.getStyleClass().add("nearbyText");
            pill.getChildren().addAll(dot, text);
            pill.setMaxWidth(Double.MAX_VALUE);
            GridPane.setHgrow(pill, javafx.scene.layout.Priority.ALWAYS);
            nearbyGrid.add(pill, col, row);
            col++;
            if (col >= 2) { col = 0; row++; }
        }
    }

    private void buildFacilities(List<String> facilities) {
        facilitiesGrid.getChildren().clear();
        if (facilities == null) facilities = List.of();
        List<String> items = new ArrayList<>(facilities);
        if (items.isEmpty()) {
            items.add("Check-In");
            items.add("Check-Out");
        } else {
            // ensure Check-In and Check-Out appear after the first facility
            int insertPos = Math.min(1, items.size());
            if (items.stream().noneMatch(s -> s.toLowerCase().contains("check-in") || s.toLowerCase().contains("check in"))) {
                items.add(insertPos, "Check-In");
                insertPos++;
            }
            if (items.stream().noneMatch(s -> s.toLowerCase().contains("check-out") || s.toLowerCase().contains("check out"))) {
                items.add(insertPos, "Check-Out");
            }
        }
        for (String f : items) {
            facilitiesGrid.getChildren().add(createFacilityTile(f));
        }
    }

    private VBox createFacilityTile(String name) {
        String label = name == null ? "" : name.trim();
        String key = label.toLowerCase();
        String tileClass = "facilityTileDefault";
        String circleClass = "facilityIconCircle";
        String iconPath = "M3 11h18v2H3v-2z";
        if (key.contains("air")) {
            tileClass = "facilityTileAir";
            iconPath = "M10 2l1 2-1 2 2-1 2 1-1-2 1-2-2 1-2-1zm8 8l-2-1-2 1 1-2-1-2 2 1 2-1-1 2 1 2zm-8 8l-1-2 1-2-2 1-2-1 1 2-1 2 2-1 2 1zm-8-8l2 1 2-1-1 2 1 2-2-1-2 1 1-2-1-2zm8-4a4 4 0 110 8 4 4 0 010-8z";
        } else if (key.contains("check-in")) {
            tileClass = "facilityTileCheckIn";
            iconPath = "M7 12l4-4v3h6v2h-6v3z";
        } else if (key.contains("check-out") || key.contains("check out")) {
            tileClass = "facilityTileCheckOut";
            iconPath = "M17 12l-4 4v-3H7v-2h6V8z";
        } else if (key.contains("accessible") || key.contains("wheel")) {
            tileClass = "facilityTileAccessible";
            iconPath = "M12 4a2 2 0 110 4 2 2 0 010-4zm-1 4.5v2.25l-2.5 1.5.75 1.23 1.75-1.07V16h2v-3.5l2.5-1.5-.75-1.23-1.75 1.07V8.5z";
        } else if (key.contains("elev")) {
            tileClass = "facilityTileElevator";
            iconPath = "M11 16h2V8l2 2V7l-3-3-3 3v3l2-2z";
        } else if (key.contains("highchair") || key.contains("high chair") || key.contains("kids")) {
            tileClass = "facilityTileKids";
            iconPath = "M9 5a2 2 0 114 0 2 2 0 01-4 0zm-2 8l1-4h8l1 4h-2v3h-2v-3h-2v3H9v-3H7z";
        } else if (key.contains("couple")) {
            tileClass = "facilityTileCouple";
            iconPath = "M7 7a2.5 2.5 0 115 0A2.5 2.5 0 017 7zm-3 7c0-2 2.5-3 4-3s4 1 4 3v2H4v-2zm9-7a2.3 2.3 0 114.6 0A2.3 2.3 0 0113 7zm-1.5 7c0-1.7 1.9-2.5 3.5-2.5S19.5 12.3 19.5 14v2H11.5v-2zm2.6-8.8l-.8.8L12.5 5l.8-.8a2 2 0 012.8 0l.8.8-.8.8-1.1-1.2z";
        } else if (key.contains("garden")) {
            tileClass = "facilityTileGarden";
            iconPath = "M7 14c0-1.66 1.57-3 3.5-3H12v-2a3 3 0 016 0v8h-2v-3h-2v3H7v-3z";
        } else if (key.contains("coffee") || key.contains("tea")) {
            tileClass = "facilityTileCoffee";
            iconPath = "M5 6h11v4a4 4 0 01-4 4H9a4 4 0 01-4-4V6zm12 1h1.5a2.5 2.5 0 010 5H17V7z";
        } else if (key.contains("bed")) {
            iconPath = "M4 7h16v6H4V7zm-1 7h2v3h2v-3h10v3h2v-3h2V6a2 2 0 00-2-2H5a2 2 0 00-2 2v8z";
        } else if (key.contains("id")) {
            iconPath = "M4 5h16a2 2 0 012 2v10a2 2 0 01-2 2H4a2 2 0 01-2-2V7a2 2 0 012-2zm0 2v10h16V7H4zm3 2a2 2 0 114 0 2 2 0 01-4 0zm-1 5h6v-1H6v1zm9-5h3v-1h-3v1zm-4 2h7v-1h-7v1zm0 2h7v-1h-7v1z";
        } else if (key.contains("room")) {
            iconPath = "M4 4h16v14h-2v-4H6v4H4V4zm2 6h12V6H6v4zm0 2h12v-2H6v2z";
        } else if (key.contains("garden")) {
            tileClass = "facilityTileGarden";
            iconPath = "M7 14c0-1.66 1.57-3 3.5-3H12v-2a3 3 0 016 0v8h-2v-3h-2v3H7v-3z";
        } else if (key.contains("coffee") || key.contains("tea")) {
            tileClass = "facilityTileCoffee";
            iconPath = "M5 6h11v4a4 4 0 01-4 4H9a4 4 0 01-4-4V6zm12 1h1.5a2.5 2.5 0 010 5H17V7z";
        } else {
            iconPath = "M4 6h16v12H4V6zm2 2v8h12V8H6zm2 2h8v2H8v-2z";
        }
        SVGPath icon = new SVGPath();
        icon.setContent(iconPath);
        icon.getStyleClass().add("facilityTileIcon");
        VBox tile = new VBox(8);
        tile.getStyleClass().addAll("facilityTile", tileClass);
        HBox iconWrap = new HBox(icon);
        iconWrap.getStyleClass().add(circleClass);
        iconWrap.setAlignment(javafx.geometry.Pos.CENTER);
        Label text = new Label(label);
        text.getStyleClass().add("facilityTileText");
        tile.getChildren().addAll(iconWrap, text);
        tile.setAlignment(javafx.geometry.Pos.CENTER);
        return tile;
    }
}
