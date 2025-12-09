/* Lets an authenticated admin review, create, edit, and delete packages by
   wiring the dashboard form fields to the admin network helper and keeping
   the list of packages in sync with the server. Runs socket calls on worker
   threads and uses JavaFX bindings so staff can manage everything in one pane. */
package com.travel.frontend.controller;

import com.travel.frontend.admin.AdminSocketClient;
import com.travel.frontend.admin.AdminSocketClient.PackageVM;
import com.travel.frontend.ui.Navigator;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public class AdminDashboardController {
    @FXML private ListView<PackageVM> listView;
    @FXML private TextField nameField;
    @FXML private TextField locationField;
    @FXML private TextField priceField;
    @FXML private TextField image1Field;
    @FXML private TextField image2Field;
    @FXML private TextField image3Field;
    @FXML private TextField image4Field;
    @FXML private TextField image5Field;
    @FXML private TextArea overviewArea;
    @FXML private TextArea pointsArea;
    @FXML private TextArea timingArea;
    @FXML private TextArea itineraryArea;
    @FXML private TextField groupSizeField;
    @FXML private DatePicker bookingDeadlinePicker;
    @FXML private CheckBox packageAvailableBox;
    @FXML private CheckBox activeBox;
    @FXML private Label statusLabel;

    private final AdminSocketClient client = new AdminSocketClient();
    private PackageVM current;

    /* Sets up the selection listener so clicking a package populates the form,
       then immediately fetches data from the admin socket. */
    @FXML private void initialize() {
        listView.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
            current = n;
            if (n != null) fillForm(n);
        });
        onRefresh();
    }

    @FXML public void onBack() { Navigator.goLogin(); }
    @FXML public void goDestinations() { Navigator.goAdminDestinations(); }

    /* Pulls the latest packages from the AdminSocketClient.list() call on a
       background thread and updates the ListView once finished. */
    @FXML public void onRefresh() {
        if (com.travel.frontend.admin.AdminSession.getToken() == null) {
            statusLabel.setText("Please login as admin first.");
            return;
        }
        statusLabel.setText("Loading...");
        new Thread(() -> {
            try {
                List<PackageVM> items = com.travel.frontend.cache.FileCache.getOrLoad("admin:packages",
                        new com.fasterxml.jackson.core.type.TypeReference<List<PackageVM>>(){},
                        () -> {
                            try { return client.list(); }
                            catch (Exception ex) { throw new RuntimeException(ex); }
                        });
                Platform.runLater(() -> {
                    listView.getItems().setAll(items);
                    statusLabel.setText("");
                });
            } catch (Exception e) {
                String msg = detailedMessage(e);
                Platform.runLater(() -> {
                    listView.getItems().clear();
                    statusLabel.setText("Admin socket unavailable: " + msg + " (start admin server on 127.0.0.1:9090 and retry)");
                });
            }
        }).start();
    }

    @FXML public void onNew() {
        current = null;
        clearForm();
    }

    /* Reads the form, decides whether to create or update, and performs the
       socket call in a Thread so the dashboard stays responsive. */
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
                        current.image1 = vm.image1;
                        current.image2 = vm.image2;
                        current.image3 = vm.image3;
                        current.image4 = vm.image4;
                        current.image5 = vm.image5;
                        current.overview = vm.overview;
                        current.locationPoints = vm.locationPoints;
                        current.timing = vm.timing;
                        current.itinerary = vm.itinerary;
                        current.bookingDeadline = vm.bookingDeadline;
                        current.active = vm.active;
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

    /* Deletes the selected package by id and clears the form so we never show
       stale data after the server removes the entry. */
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
                e.printStackTrace();
                Platform.runLater(() -> statusLabel.setText("Delete failed: " + detailedMessage(e)));
            }
        }).start();
    }

    @FXML public void onViewAllBookings() {
        Navigator.goAdminAllPackageBookings();
    }

    /* Populates the form controls from a PackageVM, helping the admin see what
       they’re editing right after selecting an item from the ListView. */
    private void fillForm(PackageVM vm) {
        nameField.setText(n(vm.name));
        locationField.setText(n(vm.location));
        priceField.setText(vm.basePrice == null ? "" : vm.basePrice.toString());
        image1Field.setText(n(vm.image1));
        image2Field.setText(n(vm.image2));
        image3Field.setText(n(vm.image3));
        image4Field.setText(n(vm.image4));
        image5Field.setText(n(vm.image5));
        overviewArea.setText(n(vm.overview));
        pointsArea.setText(n(vm.locationPoints));
        timingArea.setText(n(vm.timing));
        itineraryArea.setText(toItineraryLines(vm));
        groupSizeField.setText(n(vm.groupSize));
        if (bookingDeadlinePicker != null) {
            bookingDeadlinePicker.setValue(parseDate(vm.bookingDeadline));
        }
        packageAvailableBox.setSelected(vm.packageAvailable);
        activeBox.setSelected(vm.active);
    }

    /* Builds a PackageVM from whatever is currently typed into the form,
       including parsing the price into BigDecimal so the socket payload is clean. */
    private PackageVM readForm() {
        PackageVM vm = new PackageVM();
        if (current != null) vm.id = current.id;
        vm.name = t(nameField);
        vm.location = t(locationField);
        String p = t(priceField);
        vm.basePrice = p.isBlank() ? null : new BigDecimal(p);
        vm.image1 = t(image1Field);
        vm.image2 = t(image2Field);
        vm.image3 = t(image3Field);
        vm.image4 = t(image4Field);
        vm.image5 = t(image5Field);
        vm.destImageUrl = vm.image1; // use first as destination card
        vm.hotelImageUrl = vm.image5; // use fifth as hotel image
        vm.overview = overviewArea.getText();
        vm.locationPoints = pointsArea.getText();
        vm.timing = timingArea.getText();
        vm.itinerary = parseItinerary(itineraryArea.getText());
        vm.groupSize = t(groupSizeField);
        vm.bookingDeadline = bookingDeadlinePicker != null && bookingDeadlinePicker.getValue() != null
                ? bookingDeadlinePicker.getValue().toString()
                : null;
        vm.packageAvailable = packageAvailableBox.isSelected();
        vm.active = activeBox.isSelected();
        return vm;
    }

    private void clearForm() {
        nameField.clear();
        locationField.clear();
        priceField.clear();
        image1Field.clear();
        image2Field.clear();
        image3Field.clear();
        image4Field.clear();
        image5Field.clear();
        overviewArea.clear();
        pointsArea.clear();
        timingArea.clear();
        itineraryArea.clear();
        groupSizeField.clear();
        if (bookingDeadlinePicker != null) bookingDeadlinePicker.setValue(null);
        packageAvailableBox.setSelected(false);
        activeBox.setSelected(true);
    }

    private static String t(TextField f) { return f.getText() == null ? "" : f.getText().trim(); }
    private static String n(String s) { return s == null ? "" : s; }
    private java.time.LocalDate parseDate(String s) {
        if (s == null || s.isBlank()) return null;
        try { return java.time.LocalDate.parse(s); } catch (Exception e) { return null; }
    }
    private String detailedMessage(Exception e) {
        if (e.getMessage() != null && !e.getMessage().isBlank()) return e.getMessage();
        Throwable c = e.getCause();
        while (c != null) {
            if (c.getMessage() != null && !c.getMessage().isBlank()) return c.getMessage();
            c = c.getCause();
        }
        return e.toString();
    }

    // Converts itinerary list to editable lines.
    private String toItineraryLines(PackageVM vm) {
        if (vm == null || vm.itinerary == null || vm.itinerary.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (AdminSocketClient.ItineraryVM it : vm.itinerary) {
            if (sb.length() > 0) sb.append("\n");
            sb.append(it.dayNumber).append("|").append(n(it.title)).append("|").append(n(it.subtitle));
        }
        return sb.toString();
    }

    // Parses textarea lines into itinerary items (day|title|subtitle).
    private List<AdminSocketClient.ItineraryVM> parseItinerary(String text) {
        if (text == null || text.isBlank()) return List.of();
        String[] lines = text.split("\\r?\\n");
        java.util.ArrayList<AdminSocketClient.ItineraryVM> list = new java.util.ArrayList<>();
        for (String line : lines) {
            if (line.isBlank()) continue;
            String[] parts = line.split("\\|", 3);
            AdminSocketClient.ItineraryVM it = new AdminSocketClient.ItineraryVM();
            try { it.dayNumber = Integer.parseInt(parts[0].trim()); } catch (Exception e) { it.dayNumber = list.size()+1; }
            it.title = parts.length > 1 ? parts[1].trim() : "";
            it.subtitle = parts.length > 2 ? parts[2].trim() : "";
            list.add(it);
        }
        return list;
    }
}

