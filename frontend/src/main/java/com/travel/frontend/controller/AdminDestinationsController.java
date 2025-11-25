package com.travel.frontend.controller;

import com.travel.frontend.admin.AdminSession;
import com.travel.frontend.admin.AdminSocketClient;
import com.travel.frontend.admin.AdminSocketClient.DestinationVM;
import com.travel.frontend.ui.Navigator;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.util.List;

public class AdminDestinationsController {
    @FXML private ListView<DestinationVM> listView;
    @FXML private TextField nameField;
    @FXML private TextField regionField;
    @FXML private TextField tagsField;
    @FXML private TextField seasonField;
    @FXML private TextField imageField;
    @FXML private TextField hotelsField;
    @FXML private CheckBox activeBox;
    @FXML private Label pkgAvailableLabel;
    @FXML private Label statusLabel;

    private final AdminSocketClient client = new AdminSocketClient();
    private DestinationVM current;

    @FXML
    private void initialize() {
        listView.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
            current = n;
            if (n != null) fillForm(n);
        });
        onRefresh();
    }

    @FXML public void onBack() { Navigator.goLogin(); }
    @FXML public void goPackages() { Navigator.goAdminDashboard(); }

    @FXML
    public void onRefresh() {
        if (AdminSession.getToken() == null) {
            statusLabel.setText("Please login as admin first.");
            return;
        }
        statusLabel.setText("Loading...");
        new Thread(() -> {
            try {
                List<DestinationVM> items = client.listDestinations();
                Platform.runLater(() -> {
                    listView.getItems().setAll(items);
                    statusLabel.setText("");
                });
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> statusLabel.setText("Load failed: " + detailedMessage(e)));
            }
        }).start();
    }

    @FXML
    public void onNew() {
        current = null;
        clearForm();
    }

    @FXML
    public void onSave() {
        DestinationVM vm = readForm();
        statusLabel.setText("Saving...");
        new Thread(() -> {
            try {
                if (current == null || current.id == null || current.id.isBlank()) {
                    String id = client.createDestination(vm);
                    vm.id = id;
                    Platform.runLater(() -> {
                        listView.getItems().add(vm);
                        listView.getSelectionModel().select(vm);
                        statusLabel.setText("Created");
                    });
                } else {
                    client.updateDestination(current.id, vm);
                    Platform.runLater(() -> {
                        copy(vm, current);
                        listView.refresh();
                        statusLabel.setText("Saved");
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> statusLabel.setText("Save failed: " + detailedMessage(e)));
            }
        }).start();
    }

    @FXML
    public void onDelete() {
        DestinationVM sel = listView.getSelectionModel().getSelectedItem();
        if (sel == null || sel.id == null) { statusLabel.setText("Select a destination to delete"); return; }
        statusLabel.setText("Deleting...");
        new Thread(() -> {
            try {
                client.deleteDestination(sel.id);
                Platform.runLater(() -> {
                    listView.getItems().remove(sel);
                    clearForm();
                    statusLabel.setText("Deleted");
                });
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> statusLabel.setText("Delete failed: " + detailedMessage(e)));
            }
        }).start();
    }

    private void fillForm(DestinationVM vm) {
        nameField.setText(n(vm.name));
        regionField.setText(n(vm.region));
        tagsField.setText(n(vm.tags));
        seasonField.setText(n(vm.bestSeason));
        imageField.setText(n(vm.imageUrl));
        hotelsField.setText(vm.hotelsCount == null ? "" : vm.hotelsCount.toString());
        activeBox.setSelected(vm.active);
        pkgAvailableLabel.setText(vm.packageAvailable ? "Yes" : "No");
    }

    private DestinationVM readForm() {
        DestinationVM vm = new DestinationVM();
        if (current != null) vm.id = current.id;
        vm.name = t(nameField);
        vm.region = t(regionField);
        vm.tags = t(tagsField);
        vm.bestSeason = t(seasonField);
        vm.imageUrl = t(imageField);
        String h = t(hotelsField);
        try { vm.hotelsCount = h.isBlank() ? 0 : Integer.parseInt(h); } catch (NumberFormatException e) { vm.hotelsCount = 0; }
        vm.active = activeBox.isSelected();
        return vm;
    }

    private void clearForm() {
        nameField.clear();
        regionField.clear();
        tagsField.clear();
        seasonField.clear();
        imageField.clear();
        hotelsField.clear();
        activeBox.setSelected(true);
        pkgAvailableLabel.setText("");
    }

    private static void copy(DestinationVM from, DestinationVM to) {
        to.name = from.name;
        to.region = from.region;
        to.tags = from.tags;
        to.bestSeason = from.bestSeason;
        to.imageUrl = from.imageUrl;
        to.hotelsCount = from.hotelsCount;
        to.active = from.active;
    }

    private static String t(TextField f) { return f.getText() == null ? "" : f.getText().trim(); }
    private static String n(String s) { return s == null ? "" : s; }
    private String detailedMessage(Exception e) {
        if (e.getMessage() != null && !e.getMessage().isBlank()) return e.getMessage();
        Throwable c = e.getCause();
        while (c != null) {
            if (c.getMessage() != null && !c.getMessage().isBlank()) return c.getMessage();
            c = c.getCause();
        }
        return e.toString();
    }
}
