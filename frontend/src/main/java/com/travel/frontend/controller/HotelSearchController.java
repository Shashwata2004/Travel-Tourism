package com.travel.frontend.controller;

import com.travel.frontend.cache.DataCache;
import com.travel.frontend.controller.DestinationsController.DestinationCard;
import com.travel.frontend.ui.Navigator;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.DateCell;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.time.LocalDate;

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

    private static final int MIN_GUESTS = 1;
    private static final int MAX_GUESTS = 12;
    private int guestCount = 2;

    @FXML
    private void initialize() {
        if (navbarController != null) {
            navbarController.setActive(NavbarController.ActivePage.DESTINATIONS);
        }
        configureHeroTexts();
        setupDatePickers();
        updateGuestLabels();
        hideGuestsOverlay();
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
        checkInPicker.setDayCellFactory(picker -> disableBefore(picker, minCheckIn));
        checkInPicker.setValue(minCheckIn);

        refreshCheckOutFactory();
        checkOutPicker.setValue(minCheckIn.plusDays(1));

        checkInPicker.valueProperty().addListener((obs, old, val) -> {
            LocalDate selected = val == null ? LocalDate.now().plusDays(1) : val;
            if (val == null || val.isBefore(minCheckIn)) {
                checkInPicker.setValue(minCheckIn);
                selected = minCheckIn;
            }
            if (checkOutPicker.getValue() == null || !checkOutPicker.getValue().isAfter(selected)) {
                checkOutPicker.setValue(selected.plusDays(1));
            }
            refreshCheckOutFactory();
        });
    }

    private void refreshCheckOutFactory() {
        if (checkOutPicker == null || checkInPicker == null) return;
        LocalDate checkIn = checkInPicker.getValue();
        LocalDate minCheckout = (checkIn == null ? LocalDate.now().plusDays(2) : checkIn.plusDays(1));
        checkOutPicker.setDayCellFactory(picker -> disableBefore(picker, minCheckout));
    }

    private DateCell disableBefore(DatePicker picker, LocalDate minDate) {
        return new DateCell() {
            @Override
            public void updateItem(LocalDate item, boolean empty) {
                super.updateItem(item, empty);
                boolean disable = empty || item.isBefore(minDate);
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
    }

    @FXML
    private void toggleGuests() {
        if (guestOverlay == null) return;
        boolean show = !guestOverlay.isVisible();
        guestOverlay.setVisible(show);
        guestOverlay.setManaged(show);
    }

    private void hideGuestsOverlay() {
        if (guestOverlay != null) {
            guestOverlay.setVisible(false);
            guestOverlay.setManaged(false);
        }
    }

    @FXML
    private void closeGuests() {
        hideGuestsOverlay();
    }

    @FXML
    private void consumeGuestCard(MouseEvent event) {
        event.consume();
    }

    @FXML
    private void incrementGuests() {
        if (guestCount >= MAX_GUESTS) return;
        guestCount++;
        updateGuestLabels();
    }

    @FXML
    private void decrementGuests() {
        if (guestCount <= MIN_GUESTS) return;
        guestCount--;
        updateGuestLabels();
    }

    @FXML
    private void handleSearch() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setHeaderText(null);
        String checkIn = checkInPicker.getValue() == null ? "select a check-in date" : checkInPicker.getValue().toString();
        String checkOut = checkOutPicker.getValue() == null ? "select a check-out date" : checkOutPicker.getValue().toString();
        alert.setContentText("We're finalizing hotel listings.\nSelection: " + checkIn + " ➜ " + checkOut + " for " + guestCount + (guestCount == 1 ? " guest." : " guests."));
        alert.setTitle("Hotels coming soon");
        alert.showAndWait();
    }

    @FXML
    private void goBack() {
        Navigator.goDestinations();
    }
}
