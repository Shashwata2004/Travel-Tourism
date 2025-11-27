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
import javafx.scene.Node;
import javafx.scene.layout.VBox;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.GridPane;
import javafx.scene.control.Separator;
import javafx.util.Duration;
import javafx.scene.shape.SVGPath;
import javafx.scene.control.Button;
import javafx.scene.layout.Priority;
import javafx.geometry.Insets;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.animation.FadeTransition;
import javafx.animation.TranslateTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.Interpolator;
import javafx.animation.ScaleTransition;
import javafx.scene.text.Text;
import com.travel.frontend.cache.FileCache;

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
    @FXML private Label roomsMetaLabel;
    @FXML private Label floorsMetaLabel;
    @FXML private Label descriptionText;
    @FXML private GridPane nearbyGrid;
    @FXML private TilePane facilitiesGrid;
    @FXML private VBox roomsContainer;
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
    private static final String CACHE_VERSION = "v2";
    private UUID currentHotelId;

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
        currentHotelId = id;
        final UUID finalId = id;
        new Thread(() -> {
            try {
                HotelDetails details = DataCache.getOrLoad("hotel:details:" + finalId, () ->
                        FileCache.getOrLoad("hotel_details_" + CACHE_VERSION + "_" + finalId, new TypeReference<HotelDetails>(){}, () -> {
                            try {
                                return api.getHotelDetails(finalId);
                            } catch (ApiClient.ApiException e) {
                                throw new RuntimeException(e);
                            }
                        })
                );
                // If rooms are missing, force a fresh fetch and refresh caches
                if (details == null || details.rooms == null || details.rooms.isEmpty()) {
                    try {
                        HotelDetails fresh = api.getHotelDetails(finalId);
                        DataCache.put("hotel:details:" + finalId, fresh);
                        FileCache.put("hotel_details_" + CACHE_VERSION + "_" + finalId, fresh);
                        details = fresh;
                    } catch (ApiClient.ApiException ignore) {
                        // stick with cached if backend not reachable
                    }
                }
                final HotelDetails toApply = details;
                javafx.application.Platform.runLater(() -> apply(toApply));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    @FXML
    private void reload() {
        if (currentHotelId == null) return;
        // Clear caches for this hotel then fetch fresh
        DataCache.remove("hotel:details:" + currentHotelId);
        FileCache.remove("hotel_details_" + CACHE_VERSION + "_" + currentHotelId);
        loadData(); // reuse flow
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
        buildDescription(d);
        buildRooms(d.rooms);
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
            ImageView iv = new ImageView(loadImage(images.get(i), 120, 80));
            iv.getStyleClass().add("thumb");
            iv.setOnMouseClicked(e -> showImage(idx));
            thumbnails.getChildren().add(iv);
        }
    }

    private void showImage(int idx) {
        if (images.isEmpty()) return;
        currentIndex = ((idx % images.size()) + images.size()) % images.size();
        mainImage.setImage(loadImage(images.get(currentIndex), 1200, 800));
        imageCounter.setText((currentIndex + 1) + " / " + images.size());
        highlightThumb();
    }

    private Image loadImage(String url, double w, double h) {
        if (url == null || url.isBlank()) {
            return new Image("https://dummyimage.com/1200x800/edf2f7/9ca3af&text=Image+not+available", w, h, true, true, true);
        }
        try {
            return DataCache.getOrLoad("img:" + w + "x" + h + ":" + url, () -> new Image(url, w, h, true, true, true));
        } catch (Exception e) {
            return new Image("https://dummyimage.com/1200x800/edf2f7/9ca3af&text=Image+not+available", w, h, true, true, true);
        }
    }

    private void buildRooms(List<HotelDetails.RoomInfo> rooms) {
        if (roomsContainer == null) return;
        roomsContainer.getChildren().clear();
        if (rooms == null || rooms.isEmpty()) {
            Label none = new Label("No rooms available.");
            none.getStyleClass().add("locationText");
            roomsContainer.getChildren().add(none);
            return;
        }
        for (HotelDetails.RoomInfo room : rooms) {
            roomsContainer.getChildren().add(createRoomCard(room));
        }
    }

    private Pane createRoomCard(HotelDetails.RoomInfo room) {
        BorderPane card = new BorderPane();
        card.getStyleClass().add("roomCard");
        card.setPadding(new Insets(12));
        card.setMinHeight(380);

        // Gallery (left)
        VBox gallery = new VBox(8);
        gallery.getStyleClass().add("roomGallery");
        ImageView main = new ImageView(loadImage(firstImage(room), 520, 320));
        main.setFitWidth(520);
        main.setFitHeight(320);
        main.setPreserveRatio(true);
        HBox thumbs = new HBox(6);
        List<String> imgs = roomImages(room);
        for (int i = 0; i < imgs.size(); i++) {
            String url = imgs.get(i);
            ImageView iv = new ImageView(loadImage(url, 96, 64));
            iv.getStyleClass().add("roomThumb");
            int idx = i;
            iv.setOnMouseClicked(e -> main.setImage(loadImage(imgs.get(idx), 520, 320)));
            thumbs.getChildren().add(iv);
        }
        gallery.getChildren().addAll(main, thumbs);

        // Right side (details + price)
        VBox right = new VBox(10);
        right.setPadding(new Insets(6, 4, 6, 12));

        HBox titleRow = new HBox(10);
        Label title = new Label(room.name == null ? "Room" : room.name);
        title.getStyleClass().add("roomTitle");
        titleRow.getChildren().add(title);

        VBox metaRow = new VBox(8);
        metaRow.getChildren().add(metaChip(safe(room.bedType), "bedMeta", "M4 10V7a2 2 0 0 1 2-2h6a2 2 0 0 1 2 2v3h2.5a1.5 1.5 0 0 1 1.5 1.5V17H20v2h-2v-2H6v2H4v-2H3v-3.5A1.5 1.5 0 0 1 4.5 10H7v-1h10v1h-5z"));
        metaRow.getChildren().add(metaChip("Maximum Room Capacity: " + safeInt(room.maxGuests), "capMeta", "M12 12a3 3 0 1 0-3-3 3 3 0 0 0 3 3Zm-7 7v-1a4 4 0 0 1 4-4h6a4 4 0 0 1 4 4v1Z"));

        Label remaining = new Label(remainingText(room));
        remaining.getStyleClass().add("remainingCapsule");
        VBox.setMargin(remaining, new Insets(12, 0, 12, 0));

        FlowPane facilities = new FlowPane(8, 8);
        facilities.getChildren().addAll(facilityChips(room.facilities));
        facilities.getStyleClass().add("roomFacilities");

        VBox priceCard = new VBox(6);
        priceCard.getStyleClass().add("roomPriceCard");
        if (room.realPrice != null && room.currentPrice != null) {
            double rp = room.realPrice.doubleValue();
            double cp = room.currentPrice.doubleValue();
            int off = (rp > 0) ? (int) Math.round((1 - (cp / rp)) * 100) : 0;
            Label offBadge = new Label(off + "% off");
            offBadge.getStyleClass().add("discountBadge");
            priceCard.getChildren().add(offBadge);
            // bounce animation to draw attention
            TranslateTransition bounce = new TranslateTransition(Duration.millis(900), offBadge);
            bounce.setFromY(0);
            bounce.setToY(-6);
            bounce.setAutoReverse(true);
            bounce.setCycleCount(TranslateTransition.INDEFINITE);
            bounce.setInterpolator(Interpolator.EASE_BOTH);
            bounce.play();
        }
        if (room.realPrice != null) {
            Text real = new Text("BDT " + room.realPrice);
            real.setStrikethrough(true);
            real.getStyleClass().add("realPrice");
            priceCard.getChildren().add(real);
        }
        Label current = new Label("BDT " + (room.currentPrice != null ? room.currentPrice : "-"));
        current.getStyleClass().add("currentPrice");
        priceCard.getChildren().add(current);

        Button addBtn = new Button("Add Room");
        addBtn.getStyleClass().add("addRoomBtn");
        addBtn.setMaxWidth(Double.MAX_VALUE);
        priceCard.getChildren().add(addBtn);

        right.getChildren().addAll(titleRow, metaRow, remaining, facilities, priceCard);
        VBox.setVgrow(facilities, Priority.ALWAYS);

        card.setLeft(gallery);
        card.setCenter(right);
        applyCardAnimations(card);
        return card;
    }

    private String remainingText(HotelDetails.RoomInfo room) {
        Integer rem = room.remainingRooms != null ? room.remainingRooms : room.totalRooms;
        if (rem == null) return "Rooms remaining: N/A";
        return rem + (rem == 1 ? " room remaining" : " rooms remaining");
    }

    private String safe(String s) { return (s == null || s.isBlank()) ? "N/A" : s; }
    private String safeInt(Integer i) { return i == null ? "N/A" : i.toString(); }

    private List<String> roomImages(HotelDetails.RoomInfo r) {
        List<String> list = new ArrayList<>();
        if (r.image1 != null && !r.image1.isBlank()) list.add(r.image1);
        if (r.image2 != null && !r.image2.isBlank()) list.add(r.image2);
        if (r.image3 != null && !r.image3.isBlank()) list.add(r.image3);
        if (r.image4 != null && !r.image4.isBlank()) list.add(r.image4);
        return list.isEmpty() ? List.of("https://dummyimage.com/1200x800/edf2f7/9ca3af&text=Room+image") : list;
    }

    private String firstImage(HotelDetails.RoomInfo r) {
        List<String> imgs = roomImages(r);
        return imgs.get(0);
    }

    private Node metaChip(String text, String styleClass, String svgPath) {
        HBox box = new HBox(8);
        box.getStyleClass().add(styleClass);
        SVGPath icon = new SVGPath();
        icon.setContent(svgPath);
        icon.getStyleClass().add("metaIcon");
        Label l = new Label(text);
        l.getStyleClass().add(styleClass + "Text");
        box.getChildren().addAll(icon, l);
        return box;
    }

    private List<Node> facilityChips(String facilitiesStr) {
        List<Node> chips = new ArrayList<>();
        if (facilitiesStr == null || facilitiesStr.isBlank()) {
            chips.add(makeChip("Facility info not available"));
            return chips;
        }
        String[] parts = facilitiesStr.split(",");
        for (String p : parts) {
            String v = p.trim();
            if (!v.isEmpty()) chips.add(makeChip(v));
        }
        if (chips.isEmpty()) chips.add(makeChip("Facility info not available"));
        return chips;
    }

    private Node makeChip(String text) {
        HBox chip = new HBox(8);
        chip.getStyleClass().add("facilityChip");
        SVGPath tick = new SVGPath();
        tick.setContent("M5 13l4 4L19 7");
        tick.getStyleClass().add("facilityTick");
        Label label = new Label(text);
        label.getStyleClass().add("facilityLabel");
        chip.getChildren().addAll(tick, label);
        return chip;
    }

    private void applyCardAnimations(Pane card) {
        card.setOpacity(0);
        card.setTranslateY(16);
        FadeTransition fade = new FadeTransition(Duration.millis(420), card);
        fade.setFromValue(0);
        fade.setToValue(1);
        TranslateTransition rise = new TranslateTransition(Duration.millis(420), card);
        rise.setFromY(16);
        rise.setToY(0);
        rise.setInterpolator(Interpolator.EASE_OUT);
        new ParallelTransition(fade, rise).play();

        card.setOnMouseEntered(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(160), card);
            st.setToX(1.02);
            st.setToY(1.02);
            st.play();
        });
        card.setOnMouseExited(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(160), card);
            st.setToX(1.0);
            st.setToY(1.0);
            st.play();
        });
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

    private void buildDescription(HotelDetails d) {
        if (roomsMetaLabel != null) {
            String roomsText = d.roomsCount == null ? "Rooms: N/A" : "Rooms: " + d.roomsCount;
            roomsMetaLabel.setText(roomsText);
        }
        if (floorsMetaLabel != null) {
            String floorsText = d.floorsCount == null ? "Floors: N/A" : "Floors: " + d.floorsCount;
            floorsMetaLabel.setText(floorsText);
        }
        if (descriptionText != null) {
            String body = (d.description == null || d.description.isBlank())
                    ? "No description available."
                    : d.description.trim();
            descriptionText.setText(body);
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
