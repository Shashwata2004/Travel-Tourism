package com.travel.frontend.controller;

import com.travel.frontend.model.RoomBookingAdminView;
import com.travel.frontend.net.ApiClient;
import com.travel.frontend.ui.Navigator;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class AdminAllRoomBookingsController {
    @FXML private TableView<RoomBookingAdminView> table;
    @FXML private TableColumn<RoomBookingAdminView, String> customerCol;
    @FXML private TableColumn<RoomBookingAdminView, String> emailCol;
    @FXML private TableColumn<RoomBookingAdminView, String> destinationCol;
    @FXML private TableColumn<RoomBookingAdminView, String> hotelCol;
    @FXML private TableColumn<RoomBookingAdminView, String> roomCol;
    @FXML private TableColumn<RoomBookingAdminView, Integer> roomsCol;
    @FXML private TableColumn<RoomBookingAdminView, Integer> guestsCol;
    @FXML private TableColumn<RoomBookingAdminView, String> idCol;
    @FXML private TableColumn<RoomBookingAdminView, java.time.LocalDate> checkInCol;
    @FXML private TableColumn<RoomBookingAdminView, java.time.LocalDate> checkOutCol;
    @FXML private TableColumn<RoomBookingAdminView, Instant> bookedCol;
    @FXML private TableColumn<RoomBookingAdminView, Instant> canceledCol;
    @FXML private TableColumn<RoomBookingAdminView, String> txnCol;
    @FXML private TextField searchField;
    @FXML private Label statusLabel;

    private final ApiClient api = ApiClient.get();
    private List<RoomBookingAdminView> all;

    @FXML
    private void initialize() {
        if (table != null) {
            table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
            customerCol.setCellValueFactory(new PropertyValueFactory<>("customerName"));
            emailCol.setCellValueFactory(new PropertyValueFactory<>("userEmail"));
            destinationCol.setCellValueFactory(new PropertyValueFactory<>("destinationName"));
            hotelCol.setCellValueFactory(new PropertyValueFactory<>("hotelName"));
            roomCol.setCellValueFactory(new PropertyValueFactory<>("roomName"));
            roomsCol.setCellValueFactory(new PropertyValueFactory<>("roomsBooked"));
            guestsCol.setCellValueFactory(new PropertyValueFactory<>("totalGuests"));
            idCol.setCellValueFactory(new PropertyValueFactory<>("idNumber"));
            checkInCol.setCellValueFactory(new PropertyValueFactory<>("checkIn"));
            checkOutCol.setCellValueFactory(new PropertyValueFactory<>("checkOut"));
            bookedCol.setCellValueFactory(new PropertyValueFactory<>("createdAt"));
            canceledCol.setCellValueFactory(new PropertyValueFactory<>("canceledAt"));
            txnCol.setCellValueFactory(new PropertyValueFactory<>("transactionId"));
            table.setOnMouseClicked(evt -> {
                if (evt.getClickCount() == 2) {
                    RoomBookingAdminView sel = table.getSelectionModel().getSelectedItem();
                    if (sel != null) {
                        var root = table.getScene().getRoot();
                        if (root instanceof javafx.scene.layout.Pane p) {
                            AdminBookingModalController.showRoom(p, sel);
                        }
                    }
                }
            });
        }
        if (searchField != null) {
            searchField.textProperty().addListener((obs, o, n) -> render());
        }
        loadData();
    }

    @FXML private void onBack() { Navigator.goAdminDestinations(); }

    @FXML private void onReload() { loadData(); }

    private void loadData() {
        status("Loading...");
        new Thread(() -> {
            try {
                List<RoomBookingAdminView> list = api.getAllRoomBookings();
                Platform.runLater(() -> {
                    all = list;
                    render();
                    status("Loaded " + (list == null ? 0 : list.size()));
                });
            } catch (Exception e) {
                Platform.runLater(() -> status("Error: " + e.getMessage()));
            }
        }).start();
    }

    private void render() {
        if (table == null) return;
        String q = searchField == null || searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase(Locale.ROOT);
        List<RoomBookingAdminView> filtered = all == null ? List.of() : all.stream()
                .filter(b -> matches(b, q))
                .collect(Collectors.toList());
        table.getItems().setAll(filtered);
    }

    private boolean matches(RoomBookingAdminView b, String q) {
        if (q.isBlank()) return true;
        return contains(b.customerName, q) || contains(b.userEmail, q) || contains(b.destinationName, q)
                || contains(b.hotelName, q) || contains(b.roomName, q) || contains(b.idNumber, q)
                || contains(b.idType, q) || contains(b.transactionId, q);
    }

    private boolean contains(String s, String q) {
        return s != null && s.toLowerCase(Locale.ROOT).contains(q);
    }

    private void status(String s) { if (statusLabel != null) statusLabel.setText(s); }
}
