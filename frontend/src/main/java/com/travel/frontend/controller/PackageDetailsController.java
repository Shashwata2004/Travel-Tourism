/* Opens a pop-up window for a specific package, fetches its full description,
   and hands off to the booking dialog when someone clicks “Book Now.”
   Uses cached API responses so re-opening the same package feels instant. */
package com.travel.frontend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.travel.frontend.cache.DataCache;
import com.travel.frontend.net.ApiClient;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.geometry.Pos;
import javafx.scene.shape.Rectangle;
import javafx.animation.ScaleTransition;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.animation.*;
import javafx.util.Duration;
import java.util.List;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URL;
import java.util.UUID;

public class PackageDetailsController {
    private static final String CACHE_VERSION = "v2";
    @FXML private Label nameLabel;
    @FXML private Label locationLabel;
    @FXML private Label priceLabel;
    @FXML private Label durationLabel;
    @FXML private Label groupLabel;
    @FXML private Label overviewText;
    @FXML private Label locationPointsText;
    @FXML private VBox timingContainer;
    @FXML private ImageView mainImage;
    @FXML private ImageView thumbMain;
    @FXML private ImageView thumbAlt;
    @FXML private ImageView thumb3;
    @FXML private ImageView thumb4;
    @FXML private ImageView thumb5;
    @FXML private Pane blobLayer;
    @FXML private Pane particleLayer;
    @FXML private StackPane detailRoot;
    @FXML private StackPane heroContainer;
    @FXML private VBox heroColumn;
    @FXML private VBox infoCardBox;
    @FXML private VBox pricePanel;
    @FXML private Pane detailShell;
    @FXML private Pane topBar;

    private final ApiClient api = ApiClient.get();
    private final ObjectMapper mapper = new ObjectMapper();
    private UUID packageId;

    /* Entry point from the packages list: stores the selected id, loads the
       FXML layout, and displays a modal Stage sized for content-heavy tabs. */
    public static void open(UUID packageId) {
        try {
            // Set selected package id before loading so initialize() can read it
            PackageDetailsState.pendingPackageId = packageId;
            URL url = PackageDetailsController.class.getResource("/fxml/package_details.fxml");
            Parent root = FXMLLoader.load(url);
            Stage s = new Stage();
            s.setTitle("Package Details");
            s.setScene(new Scene(root, 1200, 720));
            s.initModality(Modality.APPLICATION_MODAL);
            s.show();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /* Loads the selected package id, then fetches details on a new Thread,
       caching the JSON response via DataCache and updating labels on the FX thread. */
    @FXML
    private void initialize() {
        this.packageId = PackageDetailsState.pendingPackageId;
        if (this.packageId == null) return;

        playEntryAnimations();
        applyImageClip();
        attachHeroHover();
        animatePricePanel();
        animateBlobsAndParticles();

        new Thread(() -> {
            try {
                PackageDetailsVM vm = DataCache.getOrLoad("pkg:" + CACHE_VERSION + ":" + packageId, () -> {
                    var res = api.rawGet("/packages/" + packageId, true);
                    if (res.statusCode() != 200) throw new ApiClient.ApiException("Failed to load package");
                    return mapper.readValue(res.body(), PackageDetailsVM.class);
                });
                Platform.runLater(() -> fill(vm));
            } catch (Exception e) {
                Platform.runLater(() -> overviewText.setText("Failed to load: " + e.getMessage()));
            }
        }).start();
    }

    /* Moves fields from PackageDetailsVM into labels so the UI reflects the
       latest info (name, location, overview, etc.). */
    private void fill(PackageDetailsVM vm) {
        if (nameLabel != null) nameLabel.setText(vm.name);
        if (locationLabel != null) locationLabel.setText(vm.location);
        if (priceLabel != null) priceLabel.setText("BDT " + vm.basePrice);
        if (durationLabel != null) durationLabel.setText(n(vm.timing).isBlank() ? "3 Days, 2 Nights" : vm.timing);
        if (groupLabel != null) groupLabel.setText(n(vm.groupSize).isBlank() ? "2-6 People" : vm.groupSize);
        overviewText.setText(n(vm.overview));
        locationPointsText.setText(n(vm.locationPoints));
        renderItinerary(vm);
        String[] imgs = new String[] {
                n(vm.image1), n(vm.image2), n(vm.image3),
                n(vm.image4), n(vm.image5),
                n(vm.destImageUrl), n(vm.hotelImageUrl)
        };
        System.out.println("[PackageDetails] image candidates=" + java.util.Arrays.toString(imgs));
        String primary = null;
        for (String u : imgs) { if (u != null && !u.isBlank()) { primary = u; break; } }
        loadImage(mainImage, primary);

        ImageView[] thumbs = new ImageView[] { thumbMain, thumbAlt, thumb3, thumb4, thumb5 };
        int idx = 0;
        for (String u : imgs) {
            if (idx >= thumbs.length) break;
            if (u == null || u.isBlank()) continue;
            loadImage(thumbs[idx], u);
            idx++;
        }
        // if main image still empty, use first thumb
        if ((mainImage.getImage() == null) && thumbMain.getImage() != null) {
            mainImage.setImage(thumbMain.getImage());
        }
        if (mainImage.getImage() == null) {
            System.out.println("[PackageDetails] No image loaded for package " + vm.id + ". Check URLs.");
            overviewText.setText("No images available for this package. Please verify URLs in admin.");
        }
    }

    /* Button handler bridging to the booking dialog, sending through the id and
       price text gathered on this screen. */
    @FXML
    private void onBookNow() {
        if (packageId == null) {
            javafx.scene.control.Alert a = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.WARNING, "Package not loaded yet. Please try again.");
            a.showAndWait();
            return;
        }
        BookingDialogController.open(packageId, nameLabel.getText(), priceLabel.getText());
    }

    @FXML
    private void showMain() {
        if (thumbMain != null && thumbMain.getImage() != null && mainImage != null) {
            mainImage.setImage(thumbMain.getImage());
        }
    }

    @FXML
    private void showAlt() {
        if (thumbAlt != null && thumbAlt.getImage() != null && mainImage != null) {
            mainImage.setImage(thumbAlt.getImage());
        }
    }

    @FXML private void show3() { swapFromThumb(thumb3); }
    @FXML private void show4() { swapFromThumb(thumb4); }
    @FXML private void show5() { swapFromThumb(thumb5); }

    private void swapFromThumb(ImageView thumb) {
        if (thumb != null && thumb.getImage() != null && mainImage != null) {
            mainImage.setImage(thumb.getImage());
        }
    }

    private void applyImageClip() {
        if (mainImage == null) return;
        Rectangle clip = new Rectangle();
        clip.widthProperty().bind(mainImage.fitWidthProperty());
        clip.heightProperty().bind(mainImage.fitHeightProperty());
        clip.setArcWidth(32);
        clip.setArcHeight(32);
        mainImage.setClip(clip);
    }

    private void attachHeroHover() {
        if (heroContainer == null) return;
        heroContainer.setOnMouseEntered(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(200), heroContainer);
            st.setToX(1.02); st.setToY(1.02);
            st.play();
        });
        heroContainer.setOnMouseExited(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(200), heroContainer);
            st.setToX(1.0); st.setToY(1.0);
            st.play();
        });
    }

    private void animatePricePanel() {
        if (pricePanel == null) return;
        Timeline tl = new Timeline();
        tl.setCycleCount(Timeline.INDEFINITE);
        double durationMs = 3000;
        for (int i = 0; i <= 48; i++) {
            double frac = i / 48.0;
            double millis = frac * durationMs;
            tl.getKeyFrames().add(new KeyFrame(Duration.millis(millis), e -> setPriceGradient(frac)));
        }
        tl.setAutoReverse(true);
        tl.play();
    }

    private void setPriceGradient(double frac) {
        double wave = Math.sin(frac * Math.PI * 2); // -1..1
        double center = 0.15 + (0.7 * ((wave + 1) / 2)); // 0.15..0.85
        double span = 0.35;
        double start = Math.max(0, center - span);
        double end = Math.min(1, center + span);

        Color a = Color.web("#5b8dff");
        Color b = Color.web("#4f46e5");
        Color c = Color.web("#1fb6ff");
        double mix = 0.5 + (wave * 0.5); // 0..1
        Color startColor = a.interpolate(b, mix);
        Color midColor = startColor.interpolate(c, 0.6);
        Color endColor = b.interpolate(c, 0.5 * (1 - Math.cos(frac * Math.PI * 2)));

        LinearGradient lg = new LinearGradient(
                start, 0, end, 1,
                true, CycleMethod.NO_CYCLE,
                new Stop(0, startColor),
                new Stop(0.45, midColor),
                new Stop(1, endColor)
        );
        BackgroundFill fill = new BackgroundFill(lg, new javafx.scene.layout.CornerRadii(16), Insets.EMPTY);
        pricePanel.setBackground(new Background(fill));
        pricePanel.setBorder(new javafx.scene.layout.Border(new javafx.scene.layout.BorderStroke(
                Color.web("rgba(255,255,255,0.24)"),
                javafx.scene.layout.BorderStrokeStyle.SOLID,
                new javafx.scene.layout.CornerRadii(16),
                new javafx.scene.layout.BorderWidths(1)
        )));
        pricePanel.setPadding(new Insets(10,12,12,12));
    }

    // --- Animations ---
    private void playEntryAnimations() {
        if (detailRoot != null) {
            detailRoot.setOpacity(0);
            FadeTransition f = new FadeTransition(Duration.millis(600), detailRoot);
            f.setFromValue(0); f.setToValue(1);
            f.play();
        }
        // top bar fade/slide
        if (topBar != null) {
            topBar.setOpacity(0);
            topBar.setTranslateY(-15);
            FadeTransition f = new FadeTransition(Duration.millis(550), topBar);
            f.setFromValue(0); f.setToValue(1);
            TranslateTransition t = new TranslateTransition(Duration.millis(550), topBar);
            t.setFromY(-15); t.setToY(0);
            new ParallelTransition(f, t).play();
        }
        // left column slide in from left
        if (heroColumn != null) {
            heroColumn.setOpacity(0);
            heroColumn.setTranslateX(-40);
            FadeTransition f = new FadeTransition(Duration.millis(650), heroColumn);
            f.setFromValue(0); f.setToValue(1);
            TranslateTransition t = new TranslateTransition(Duration.millis(650), heroColumn);
            t.setFromX(-40); t.setToX(0);
            new ParallelTransition(f, t).play();
        }
        // right info card slide in from right
        if (infoCardBox != null) {
            infoCardBox.setOpacity(0);
            infoCardBox.setTranslateX(40);
            FadeTransition f = new FadeTransition(Duration.millis(650), infoCardBox);
            f.setFromValue(0); f.setToValue(1);
            TranslateTransition t = new TranslateTransition(Duration.millis(650), infoCardBox);
            t.setFromX(40); t.setToX(0);
            new ParallelTransition(f, t).play();
        }
    }

    private void animateBlobsAndParticles() {
        if (blobLayer != null) {
            for (javafx.scene.Node child : blobLayer.getChildren()) {
                TranslateTransition tt = new TranslateTransition(Duration.seconds(18 + Math.random()*4), child);
                tt.setFromX(0); tt.setToX((Math.random() * 120) - 60);
                tt.setFromY(0); tt.setToY((Math.random() * 80) - 40);
                tt.setAutoReverse(true);
                tt.setCycleCount(TranslateTransition.INDEFINITE);
                tt.setInterpolator(Interpolator.EASE_BOTH);
                tt.play();
            }
        }
        if (particleLayer != null) {
            particleLayer.getChildren().clear();
            for (int i = 0; i < 20; i++) {
                javafx.scene.shape.Circle c = new javafx.scene.shape.Circle(2, javafx.scene.paint.Color.web("#7c8cf5", 0.4));
                c.setCenterX(Math.random() * 1200);
                c.setCenterY(Math.random() * 720);
                particleLayer.getChildren().add(c);
                TranslateTransition tt = new TranslateTransition(Duration.seconds(3 + Math.random()*4), c);
                tt.setFromY(0); tt.setToY(-30);
                tt.setAutoReverse(true);
                tt.setCycleCount(TranslateTransition.INDEFINITE);
                tt.setDelay(Duration.seconds(Math.random()*5));
                tt.setInterpolator(Interpolator.EASE_BOTH);
                FadeTransition f = new FadeTransition(Duration.seconds(3 + Math.random()*4), c);
                f.setFromValue(0.2); f.setToValue(0.8);
                f.setAutoReverse(true);
                f.setCycleCount(FadeTransition.INDEFINITE);
                f.play();
                tt.play();
            }
        }
    }

    private static String n(String s) { return s == null ? "" : s; }

    private void renderItinerary(PackageDetailsVM vm) {
        if (timingContainer == null) return;
        timingContainer.getChildren().clear();
        if (vm.itinerary == null || vm.itinerary.isEmpty()) {
            Label placeholder = new Label("No itinerary provided yet. Check back soon.");
            placeholder.getStyleClass().add("detailBodyText");
            timingContainer.getChildren().add(placeholder);
            return;
        }
        String[] icons = new String[] {"\u2600", "\u26f0", "\ud83c\udfd9"};
        int idx = 0;
        for (ItineraryItem item : vm.itinerary) {
            String icon = icons[idx % icons.length];
            idx++;
            VBox card = new VBox();
            card.getStyleClass().add("itineraryCard");
            card.setSpacing(4);
            card.setMaxWidth(Double.MAX_VALUE);

            HBox head = new HBox(8);
            head.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            Label iconLabel = new Label(icon + " ");
            iconLabel.getStyleClass().add("itineraryIcon");
            Label title = new Label("Day " + item.dayNumber + ": " + n(item.title));
            title.getStyleClass().add("itineraryTitle");
            head.getChildren().addAll(iconLabel, title);

            Label sub = new Label(n(item.subtitle));
            sub.getStyleClass().add("itinerarySubtitle");
            sub.setWrapText(true);
            sub.setMaxWidth(640);

            card.getChildren().addAll(head, sub);
            timingContainer.getChildren().add(card);
        }
    }

    private void loadImage(ImageView view, String url) {
        if (view == null) return;
        if (url == null || url.isBlank()) {
            view.setImage(null);
            System.out.println("[PackageDetails] Skipping empty image url");
            return;
        }
        try {
            Image img = DataCache.getOrLoad("img:detail:" + url, () -> new Image(url, true));
            view.setImage(img);
        } catch (Exception e) {
            view.setImage(null);
            System.out.println("[PackageDetails] Failed to load image url=" + url + " err=" + e.getMessage());
        }
    }

    // VM classes
    /* Simple data holder mirroring the backend’s package details response. */
    public static class PackageDetailsVM {
        public UUID id;
        public String name;
        public String location;
        public BigDecimal basePrice;
        public String destImageUrl;
        public String hotelImageUrl;
        public String image1;
        public String image2;
        public String image3;
        public String image4;
        public String image5;
        public String overview;
        public String locationPoints;
        public String timing;
        public java.util.List<ItineraryItem> itinerary;
        public String groupSize;
    }

    public static class ItineraryItem {
        public int dayNumber;
        public String title;
        public String subtitle;
    }

    // Quick holder to pass selected package id into newly loaded controller
    /* Tiny static helper for passing the chosen id from the list controller to
       this modal without building a global event bus. */
    static class PackageDetailsState { static UUID pendingPackageId; }
}
