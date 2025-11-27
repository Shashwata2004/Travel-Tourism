package com.travel.frontend.controller;

import com.travel.frontend.admin.AdminSocketClient;
import com.travel.frontend.admin.AdminSocketClient.RoomVM;
import com.travel.frontend.cache.FileCache;
import com.fasterxml.jackson.core.type.TypeReference;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;

import java.util.List;
import java.util.UUID;

public class AdminRoomsController {

    @FXML private Label hotelLabel;
    @FXML private TableView<RoomVM> roomTable;
    @FXML private TableColumn<RoomVM, String> nameCol;
    @FXML private TableColumn<RoomVM, String> bedCol;
    @FXML private TableColumn<RoomVM, String> totalCol;
    @FXML private TableColumn<RoomVM, String> capacityCol;
    @FXML private TableColumn<RoomVM, String> priceCol;
    @FXML private TextField nameField;
    @FXML private TextField bedTypeField;
    @FXML private TextField totalField;
    @FXML private TextField maxGuestsField;
    @FXML private TextField facilitiesField;
    @FXML private TextField realPriceField;
    @FXML private TextField currentPriceField;
    @FXML private TextField image1Field;
    @FXML private TextField image2Field;
    @FXML private TextField image3Field;
    @FXML private TextField image4Field;
    @FXML private TextArea descriptionArea;
    @FXML private Label statusLabel;

    private final AdminSocketClient client = new AdminSocketClient();
    private final ObservableList<RoomVM> items = FXCollections.observableArrayList();
    private RoomVM current;

    @FXML
    private void initialize() {
        UUID hotelId = AdminRoomsState.getHotelId();
        String hotelName = AdminRoomsState.getHotelName();
        if (hotelLabel != null && hotelName != null) {
            hotelLabel.setText("Hotel: " + hotelName);
        }
        if (roomTable != null) {
            roomTable.setItems(items);
            nameCol.setCellValueFactory(data -> new SimpleStringProperty(n(data.getValue().name)));
            bedCol.setCellValueFactory(data -> new SimpleStringProperty(n(data.getValue().bedType)));
            totalCol.setCellValueFactory(data -> new SimpleStringProperty(num(data.getValue().totalRooms)));
            capacityCol.setCellValueFactory(data -> new SimpleStringProperty(num(data.getValue().maxGuests)));
            priceCol.setCellValueFactory(data -> new SimpleStringProperty(price(data.getValue())));
            roomTable.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
                current = n;
                if (n != null) fillForm(n);
            });
        }
        loadRooms(hotelId);
    }

    @FXML private void onBack() { AdminRoomsState.clear(); com.travel.frontend.ui.Navigator.goAdminHotels(); }

    @FXML
    private void onNew() {
        current = null;
        clearForm();
    }

    @FXML
    private void onSave() {
        UUID hotelId = AdminRoomsState.getHotelId();
        if (hotelId == null) { statusLabel.setText("No hotel selected."); return; }
        RoomVM vm = readForm();
        // Merge into current list before sending so existing rooms are preserved
        List<RoomVM> payload = new java.util.ArrayList<>(items);
        if (current != null) {
            int idx = payload.indexOf(current);
            if (idx >= 0) {
                vm.id = current.id;
                payload.set(idx, vm);
            } else {
                payload.add(vm);
            }
        } else {
            payload.add(vm);
        }

        statusLabel.setText("Saving rooms...");
        new Thread(() -> {
            try {
                client.saveRooms(hotelId.toString(), payload);
                // Refresh list after save to capture generated IDs
                List<RoomVM> fresh = client.listRooms(hotelId.toString());
                FileCache.put("admin:rooms:" + hotelId, fresh);
                Platform.runLater(() -> {
                    items.setAll(fresh);
                    roomTable.getSelectionModel().select(vm);
                    statusLabel.setText("Saved");
                });
            } catch (Exception e) {
                Platform.runLater(() -> statusLabel.setText("Save failed: " + e.getMessage()));
            }
        }).start();
    }

    @FXML
    private void onDelete() {
        if (current == null) { statusLabel.setText("Select a room"); return; }
        UUID hotelId = AdminRoomsState.getHotelId();
        if (hotelId == null) { statusLabel.setText("No hotel selected."); return; }
        statusLabel.setText("Deleting...");
        new Thread(() -> {
            try {
                List<RoomVM> fresh = items.filtered(r -> current.id == null || !current.id.equals(r.id));
                client.saveRooms(hotelId.toString(), fresh);
                FileCache.put("admin:rooms:" + hotelId, fresh);
                Platform.runLater(() -> {
                    items.setAll(fresh);
                    clearForm();
                    statusLabel.setText("Deleted");
                });
            } catch (Exception e) {
                Platform.runLater(() -> statusLabel.setText("Delete failed: " + e.getMessage()));
            }
        }).start();
    }

    private void loadRooms(UUID hotelId) {
        if (hotelId == null) { statusLabel.setText("No hotel selected."); return; }
        statusLabel.setText("Loading...");
        new Thread(() -> {
            try {
                List<RoomVM> rooms = FileCache.getOrLoad("admin:rooms:" + hotelId,
                        new TypeReference<List<RoomVM>>(){},
                        () -> {
                            try { return client.listRooms(hotelId.toString()); }
                            catch (Exception ex) { throw new RuntimeException(ex); }
                        });
                Platform.runLater(() -> {
                    items.setAll(rooms);
                    statusLabel.setText("");
                });
            } catch (Exception e) {
                Platform.runLater(() -> statusLabel.setText("Load failed: " + e.getMessage()));
            }
        }).start();
    }

    private void fillForm(RoomVM vm) {
        nameField.setText(n(vm.name));
        bedTypeField.setText(n(vm.bedType));
        totalField.setText(num(vm.totalRooms));
        maxGuestsField.setText(num(vm.maxGuests));
        facilitiesField.setText(n(vm.facilities));
        realPriceField.setText(n(vm.realPrice));
        currentPriceField.setText(n(vm.currentPrice != null ? vm.currentPrice : vm.price));
        descriptionArea.setText(n(vm.description));
        image1Field.setText(n(vm.image1));
        image2Field.setText(n(vm.image2));
        image3Field.setText(n(vm.image3));
        image4Field.setText(n(vm.image4));
    }

    private RoomVM readForm() {
        RoomVM vm = new RoomVM();
        vm.name = t(nameField);
        vm.bedType = t(bedTypeField);
        vm.totalRooms = parseInt(totalField.getText());
        vm.maxGuests = parseInt(maxGuestsField.getText());
        vm.facilities = t(facilitiesField);
        vm.realPrice = t(realPriceField);
        vm.currentPrice = t(currentPriceField);
        vm.description = descriptionArea.getText();
        vm.image1 = t(image1Field);
        vm.image2 = t(image2Field);
        vm.image3 = t(image3Field);
        vm.image4 = t(image4Field);
        return vm;
    }

    private void clearForm() {
        nameField.clear();
        bedTypeField.clear();
        totalField.clear();
        maxGuestsField.clear();
        facilitiesField.clear();
        realPriceField.clear();
        currentPriceField.clear();
        image1Field.clear();
        image2Field.clear();
        image3Field.clear();
        image4Field.clear();
        descriptionArea.clear();
        statusLabel.setText("");
    }

    private String n(String s) { return s == null ? "" : s; }
    private String t(TextField f) { return f == null ? "" : n(f.getText()); }
    private String num(Integer i) { return i == null ? "" : i.toString(); }
    private Integer parseInt(String s) { try { return (s == null || s.isBlank()) ? null : Integer.parseInt(s.trim()); } catch (Exception e) { return null; } }
    private String price(RoomVM r) {
        if (r.currentPrice != null && !r.currentPrice.isBlank()) return r.currentPrice;
        if (r.realPrice != null && !r.realPrice.isBlank()) return r.realPrice;
        return r.price == null ? "" : r.price;
    }
}
