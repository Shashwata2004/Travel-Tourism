package com.travel.frontend.controller;

import com.travel.frontend.admin.AdminSocketClient;
import com.travel.frontend.admin.AdminSocketClient.PackageVM;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.math.BigDecimal;
import java.util.List;

public class AdminDashboardController {
    @FXML private ListView<PackageVM> listView;
    @FXML private TextField nameField;
    @FXML private TextField locationField;
    @FXML private TextField priceField;
    @FXML private TextField destImageField;
    @FXML private TextField hotelImageField;
    @FXML private TextArea overviewArea;
    @FXML private TextArea pointsArea;
    @FXML private TextArea timingArea;
    @FXML private CheckBox activeBox;
    @FXML private Label statusLabel;

    private final AdminSocketClient client = new AdminSocketClient();
    private PackageVM current;

    @FXML private void initialize() {
        listView.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
            current = n;
            if (n != null) fillForm(n);
        });
        onRefresh();
    }

    @FXML public void onRefresh() {
        if (com.travel.frontend.admin.AdminSession.getToken() == null) {
            statusLabel.setText("Please login as admin first.");
            return;
        }
        statusLabel.setText("Loading...");
        new Thread(() -> {
            try {
                List<PackageVM> items = client.list();
                Platform.runLater(() -> {
                    listView.getItems().setAll(items);
                    statusLabel.setText("");
                });
            } catch (Exception e) {
                Platform.runLater(() -> statusLabel.setText("Load failed: " + e.getMessage()));
            }
        }).start();
    }

    @FXML public void onNew() {
        current = null;
        clearForm();
    }

    @FXML public void onSave() {
        PackageVM vm = readForm();
        statusLabel.setText("Saving...");
        new Thread(() -> {
            try {
                if (current == null || current.id == null || current.id.isBlank()) {
                    String id = client.create(vm);
                    vm.id = id;
                    Platform.runLater(() -> {
                        listView.getItems().add(vm);
                        listView.getSelectionModel().select(vm);
                        statusLabel.setText("Created");
                    });
                } else {
                    client.update(current.id, vm);
                    Platform.runLater(() -> {
                        // update current item fields
                        current.name = vm.name;
                        current.location = vm.location;
                        current.basePrice = vm.basePrice;
                        current.destImageUrl = vm.destImageUrl;
                        current.hotelImageUrl = vm.hotelImageUrl;
                        current.overview = vm.overview;
                        current.locationPoints = vm.locationPoints;
                        current.timing = vm.timing;
                        current.active = vm.active;
                        listView.refresh();
                        statusLabel.setText("Saved");
                    });
                }
            } catch (Exception e) {
                Platform.runLater(() -> statusLabel.setText("Save failed: " + e.getMessage()));
            }
        }).start();
    }

    @FXML public void onDelete() {
        PackageVM sel = listView.getSelectionModel().getSelectedItem();
        if (sel == null || sel.id == null) { statusLabel.setText("Select a package to delete"); return; }
        statusLabel.setText("Deleting...");
        new Thread(() -> {
            try {
                client.delete(sel.id);
                Platform.runLater(() -> {
                    listView.getItems().remove(sel);
                    clearForm();
                    statusLabel.setText("Deleted");
                });
            } catch (Exception e) {
                Platform.runLater(() -> statusLabel.setText("Delete failed: " + e.getMessage()));
            }
        }).start();
    }

    private void fillForm(PackageVM vm) {
        nameField.setText(n(vm.name));
        locationField.setText(n(vm.location));
        priceField.setText(vm.basePrice == null ? "" : vm.basePrice.toString());
        destImageField.setText(n(vm.destImageUrl));
        hotelImageField.setText(n(vm.hotelImageUrl));
        overviewArea.setText(n(vm.overview));
        pointsArea.setText(n(vm.locationPoints));
        timingArea.setText(n(vm.timing));
        activeBox.setSelected(vm.active);
    }

    private PackageVM readForm() {
        PackageVM vm = new PackageVM();
        if (current != null) vm.id = current.id;
        vm.name = t(nameField);
        vm.location = t(locationField);
        String p = t(priceField);
        vm.basePrice = p.isBlank() ? null : new BigDecimal(p);
        vm.destImageUrl = t(destImageField);
        vm.hotelImageUrl = t(hotelImageField);
        vm.overview = overviewArea.getText();
        vm.locationPoints = pointsArea.getText();
        vm.timing = timingArea.getText();
        vm.active = activeBox.isSelected();
        return vm;
    }

    private void clearForm() {
        nameField.clear();
        locationField.clear();
        priceField.clear();
        destImageField.clear();
        hotelImageField.clear();
        overviewArea.clear();
        pointsArea.clear();
        timingArea.clear();
        activeBox.setSelected(true);
    }

    private static String t(TextField f) { return f.getText() == null ? "" : f.getText().trim(); }
    private static String n(String s) { return s == null ? "" : s; }
}

