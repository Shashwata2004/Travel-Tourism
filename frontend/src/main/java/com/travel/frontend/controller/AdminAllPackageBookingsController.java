package com.travel.frontend.controller;

import com.travel.frontend.model.PackageBookingAdminView;
import com.travel.frontend.net.ApiClient;
import com.travel.frontend.ui.Navigator;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TableRow;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class AdminAllPackageBookingsController {
    @FXML private TableView<PackageBookingAdminView> table;
    @FXML private TableColumn<PackageBookingAdminView, String> customerCol;
    @FXML private TableColumn<PackageBookingAdminView, String> emailCol;
    @FXML private TableColumn<PackageBookingAdminView, String> packageCol;
    @FXML private TableColumn<PackageBookingAdminView, Integer> personsCol;
    @FXML private TableColumn<PackageBookingAdminView, String> idCol;
    @FXML private TableColumn<PackageBookingAdminView, Instant> bookedCol;
    @FXML private TableColumn<PackageBookingAdminView, Instant> canceledCol;
    @FXML private TableColumn<PackageBookingAdminView, String> txnCol;
    @FXML private TextField searchField;
    @FXML private Label statusLabel;

    private final ApiClient api = ApiClient.get();
    private List<PackageBookingAdminView> all;
    private final DateTimeFormatter df = DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a", Locale.ENGLISH);

    @FXML
    private void initialize() {
        if (table != null) {
            table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
            customerCol.setCellValueFactory(new PropertyValueFactory<>("customerName"));
            emailCol.setCellValueFactory(new PropertyValueFactory<>("userEmail"));
            packageCol.setCellValueFactory(new PropertyValueFactory<>("packageName"));
            personsCol.setCellValueFactory(new PropertyValueFactory<>("totalPersons"));
            idCol.setCellValueFactory(new PropertyValueFactory<>("idNumber"));
            bookedCol.setCellValueFactory(new PropertyValueFactory<>("createdAt"));
            canceledCol.setCellValueFactory(new PropertyValueFactory<>("canceledAt"));
            txnCol.setCellValueFactory(new PropertyValueFactory<>("transactionId"));
            table.setRowFactory(tv -> new TableRow<>() {
                @Override
                protected void updateItem(PackageBookingAdminView item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        getStyleClass().remove("canceledRow");
                        getStyleClass().remove("adminCanceledRow");
                        getStyleClass().remove("upcomingRow");
                        return;
                    }
                    boolean canceled = item.status != null && item.status.equalsIgnoreCase("CANCELED");
                    getStyleClass().remove("upcomingRow");
                    getStyleClass().remove("canceledRow");
                    getStyleClass().remove("adminCanceledRow");
                    if (canceled) getStyleClass().add("canceledRow");
                    if (item.canceledBy != null && item.canceledBy.equalsIgnoreCase("ADMIN")) {
                        getStyleClass().add("adminCanceledRow");
                    } else if (!canceled) {
                        LocalDate today = LocalDate.now();
                        boolean upcoming = item.bookingDeadline != null && item.bookingDeadline.isAfter(today);
                        if (upcoming) getStyleClass().add("upcomingRow");
                    }
                }
            });
            table.setOnMouseClicked(evt -> {
                if (evt.getClickCount() == 2) {
                    PackageBookingAdminView sel = table.getSelectionModel().getSelectedItem();
                    if (sel != null) {
                        var root = table.getScene().getRoot();
                        if (root instanceof javafx.scene.layout.Pane p) {
                            AdminBookingModalController.showPackage(p, sel);
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

    @FXML private void onBack() { Navigator.goAdminDashboard(); }

    @FXML private void onReload() { loadData(); }

    private void loadData() {
        status("Loading...");
        new Thread(() -> {
            try {
                List<PackageBookingAdminView> list = api.getAllPackageBookings();
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
        List<PackageBookingAdminView> filtered = all == null ? List.of() : all.stream()
                .filter(b -> matches(b, q))
                .collect(Collectors.toList());
        table.getItems().setAll(filtered);
    }

    private boolean matches(PackageBookingAdminView b, String q) {
        if (q.isBlank()) return true;
        return contains(b.customerName, q) || contains(b.userEmail, q) || contains(b.packageName, q)
                || contains(b.idNumber, q) || contains(b.idType, q) || contains(b.transactionId, q)
                || (b.totalPersons + "").contains(q);
    }

    private boolean contains(String s, String q) {
        return s != null && s.toLowerCase(Locale.ROOT).contains(q);
    }

    private void status(String s) { if (statusLabel != null) statusLabel.setText(s); }
}
