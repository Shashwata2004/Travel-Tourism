package com.travel.frontend.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.travel.frontend.cache.DataCache;
import com.travel.frontend.model.RoomBookingAdminView;
import com.travel.frontend.net.ApiClient;
import com.travel.frontend.ui.Navigator;
import com.travel.frontend.admin.AdminSession;
import com.travel.frontend.session.Session;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AdminRoomBookingsController {

    @FXML private Label headerLabel;
    @FXML private Label subHeaderLabel;
    @FXML private Label statusLabel;
    @FXML private Label nextAvailableLabel;
    @FXML private TableView<RoomBookingAdminView> bookingTable;
    @FXML private TableColumn<RoomBookingAdminView, LocalDate> checkInCol;
    @FXML private TableColumn<RoomBookingAdminView, LocalDate> checkOutCol;
    @FXML private TableColumn<RoomBookingAdminView, Integer> roomsCol;
    @FXML private TableColumn<RoomBookingAdminView, Integer> guestsCol;
    @FXML private TableColumn<RoomBookingAdminView, String> customerCol;
    @FXML private TableColumn<RoomBookingAdminView, String> idTypeCol;
    @FXML private TableColumn<RoomBookingAdminView, String> idNumberCol;
    @FXML private TableColumn<RoomBookingAdminView, java.math.BigDecimal> priceCol;
    @FXML private TableColumn<RoomBookingAdminView, java.time.Instant> createdCol;

    private final ApiClient api = ApiClient.get();
    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private UUID roomId;
    private String roomName;
    private String hotelName;

    @FXML
    private void initialize() {
        roomId = AdminRoomsState.getRoomId();
        roomName = AdminRoomsState.getRoomName();
        hotelName = AdminRoomsState.getHotelName();
        if (roomId == null) {
            Navigator.goAdminRooms();
            return;
        }
        if (headerLabel != null) headerLabel.setText("Bookings for: " + nullSafe(roomName));
        if (subHeaderLabel != null) subHeaderLabel.setText(nullSafe(hotelName));

        if (bookingTable != null) {
            checkInCol.setCellValueFactory(new PropertyValueFactory<>("checkIn"));
            checkOutCol.setCellValueFactory(new PropertyValueFactory<>("checkOut"));
            roomsCol.setCellValueFactory(new PropertyValueFactory<>("roomsBooked"));
            guestsCol.setCellValueFactory(new PropertyValueFactory<>("totalGuests"));
            customerCol.setCellValueFactory(new PropertyValueFactory<>("customerName"));
            idTypeCol.setCellValueFactory(new PropertyValueFactory<>("idType"));
            idNumberCol.setCellValueFactory(new PropertyValueFactory<>("idNumber"));
            priceCol.setCellValueFactory(new PropertyValueFactory<>("totalPrice"));
            createdCol.setCellValueFactory(new PropertyValueFactory<>("createdAt"));
        }
        loadData();
    }

    @FXML
    private void onReload() {
        if (roomId == null) return;
        DataCache.remove("admin:roomBookings:" + roomId);
        loadData();
    }

    @FXML
    private void onBack() {
        Navigator.goAdminRooms();
    }

    private void loadData() {
        status("Loading bookings...");
        new Thread(() -> {
            try {
                // ensure admin token is used
                if (AdminSession.isAuthenticated()) {
                    Session.setToken(AdminSession.getToken());
                }
                String key = "admin:roomBookings:" + roomId;
                List<RoomBookingAdminView> list = DataCache.getOrLoad(key, () -> fetchBookings());
                LocalDate next = fetchNextAvailable();
                Platform.runLater(() -> {
                    bookingTable.getItems().setAll(list);
                    nextAvailableLabel.setText(next != null ? next.format(DateTimeFormatter.ISO_DATE) : "N/A");
                    status("Loaded " + list.size() + " bookings");
                });
            } catch (Exception e) {
                Platform.runLater(() -> status("Error: " + e.getMessage()));
            }
        }).start();
    }

    private List<RoomBookingAdminView> fetchBookings() throws Exception {
        var res = api.rawGet("/admin/rooms/" + roomId + "/bookings", true);
        if (res.statusCode() != 200) throw new RuntimeException("HTTP " + res.statusCode() + ": " + res.body());
        return mapper.readValue(res.body(), new TypeReference<ArrayList<RoomBookingAdminView>>() {});
    }

    private LocalDate fetchNextAvailable() {
        try {
            var res = api.rawGet("/admin/rooms/" + roomId + "/occupancy", true);
            if (res.statusCode() == 200 && res.body() != null && !res.body().isBlank()) {
                return LocalDate.parse(res.body().replace("\"", ""));
            }
        } catch (Exception ignored) {}
        return null;
    }

    private void status(String msg) { if (statusLabel != null) statusLabel.setText(msg); }
    private String nullSafe(String s) { return s == null ? "" : s; }
}
