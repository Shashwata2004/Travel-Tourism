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
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public class PackagesController {
    @FXML private VBox listContainer;

    private final ApiClient api = ApiClient.get();
    private final ObjectMapper mapper = new ObjectMapper();

    /* JavaFX lifecycle hook that kicks off the first load so visitors instantly
       see featured trips once the screen renders. */
    @FXML
    private void initialize() {
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
        for (PackageCard p : items) {
            listContainer.getChildren().add(createCard(p));
        }
    }

    /* Creates the visual card for one package using standard layout nodes and
       wires the “View Details” button to open the modal controller. */
    private Pane createCard(PackageCard p) {
        HBox card = new HBox(18);
        card.getStyleClass().add("pkg-card");
        card.setPadding(new Insets(16));
        card.setMaxWidth(960);

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
        card.getChildren().addAll(createImageNode(p.destImageUrl), content);
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
                    () -> new Image(url, width, height, true, true, false));
        } catch (Exception e) {
            return null;
        }
    }

    @FXML private void goPersonal() { Navigator.goHome(); }
    @FXML private void goDestinations() { /* placeholder */ }
    @FXML private void goHistory() { /* placeholder */ }
    @FXML private void goAbout() { /* placeholder */ }
    @FXML private void onLogout() { com.travel.frontend.cache.DataCache.clear(); Navigator.goLogin(); }
}
