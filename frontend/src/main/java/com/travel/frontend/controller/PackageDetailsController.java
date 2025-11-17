/* Opens a pop-up window for a specific package, fetches its full description,
   and hands off to the booking dialog when someone clicks “Book Now.”
   Uses cached API responses so re-opening the same package feels instant. */
package com.travel.frontend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.travel.frontend.net.ApiClient;
import com.travel.frontend.cache.DataCache;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TabPane;
import javafx.scene.layout.Region;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URL;
import java.util.UUID;

public class PackageDetailsController {
    @FXML private Label nameLabel;
    @FXML private Label locationLabel;
    @FXML private Label priceLabel;
    @FXML private Label overviewText;
    @FXML private Label locationPointsText;
    @FXML private Label timingText;

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
            s.setScene(new Scene(root, 860, 600));
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

        new Thread(() -> {
            try {
                PackageDetailsVM vm = DataCache.getOrLoad("pkg:" + packageId, () -> {
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
        nameLabel.setText(vm.name);
        locationLabel.setText(vm.location);
        priceLabel.setText("BDT " + vm.basePrice + " per person");
        overviewText.setText(n(vm.overview));
        locationPointsText.setText(n(vm.locationPoints));
        timingText.setText(n(vm.timing));
    }

    /* Button handler bridging to the booking dialog, sending through the id and
       price text gathered on this screen. */
    @FXML
    private void onBookNow() {
        BookingDialogController.open(packageId, nameLabel.getText(), priceLabel.getText());
    }

    private static String n(String s) { return s == null ? "" : s; }

    // VM classes
    /* Simple data holder mirroring the backend’s package details response. */
    public static class PackageDetailsVM {
        public UUID id;
        public String name;
        public String location;
        public BigDecimal basePrice;
        public String destImageUrl;
        public String hotelImageUrl;
        public String overview;
        public String locationPoints;
        public String timing;
    }

    // Quick holder to pass selected package id into newly loaded controller
    /* Tiny static helper for passing the chosen id from the list controller to
       this modal without building a global event bus. */
    static class PackageDetailsState { static UUID pendingPackageId; }
}
