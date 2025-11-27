package com.travel.frontend.controller;

import com.travel.frontend.admin.AdminSocketClient;
import com.travel.frontend.admin.AdminSocketClient.HotelVM;
import com.travel.frontend.admin.AdminSocketClient.RoomVM;
import com.travel.frontend.cache.DataCache;
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

import com.travel.frontend.ui.Navigator;
import com.travel.frontend.controller.AdminHotelsState;

public class AdminHotelsController {

    @FXML private ListView<HotelVM> hotelList;
    @FXML private TextField nameField;
    @FXML private TextField ratingField;
    // Minimal price removed from admin UI
    @FXML private TextField realPriceField;
    @FXML private TextField currentPriceField;
    @FXML private TextField locationField;
    @FXML private TextArea nearbyArea;
    @FXML private TextArea facilitiesArea;
    @FXML private TextArea descriptionArea;
    @FXML private TextField roomsCountField;
    @FXML private TextField floorsCountField;
    @FXML private TextField image1Field;
    @FXML private TextField image2Field;
    @FXML private TextField image3Field;
    @FXML private TextField image4Field;
    @FXML private TextField image5Field;
    @FXML private TextField galleryField;
    @FXML private Label statusLabel;
    @FXML private Label destinationLabel;

    @FXML private TableView<RoomVM> roomsTable;
    @FXML private TableColumn<RoomVM, String> roomNameCol;
    @FXML private TableColumn<RoomVM, String> roomPriceCol;
    @FXML private TableColumn<RoomVM, String> roomGuestsCol;
    @FXML private TableColumn<RoomVM, String> roomAvailCol;
    @FXML private TextField roomNameField;
    @FXML private TextField roomPriceField;
    @FXML private TextField roomGuestsField;
    @FXML private TextField roomAvailField;

    private final AdminSocketClient client = new AdminSocketClient();
    private HotelVM current;
    private final ObservableList<RoomVM> roomItems = FXCollections.observableArrayList();

    @FXML
    private void initialize() {
        if (destinationLabel != null && AdminHotelsState.getDestinationName() != null) {
            destinationLabel.setText("Destination: " + AdminHotelsState.getDestinationName());
        }
        hotelList.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
            current = n;
            if (n != null) fillForm(n);
            loadRoomsForSelection();
        });
        if (roomsTable != null) {
            roomsTable.setItems(roomItems);
            roomNameCol.setCellValueFactory(data -> new SimpleStringProperty(
                    data.getValue().name == null ? "" : data.getValue().name));
            roomPriceCol.setCellValueFactory(data -> new SimpleStringProperty(
                    data.getValue().price == null ? "" : data.getValue().price));
            roomGuestsCol.setCellValueFactory(data -> new SimpleStringProperty(
                    data.getValue().maxGuests == null ? "" : data.getValue().maxGuests.toString()));
            roomAvailCol.setCellValueFactory(data -> new SimpleStringProperty(
                    data.getValue().availableRooms == null ? "" : data.getValue().availableRooms.toString()));
        }
        onRefresh();
    }

    @FXML private void onBack() { AdminHotelsState.clear(); Navigator.goAdminDestinations(); }

    @FXML
    private void onRefresh() {
        statusLabel.setText("Loading...");
        UUID destId = AdminHotelsState.getDestinationId();
        if (destId == null) { statusLabel.setText("No destination selected."); return; }
        new Thread(() -> {
            try {
                List<HotelVM> items = FileCache.getOrLoad("admin:hotels:" + destId,
                        new TypeReference<List<HotelVM>>(){},
                        () -> {
                            try {
                                return client.listHotels(destId.toString());
                            } catch (Exception ex) {
                                throw new RuntimeException(ex);
                            }
                        });
                DataCache.put("admin:hotels:" + destId, items);
                Platform.runLater(() -> {
                    hotelList.getItems().setAll(items);
                    statusLabel.setText("");
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    statusLabel.setText("Load failed: " + e.getMessage());
                    showError("Hotel load failed", e.getMessage());
                });
            }
        }).start();
    }

    @FXML
    private void onNew() {
        current = null;
        clearForm();
        roomItems.clear();
    }

    @FXML
    private void onSave() {
        UUID destId = AdminHotelsState.getDestinationId();
        if (destId == null) { statusLabel.setText("No destination selected."); return; }
        HotelVM vm = readForm();
        vm.destinationId = destId.toString();
        statusLabel.setText("Saving...");
        new Thread(() -> {
            try {
                if (current == null || current.id == null) {
                    String id = client.createHotel(vm);
                    vm.id = id;
                    Platform.runLater(() -> {
                        hotelList.getItems().add(vm);
                        hotelList.getSelectionModel().select(vm);
                        statusLabel.setText("Created");
                        DataCache.remove("admin:hotels:" + destId);
                        FileCache.remove("admin:hotels:" + destId);
                    });
                } else {
                    client.updateHotel(current.id, vm);
                    Platform.runLater(() -> {
                        copy(vm, current);
                        hotelList.refresh();
                        statusLabel.setText("Saved");
                        DataCache.remove("admin:hotels:" + destId);
                        FileCache.remove("admin:hotels:" + destId);
                    });
                }
            } catch (Exception e) {
                Platform.runLater(() -> statusLabel.setText("Save failed: " + e.getMessage()));
            }
        }).start();
    }

    @FXML
    private void onDelete() {
        HotelVM sel = hotelList.getSelectionModel().getSelectedItem();
        if (sel == null || sel.id == null) { statusLabel.setText("Select a hotel to delete"); return; }
        statusLabel.setText("Deleting...");
        new Thread(() -> {
            try {
                client.deleteHotel(sel.id);
                Platform.runLater(() -> {
                    hotelList.getItems().remove(sel);
                    clearForm();
                    roomItems.clear();
                    statusLabel.setText("Deleted");
                    UUID destId = AdminHotelsState.getDestinationId();
                    if (destId != null) {
                        DataCache.remove("admin:hotels:" + destId);
                        FileCache.remove("admin:hotels:" + destId);
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    statusLabel.setText("Delete failed: " + e.getMessage());
                    showError("Delete failed", e.getMessage());
                });
            }
        }).start();
    }

    // Room handlers
    @FXML
    private void onAddRoom() {
        RoomVM r = new RoomVM();
        r.name = roomNameField.getText();
        r.price = roomPriceField.getText();
        r.maxGuests = parseInt(roomGuestsField.getText());
        r.availableRooms = parseInt(roomAvailField.getText());
        r.description = "";
        roomItems.add(r);
        clearRoomForm();
    }

    @FXML
    private void onRemoveRoom() {
        RoomVM sel = roomsTable.getSelectionModel().getSelectedItem();
        if (sel != null) {
            roomItems.remove(sel);
        }
    }

    @FXML
    private void onSaveRooms() {
        HotelVM sel = hotelList.getSelectionModel().getSelectedItem();
        if (sel == null || sel.id == null) { statusLabel.setText("Select a hotel first"); return; }
        statusLabel.setText("Saving rooms...");
        List<RoomVM> payload = List.copyOf(roomItems);
        new Thread(() -> {
            try {
                client.saveRooms(sel.id, payload);
                Platform.runLater(() -> statusLabel.setText("Rooms saved"));
                DataCache.put("admin:rooms:" + sel.id, payload);
                FileCache.put("admin:rooms:" + sel.id, payload);
            } catch (Exception e) {
                Platform.runLater(() -> statusLabel.setText("Rooms save failed: " + e.getMessage()));
            }
        }).start();
    }

    private void loadRoomsForSelection() {
        HotelVM sel = hotelList.getSelectionModel().getSelectedItem();
        if (sel == null || sel.id == null) { roomItems.clear(); return; }
        new Thread(() -> {
            try {
                List<RoomVM> rooms = FileCache.getOrLoad("admin:rooms:" + sel.id,
                        new TypeReference<List<RoomVM>>(){},
                        () -> {
                            try {
                                return client.listRooms(sel.id);
                            } catch (Exception ex) {
                                throw new RuntimeException(ex);
                            }
                        });
                DataCache.put("admin:rooms:" + sel.id, rooms);
                Platform.runLater(() -> {
                    roomItems.setAll(rooms);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    statusLabel.setText("Room load failed: " + e.getMessage());
                    showError("Room load failed", e.getMessage());
                });
            }
        }).start();
    }

    private void fillForm(HotelVM vm) {
        nameField.setText(vm.name);
        ratingField.setText(vm.rating == null ? "" : vm.rating.toString());
        realPriceField.setText(vm.realPrice == null ? "" : vm.realPrice);
        currentPriceField.setText(vm.currentPrice == null ? "" : vm.currentPrice);
        locationField.setText(vm.location);
        nearbyArea.setText(vm.nearby);
        facilitiesArea.setText(vm.facilities);
        descriptionArea.setText(vm.description);
        roomsCountField.setText(vm.roomsCount == null ? "" : vm.roomsCount.toString());
        floorsCountField.setText(vm.floorsCount == null ? "" : vm.floorsCount.toString());
        image1Field.setText(vm.image1);
        image2Field.setText(vm.image2);
        image3Field.setText(vm.image3);
        image4Field.setText(vm.image4);
        image5Field.setText(vm.image5);
        galleryField.setText(vm.gallery);
    }

    private HotelVM readForm() {
        HotelVM vm = new HotelVM();
        vm.name = nameField.getText();
        vm.rating = ratingField.getText();
        vm.realPrice = realPriceField.getText();
        vm.currentPrice = currentPriceField.getText();
        vm.location = locationField.getText();
        vm.nearby = nearbyArea.getText();
        vm.facilities = facilitiesArea.getText();
        vm.description = descriptionArea.getText();
        vm.roomsCount = parseInt(roomsCountField.getText());
        vm.floorsCount = parseInt(floorsCountField.getText());
        vm.image1 = image1Field.getText();
        vm.image2 = image2Field.getText();
        vm.image3 = image3Field.getText();
        vm.image4 = image4Field.getText();
        vm.image5 = image5Field.getText();
        vm.gallery = galleryField.getText();
        return vm;
    }

    private void clearForm() {
        nameField.clear();
        ratingField.clear();
        realPriceField.clear();
        currentPriceField.clear();
        locationField.clear();
        nearbyArea.clear();
        facilitiesArea.clear();
        descriptionArea.clear();
        roomsCountField.clear();
        floorsCountField.clear();
        image1Field.clear();
        image2Field.clear();
        image3Field.clear();
        image4Field.clear();
        image5Field.clear();
        galleryField.clear();
        statusLabel.setText("");
    }

    private void clearRoomForm() {
        roomNameField.clear();
        roomPriceField.clear();
        roomGuestsField.clear();
        roomAvailField.clear();
    }

    private void copy(HotelVM from, HotelVM to) {
        to.name = from.name;
        to.rating = from.rating;
        to.realPrice = from.realPrice;
        to.currentPrice = from.currentPrice;
        to.location = from.location;
        to.nearby = from.nearby;
        to.facilities = from.facilities;
        to.description = from.description;
        to.roomsCount = from.roomsCount;
        to.floorsCount = from.floorsCount;
        to.image1 = from.image1;
        to.image2 = from.image2;
        to.image3 = from.image3;
        to.image4 = from.image4;
        to.image5 = from.image5;
        to.gallery = from.gallery;
    }

    private Integer parseInt(String s) {
        try { return (s == null || s.isBlank()) ? null : Integer.parseInt(s.trim()); }
        catch (NumberFormatException e) { return null; }
    }

    private void showError(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setHeaderText(title);
        a.setContentText(msg);
        a.showAndWait();
    }
}
