package com.urban.frontend.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.urban.frontend.net.ApiClient;
import com.urban.frontend.ui.Navigator;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.math.BigDecimal;
import java.net.URI;
import java.util.List;
import java.util.UUID;

public class PackagesController {
    @FXML private VBox listContainer;

    private final ApiClient api = ApiClient.get();
    private final ObjectMapper mapper = new ObjectMapper();

    @FXML
    private void initialize() {
        loadPackages();
    }

    private void loadPackages() {
        new Thread(() -> {
            try {
                var res = api.rawGet("/packages", true);
                if (res.statusCode() != 200) throw new ApiClient.ApiException("Failed to load packages");
                List<PackageCard> items = mapper.readValue(res.body(), new TypeReference<List<PackageCard>>(){});
                Platform.runLater(() -> render(items));
            } catch (Exception e) {
                Platform.runLater(() -> {
                    listContainer.getChildren().setAll(new Label("Failed to load packages: " + e.getMessage()));
                });
            }
        }).start();
    }

    private void render(List<PackageCard> items) {
        listContainer.getChildren().clear();
        for (PackageCard p : items) {
            listContainer.getChildren().add(createCard(p));
        }
    }

    private Pane createCard(PackageCard p) {
        HBox card = new HBox(18);
        card.getStyleClass().add("pkg-card");
        card.setPadding(new Insets(16));
        card.setMaxWidth(960);

        Region image = new Region();
        image.setMinSize(220, 140);
        image.setPrefSize(220, 140);
        image.getStyleClass().add("image-placeholder");

        VBox content = new VBox(8);
        Label title = new Label(p.name);
        title.setId("cardTitle");
        Label loc = new Label("Location: " + p.location);
        loc.getStyleClass().add("subtle");
        Label price = new Label("Starting From BDT " + p.basePrice);
        price.getStyleClass().add("subtle");

        Button details = new Button("View Details");
        details.setOnAction(e -> PackageDetailsController.open(p.id));

        content.getChildren().addAll(title, loc, price, details);
        card.getChildren().addAll(image, content);
        return card;
    }

    public static class PackageCard {
        public UUID id;
        public String name;
        public String location;
        public BigDecimal basePrice;
        public String destImageUrl;
    }

    @FXML private void goPersonal() { Navigator.goHome(); }
    @FXML private void goDestinations() { /* placeholder */ }
    @FXML private void goHistory() { /* placeholder */ }
    @FXML private void goAbout() { /* placeholder */ }
    @FXML private void onLogout() { Navigator.goLogin(); }
}
