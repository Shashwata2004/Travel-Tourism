package com.travel.frontend.controller;

import com.travel.frontend.cache.DataCache;
import com.travel.frontend.cache.FileCache;
import com.travel.frontend.controller.DestinationsController.DestinationCard;
import com.travel.frontend.net.ApiClient;
import com.travel.frontend.ui.Navigator;
import com.fasterxml.jackson.core.type.TypeReference;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.DateCell;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Priority;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.shape.SVGPath;
import javafx.geometry.Insets;
import javafx.util.Duration;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.Interpolator;
import javafx.scene.effect.DropShadow;
import javafx.scene.paint.Color;

import java.time.LocalDate;
import java.util.UUID;
import java.util.List;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.stream.Collectors;

/**
 * Controller for the hotel search screen. Handles date restrictions,
 * guest selector overlay, hero copy updates, and navigation wiring.
 */
public class HotelSearchController {

    @FXML private NavbarController navbarController;
    @FXML private Label heroBadgeText;
    @FXML private Label heroTitle;
    @FXML private Label heroSubtitle;
    @FXML private DatePicker checkInPicker;
    @FXML private DatePicker checkOutPicker;
    @FXML private Label guestValue;
    @FXML private Label overlayGuestValue;
    @FXML private StackPane guestOverlay;
    @FXML private Label propertiesCountLabel;
    @FXML private VBox hotelsList;
    @FXML private javafx.scene.control.ComboBox<String> sortBox;
    @FXML private StackPane rootStack;
    @FXML private javafx.scene.control.Button searchButton;

    private static final int MIN_GUESTS = 1;
    private static final int MAX_GUESTS = 12;
    private int guestCount = 2;
    private final ApiClient api = ApiClient.get();
    private final List<HotelCard> hotelCache = new ArrayList<>();
    private UUID currentDestinationId;
    private boolean searchMode = false;
    private LocalDate searchCheckIn;
    private LocalDate searchCheckOut;
    private static final String CACHE_VERSION = "v2";

    @FXML
    private void initialize() {
        if (navbarController != null) {
            navbarController.setActive(NavbarController.ActivePage.DESTINATIONS);
        }
        Object cachedGuests = DataCache.peek("hotel:guestCount");
        if (cachedGuests instanceof Number n) {
            guestCount = Math.max(MIN_GUESTS, n.intValue());
        }
        if (hotelsList != null) {
            hotelsList.setAlignment(Pos.TOP_CENTER);
            hotelsList.setFillWidth(true);
        }
        if (rootStack != null && hotelsList != null) {
            rootStack.setPrefWidth(1200);
            rootStack.setMaxWidth(Double.MAX_VALUE);
            hotelsList.prefWidthProperty().unbind();
            hotelsList.maxWidthProperty().unbind();
            hotelsList.setPrefWidth(1024);
            hotelsList.setMaxWidth(1024);
            hotelsList.setViewOrder(100); // push below overlays
        }
        configureHeroTexts();
        setupDatePickers();
        updateGuestLabels();
        hideGuestsOverlay();
        if (guestOverlay != null) {
            guestOverlay.setViewOrder(-10000); // render above everything
            guestOverlay.toFront();
        }
        if (sortBox != null) {
            sortBox.getSelectionModel().select("Popularity");
            sortBox.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> applySortAndRender());
        }
        loadHotelCount();
        setupSearchButtonAnimation();
    }

    private void configureHeroTexts() {
        DestinationCard card = DataCache.peek("hotel:selected");
        if (card == null) {
            heroBadgeText.setText("Explore Bangladesh");
            return;
        }

        heroBadgeText.setText("Explore " + safe(card.name));
        if (heroTitle != null) {
            heroTitle.setText("Plan your stay in " + safe(card.name));
        }
        if (heroSubtitle != null && card.region != null && !card.region.isBlank()) {
            heroSubtitle.setText("Hand-picked stays across " + card.region + ". Filter dates, adjust guests, and we will surface the perfect hotels soon.");
        }
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private void setupDatePickers() {
        if (checkInPicker == null || checkOutPicker == null) return;

        LocalDate minCheckIn = LocalDate.now().plusDays(1);
        checkInPicker.setDayCellFactory(picker -> disableOutsideRange(picker, minCheckIn, null));
        LocalDate cachedIn = DataCache.peek("hotel:checkIn");
        LocalDate cachedOut = DataCache.peek("hotel:checkOut");
        LocalDate effectiveIn = (cachedIn != null) ? cachedIn : minCheckIn;
        checkInPicker.setValue(effectiveIn);

        refreshCheckOutFactory();
        LocalDate defaultOut = effectiveIn.plusDays(1);
        LocalDate effectiveOut = (cachedOut != null && cachedOut.isAfter(effectiveIn)) ? cachedOut : defaultOut;
        checkOutPicker.setValue(effectiveOut);
        DataCache.put("hotel:checkIn", effectiveIn);
        DataCache.put("hotel:checkOut", effectiveOut);

        checkInPicker.valueProperty().addListener((obs, old, val) -> {
            LocalDate selected = val == null ? LocalDate.now().plusDays(1) : val;
            if (val == null || val.isBefore(minCheckIn)) {
                checkInPicker.setValue(minCheckIn);
                selected = minCheckIn;
            }
            LocalDate maxCheckout = selected.plusDays(14);
            LocalDate currentOut = checkOutPicker.getValue();
            if (currentOut == null || currentOut.isBefore(selected.plusDays(1)) || currentOut.isAfter(maxCheckout)) {
                checkOutPicker.setValue(selected.plusDays(1));
            }
            refreshCheckOutFactory();
        });
    }

    private void refreshCheckOutFactory() {
        if (checkOutPicker == null || checkInPicker == null) return;
        LocalDate checkIn = checkInPicker.getValue();
        LocalDate minCheckout = (checkIn == null ? LocalDate.now().plusDays(2) : checkIn.plusDays(1));
        LocalDate maxCheckout = (checkIn == null ? minCheckout.plusDays(14) : checkIn.plusDays(14));
        checkOutPicker.setDayCellFactory(picker -> disableOutsideRange(picker, minCheckout, maxCheckout));
    }

    private DateCell disableOutsideRange(DatePicker picker, LocalDate minDate, LocalDate maxDate) {
        return new DateCell() {
            @Override
            public void updateItem(LocalDate item, boolean empty) {
                super.updateItem(item, empty);
                boolean tooEarly = minDate != null && item.isBefore(minDate);
                boolean tooLate = maxDate != null && item.isAfter(maxDate);
                boolean disable = empty || tooEarly || tooLate;
                setDisable(disable);
                setStyle(disable ? "-fx-opacity: 0.35;" : "");
            }
        };
    }

    private void updateGuestLabels() {
        if (guestValue != null) {
            guestValue.setText(guestCount + (guestCount == 1 ? " Guest" : " Guests"));
        }
        if (overlayGuestValue != null) {
            overlayGuestValue.setText(String.valueOf(guestCount));
        }
        DataCache.put("hotel:guestCount", guestCount);
    }

    @FXML
    private void toggleGuests() {
        if (guestOverlay == null) return;
        boolean show = !guestOverlay.isVisible();
        guestOverlay.setViewOrder(show ? -10000 : 0);
        guestOverlay.setVisible(show);
        if (show) {
            guestOverlay.toFront();
        }
    }

    private void hideGuestsOverlay() {
        if (guestOverlay != null) {
            guestOverlay.setVisible(false);
        }
    }

    @FXML
    private void closeGuests() {
        hideGuestsOverlay();
    }

    private void loadHotelCount() {
        DestinationCard card = DataCache.peek("hotel:selected");
        if (card == null || card.id == null || propertiesCountLabel == null) return;
        currentDestinationId = card.id;
        // restore search mode if the last view was a search
        Boolean lastSearch = DataCache.peek("hotel:lastSearch");
        if (Boolean.TRUE.equals(lastSearch)) {
            searchMode = true;
            searchCheckIn = DataCache.peek("hotel:checkIn");
            searchCheckOut = DataCache.peek("hotel:checkOut");
        } else {
            searchMode = false;
        }

        String cacheKey = "hotels:list:" + CACHE_VERSION + ":" + card.id;
        List<HotelCard> cached = DataCache.peek(cacheKey);
        if (cached != null) {
            hotelCache.clear();
            hotelCache.addAll(cached);
            propertiesCountLabel.setText(hotelCache.size() + " properties found");
            applySortAndRender();
            return; // show cached list instantly; only refresh on explicit reload/search
        }
        loadHotelsWithCache(card.id);
    }

    private void loadHotelsWithCache(UUID destinationId) {
        if (searchMode) return;
        new Thread(() -> {
            try {
                String key = "hotels_" + CACHE_VERSION + "_" + destinationId;
                List<HotelCard> hotels = FileCache.getOrLoad(
                        key,
                        new TypeReference<List<HotelCard>>(){},
                        () -> {
                            try {
                                return api.getHotelsForDestination(destinationId);
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        });
                hotelCache.clear();
                hotelCache.addAll(hotels);
                DataCache.put("hotels:list:" + CACHE_VERSION + ":" + destinationId, new ArrayList<>(hotels));
                javafx.application.Platform.runLater(() -> {
                    if (propertiesCountLabel != null) {
                        propertiesCountLabel.setText(hotels.size() + " properties found");
                    }
                    applySortAndRender();
                });
            } catch (Exception e) {
                javafx.application.Platform.runLater(() -> {
                    hotelsList.getChildren().setAll(new Label("Failed to load hotels: " + e.getMessage()));
                });
            }
        }).start();
    }

    private void fetchFilteredHotels(UUID destinationId, LocalDate in, LocalDate out) {
        new Thread(() -> {
            try {
                List<HotelCard> hotels = api.getHotelsForDestination(destinationId, in, out);
                scoreForSearch(hotels); // precompute scores once
                hotelCache.clear();
                hotelCache.addAll(hotels);
                javafx.application.Platform.runLater(() -> {
                    DataCache.put("hotels:list:" + CACHE_VERSION + ":" + destinationId, new ArrayList<>(hotels));
                    DataCache.put("hotel:lastSearch", true);
                    if (propertiesCountLabel != null) {
                        propertiesCountLabel.setText(hotels.size() + " properties found");
                    }
                    applySortAndRender();
                });
            } catch (Exception e) {
                javafx.application.Platform.runLater(() -> hotelsList.getChildren().setAll(new Label("Search failed: " + e.getMessage())));
            }
        }).start();
    }

    private void applySortAndRender() {
        if (hotelsList == null) return;
        String option = sortBox != null ? sortBox.getSelectionModel().getSelectedItem() : "Popularity";
        List<HotelCard> sorted = new ArrayList<>(hotelCache);
        if (searchMode) {
            scoreForSearch(sorted);
            sorted.sort(Comparator.comparingDouble((HotelCard h) -> h.score).reversed());
        } else {
            Comparator<HotelCard> byRating = Comparator.comparingDouble(h -> h.rating == null ? 0 : h.rating.doubleValue());
            Comparator<HotelCard> byPrice = Comparator.comparingDouble(this::effectivePrice);
            switch (option == null ? "Popularity" : option) {
                case "Price: Low to High" -> sorted.sort(byPrice);
                case "Price: High to Low" -> sorted.sort(byPrice.reversed());
                default -> sorted.sort(byRating.reversed());
            }
        }
        renderHotels(sorted);
    }

    private void scoreForSearch(List<HotelCard> hotels) {
        double minPrice = hotels.stream().mapToDouble(this::effectivePrice).filter(v -> v > 0 && v < Double.MAX_VALUE).min().orElse(0);
        double maxPrice = hotels.stream().mapToDouble(this::effectivePrice).filter(v -> v > 0 && v < Double.MAX_VALUE).max().orElse(minPrice + 1);
        double priceRange = Math.max(1, maxPrice - minPrice);
        int assumedCap = 2;
        int requiredRooms = Math.max(1, (int) Math.ceil((double) guestCount / assumedCap));

        for (HotelCard h : hotels) {
            double remain = h.availableRooms == null ? 0 : h.availableRooms;
            double availFit = Math.max(0, Math.min(1, remain / requiredRooms));
            double ratingScore = (h.rating == null ? 0 : h.rating.doubleValue()) / 5.0;
            double priceVal = effectivePrice(h);
            double priceScore = priceVal == Double.MAX_VALUE ? 0 : 1 - Math.max(0, Math.min(1, (priceVal - minPrice) / priceRange));
            double popBoost = remain > 5 ? 0.6 : 0.5;
            h.score = 0.35 * availFit + 0.30 * ratingScore + 0.25 * priceScore + 0.10 * popBoost;
        }
    }

    private void renderHotels(List<HotelCard> hotels) {
        hotelsList.getChildren().clear();
        for (HotelCard h : hotels) {
            hotelsList.getChildren().add(createHotelCard(h));
        }
    }

    private javafx.scene.Node createHotelCard(HotelCard h) {
        javafx.scene.layout.HBox card = new javafx.scene.layout.HBox();
        card.getStyleClass().add("hotelCard");
        card.setSpacing(16);
        card.setPrefWidth(0);
        if (hotelsList != null) {
            card.prefWidthProperty().bind(hotelsList.widthProperty().multiply(0.97));
        }
        card.setMaxWidth(Double.MAX_VALUE);

        javafx.scene.image.ImageView img = new javafx.scene.image.ImageView();
        img.setPreserveRatio(false);
        img.setSmooth(true);
        img.fitWidthProperty().bind(card.widthProperty().multiply(0.35));
        try {
            img.setImage(new javafx.scene.image.Image(h.image, 480, 360, true, true, true));
        } catch (Exception ignore) {}
        javafx.scene.layout.StackPane imgWrap = new javafx.scene.layout.StackPane(img);
        imgWrap.getStyleClass().add("hotelCardImage");
        img.fitHeightProperty().bind(imgWrap.heightProperty());
        imgWrap.minWidthProperty().bind(card.widthProperty().multiply(0.35));
        imgWrap.maxWidthProperty().bind(card.widthProperty().multiply(0.35));
        imgWrap.setMinHeight(360);
        imgWrap.minWidthProperty().bind(card.widthProperty().multiply(0.35));
        imgWrap.maxWidthProperty().bind(card.widthProperty().multiply(0.35));

        javafx.scene.layout.VBox content = new javafx.scene.layout.VBox(8);
        content.getStyleClass().add("hotelCardBody");
        content.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(content, Priority.ALWAYS);

        Label name = new Label(h.name);
        name.getStyleClass().add("hotelCardTitle");

        javafx.scene.layout.HBox meta = new javafx.scene.layout.HBox(8);
        Label ratingLabel = new Label(formatRating(h.rating));
        ratingLabel.getStyleClass().add("ratingBadge");

        SVGPath locIcon = new SVGPath();
        locIcon.setContent("M12 2c-3.3137 0-6 2.6863-6 6 0 4.5 6 10 6 10s6-5.5 6-10c0-3.3137-2.6863-6-6-6zm0 8.2c-1.215 0-2.2-.985-2.2-2.2 0-1.215.985-2.2 2.2-2.2 1.215 0 2.2.985 2.2 2.2 0 1.215-.985 2.2-2.2 2.2z");
        locIcon.getStyleClass().add("locationIcon");
        locIcon.setScaleX(0.8);
        locIcon.setScaleY(0.8);
        Label location = new Label(h.location == null ? "" : h.location);
        location.setWrapText(true);
        location.setMaxWidth(520);
        location.getStyleClass().add("hotelLocation");
        HBox locationRow = new HBox(6, locIcon, location);
        locationRow.setAlignment(Pos.CENTER_LEFT);
        meta.getChildren().addAll(ratingLabel, locationRow);

        Label roomsAlert = new Label(h.availableRooms != null && h.availableRooms > 0 && h.availableRooms < 10
                ? h.availableRooms + " Rooms Remaining" : "");
        roomsAlert.getStyleClass().add("roomsAlert");
        roomsAlert.setVisible(!roomsAlert.getText().isBlank());

        javafx.scene.layout.HBox facilities = new javafx.scene.layout.HBox(8);
        facilities.getStyleClass().add("facilitiesRow");
        List<String> facs = h.facilities == null ? List.of() : h.facilities.stream().limit(3).collect(Collectors.toList());
        for (String f : facs) {
            facilities.getChildren().add(createFacilityPill(f));
        }

        double discount = calcDiscount(h.realPrice, h.currentPrice);
        Label discountLabel = new Label(discount > 0 ? ((int) discount) + "% off" : "");
        discountLabel.getStyleClass().add("discountBadge");
        discountLabel.setVisible(discount > 0);

        boolean showStrike = h.realPrice != null && (h.currentPrice == null || h.realPrice.compareTo(h.currentPrice) > 0);
        Label priceLabel = new Label(formatPrice(h.currentPrice));
        priceLabel.getStyleClass().add("hotelPrice");
        Text strike = new Text(showStrike ? formatPrice(h.realPrice) : "");
        strike.getStyleClass().add("strikePrice");
        strike.setStrikethrough(showStrike);

        javafx.scene.layout.VBox priceBox = new javafx.scene.layout.VBox(4);
        priceBox.getChildren().addAll(new Label("Starts from"), strike, priceLabel, new Label("for 1 Night, per room"));

        javafx.scene.control.Button select = new javafx.scene.control.Button("Select");
        select.getStyleClass().add("hotelSelectBtn");
        select.setMinWidth(110);

        content.getChildren().addAll(name, meta, roomsAlert, facilities, discountLabel, priceBox);

        card.getChildren().addAll(imgWrap, content);

        StackPane wrapper = new StackPane(card, select);
        wrapper.getStyleClass().add("hotelCardWrapper");
        StackPane.setAlignment(select, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(select, new Insets(0, 18, 18, 0));

        // entrance animation
        wrapper.setOpacity(0);
        wrapper.setTranslateY(28);
        FadeTransition ft = new FadeTransition(Duration.millis(420), wrapper);
        ft.setToValue(1);
        TranslateTransition tt = new TranslateTransition(Duration.millis(420), wrapper);
        tt.setToY(0);
        new ParallelTransition(ft, tt).play();

        applyCardHover(wrapper, img);
        Runnable goDetails = () -> {
            if (h.id != null) {
                // Persist the latest picker/guest values even if Search was not clicked
                LocalDate in = checkInPicker != null ? checkInPicker.getValue() : null;
                LocalDate out = checkOutPicker != null ? checkOutPicker.getValue() : null;
                if (in == null) in = LocalDate.now().plusDays(1);
                if (out == null || !in.isBefore(out)) out = in.plusDays(1);
                DataCache.put("hotel:checkIn", in);
                DataCache.put("hotel:checkOut", out);
                DataCache.put("hotel:guestCount", guestCount);
                DataCache.put("hotel:destId", currentDestinationId);
                // Drop stale caches so details reload fresh availability
                FileCache.remove("hotel_details_" + CACHE_VERSION + "_" + h.id);
                DataCache.remove("hotel:details:" + CACHE_VERSION + ":" + h.id);

                try {
                    java.util.UUID uid = java.util.UUID.fromString(h.id);
                    DataCache.put("hotel:selectedId", uid);
                } catch (IllegalArgumentException ex) {
                    DataCache.put("hotel:selectedId", h.id);
                }
                Navigator.goHotelDetails();
            }
        };
        wrapper.setOnMouseClicked(evt -> goDetails.run());
        select.setOnAction(evt -> {
            evt.consume();
            goDetails.run();
        });
        return wrapper;
    }

    private Label badge(String text, String styleClass) {
        Label l = new Label(text);
        l.getStyleClass().add(styleClass);
        return l;
    }

    private HBox createFacilityPill(String name) {
        String label = name == null ? "" : name.trim();
        String key = label.toLowerCase();
        String extraClass = "facilityDefault";
        SVGPath icon = new SVGPath();
        if (key.contains("air")) {
            icon.setContent("M10 2l1 2-1 2 2-1 2 1-1-2 1-2-2 1-2-1zm8 8l-2-1-2 1 1-2-1-2 2 1 2-1-1 2 1 2zm-8 8l-1-2 1-2-2 1-2-1 1 2-1 2 2-1 2 1zm-8-8l2 1 2-1-1 2 1 2-2-1-2 1 1-2-1-2zm8-4a4 4 0 110 8 4 4 0 010-8z");
            extraClass = "facilityAir";
        } else if (key.contains("child")) {
            icon.setContent("M8 5a3 3 0 116 0 3 3 0 01-6 0zm-2 9c0-2.21 3-3 5-3s5 .79 5 3v2H6v-2zm-2.5-4.5a1.5 1.5 0 113 0 1.5 1.5 0 01-3 0zM3 15v2H1.5v-1c0-1.38 1.12-2.5 2.5-2.5h.17c-.1.32-.17.66-.17 1.02zM16.5 9a1.5 1.5 0 11-3 0 1.5 1.5 0 013 0zM18.5 16v1H17v-2c0-.36-.07-.7-.17-1.02h.17c1.38 0 2.5 1.12 2.5 2.5z");
            extraClass = "facilityKids";
        } else if (key.contains("bath")) {
            icon.setContent("M6 3a3 3 0 00-3 3v5h1.5v2a2.5 2.5 0 105 0v-2h7V9H5.5V6a1.5 1.5 0 113 0v1H10V6a3 3 0 00-3-3z");
            extraClass = "facilityBath";
        } else if (key.contains("couple")) {
            icon.setContent("M7 7a2.5 2.5 0 115 0A2.5 2.5 0 017 7zm-3 7c0-2 2.5-3 4-3s4 1 4 3v2H4v-2zm9-7a2.3 2.3 0 114.6 0A2.3 2.3 0 0113 7zm-1.5 7c0-1.7 1.9-2.5 3.5-2.5S19.5 12.3 19.5 14v2H11.5v-2zm2.6-8.8l-.8.8L12.5 5l.8-.8a2 2 0 012.8 0l.8.8-.8.8-1.1-1.2z");
            extraClass = "facilityCouple";
        } else {
            icon.setContent("M3 11h18v2H3v-2zm0-6h18v2H3V5zm0 12h18v2H3v-2z");
            extraClass = "facilityDefault";
        }
        icon.getStyleClass().add("facilityIcon");
        HBox pill = new HBox(8, icon, new Label(label));
        pill.getStyleClass().addAll("facilityPill", extraClass);
        pill.setAlignment(Pos.CENTER_LEFT);
        return pill;
    }

    private String formatPrice(java.math.BigDecimal p) {
        if (p == null) return "BDT --";
        return "BDT " + p.stripTrailingZeros().toPlainString();
    }

    private double effectivePrice(HotelCard h) {
        if (h.currentPrice != null) return h.currentPrice.doubleValue();
        if (h.realPrice != null) return h.realPrice.doubleValue();
        return Double.MAX_VALUE;
    }

    private String formatRating(java.math.BigDecimal r) {
        if (r == null) return "N/A";
        double val = r.doubleValue();
        return String.format("%.1f/5.0", val);
    }

    private double calcDiscount(java.math.BigDecimal real, java.math.BigDecimal current) {
        if (real == null || current == null) return 0;
        if (current.compareTo(real) >= 0) return 0;
        double r = real.doubleValue();
        double c = current.doubleValue();
        return Math.round((1 - (c / r)) * 100);
    }

    private void applyCardHover(javafx.scene.Node card, javafx.scene.image.ImageView img) {
        card.setOnMouseEntered(e -> {
            javafx.animation.TranslateTransition lift = new javafx.animation.TranslateTransition(javafx.util.Duration.millis(250), card);
            lift.setToY(-8);
            lift.setInterpolator(javafx.animation.Interpolator.EASE_BOTH);
            lift.play();
            if (img != null) {
                javafx.animation.ScaleTransition st = new javafx.animation.ScaleTransition(javafx.util.Duration.millis(300), img);
                st.setToX(1.03);
                st.setToY(1.03);
                st.setInterpolator(javafx.animation.Interpolator.EASE_BOTH);
                st.play();
            }
        });
        card.setOnMouseExited(e -> {
            javafx.animation.TranslateTransition drop = new javafx.animation.TranslateTransition(javafx.util.Duration.millis(250), card);
            drop.setToY(0);
            drop.setInterpolator(javafx.animation.Interpolator.EASE_BOTH);
            drop.play();
            if (img != null) {
                javafx.animation.ScaleTransition st = new javafx.animation.ScaleTransition(javafx.util.Duration.millis(300), img);
                st.setToX(1.0);
                st.setToY(1.0);
                st.setInterpolator(javafx.animation.Interpolator.EASE_BOTH);
                st.play();
            }
        });
    }

    // DTO for hotel cards
    public static class HotelCard {
        public String id;
        public String name;
        public java.math.BigDecimal rating;
        public String location;
        public String image;
        public List<String> facilities;
        public java.math.BigDecimal realPrice;
        public java.math.BigDecimal currentPrice;
        public Integer availableRooms;
        public double score;
    }

    @FXML
    private void consumeGuestCard(MouseEvent event) {
        event.consume();
    }

    @FXML
    private void incrementGuests() {
        guestCount = Math.min(MAX_GUESTS, guestCount + 1);
        updateGuestLabels();
    }

    @FXML
    private void decrementGuests() {
        guestCount = Math.max(MIN_GUESTS, guestCount - 1);
        updateGuestLabels();
    }

    private void setupSearchButtonAnimation() {
        if (searchButton == null) return;
        DropShadow glow = new DropShadow();
        glow.setColor(Color.web("#f59e0b"));
        glow.setRadius(18);
        glow.setSpread(0.2);

        searchButton.setOnMouseEntered(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(160), searchButton);
            st.setToX(1.05); st.setToY(1.05);
            st.setInterpolator(Interpolator.EASE_BOTH);
            st.playFromStart();
            searchButton.setEffect(glow);
        });
        searchButton.setOnMouseExited(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(160), searchButton);
            st.setToX(1.0); st.setToY(1.0);
            st.setInterpolator(Interpolator.EASE_BOTH);
            st.playFromStart();
            searchButton.setEffect(null);
        });
        searchButton.setOnMousePressed(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(120), searchButton);
            st.setToX(0.97); st.setToY(0.97);
            st.setInterpolator(Interpolator.EASE_BOTH);
            st.playFromStart();
        });
        searchButton.setOnMouseReleased(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(140), searchButton);
            st.setToX(1.03); st.setToY(1.03);
            st.setInterpolator(Interpolator.EASE_BOTH);
            st.playFromStart();
        });
    }

    @FXML
    private void handleSearch() {
        DestinationCard card = DataCache.peek("hotel:selected");
        if (card == null || card.id == null) return;
        LocalDate in = checkInPicker.getValue();
        LocalDate out = checkOutPicker.getValue();
        if (in == null || out == null || !in.isBefore(out)) {
            Alert alert = new Alert(Alert.AlertType.WARNING, "Select valid check-in and check-out dates.");
            alert.showAndWait();
            return;
        }
        this.searchMode = true;
        this.searchCheckIn = in;
        this.searchCheckOut = out;
        DataCache.put("hotel:guestCount", guestCount);
        DataCache.put("hotel:checkIn", in);
        DataCache.put("hotel:checkOut", out);
        if (propertiesCountLabel != null) {
            propertiesCountLabel.setText("Searching...");
        }
        fetchFilteredHotels(card.id, in, out);
    }

    @FXML
    private void goBack() {
        Navigator.goDestinations();
    }

    @FXML
    private void reloadHotels() {
        DestinationCard card = DataCache.peek("hotel:selected");
        UUID destId = currentDestinationId != null ? currentDestinationId : (card != null ? card.id : null);
        if (destId == null) return;

        // Clear caches so we fetch fresh data
        DataCache.remove("hotels:list:" + CACHE_VERSION + ":" + destId);
        FileCache.remove("hotels_" + CACHE_VERSION + "_" + destId);
        // purge legacy keys too
        DataCache.remove("hotels:list:" + destId);
        FileCache.remove("hotels_" + destId);
        DataCache.remove("hotel:lastSearch");

        if (propertiesCountLabel != null) {
            propertiesCountLabel.setText("Refreshing...");
        }

        if (searchMode && searchCheckIn != null && searchCheckOut != null) {
            fetchFilteredHotels(destId, searchCheckIn, searchCheckOut);
        } else {
            loadHotelsWithCache(destId);
        }
    }
}
