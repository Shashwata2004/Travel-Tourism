package com.travel.frontend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.travel.frontend.admin.AdminSession;
import com.travel.frontend.cache.DataCache;
import com.travel.frontend.model.PackageBookingAdminResponse;
import com.travel.frontend.model.PackageBookingAdminView;
import com.travel.frontend.net.ApiClient;
import com.travel.frontend.session.Session;
import com.travel.frontend.ui.Navigator;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TableCell;
import javafx.scene.control.TextArea;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.Region;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

public class AdminPackageBookingsController {

    @FXML private Label headerLabel;
    @FXML private Label subHeaderLabel;
    @FXML private Label statusLabel;
    @FXML private Label deadlineLabel;
    @FXML private Label totalPersonsLabel;
    @FXML private TableView<PackageBookingAdminView> bookingTable;
    @FXML private TableColumn<PackageBookingAdminView, java.time.Instant> createdCol;
    @FXML private TableColumn<PackageBookingAdminView, String> customerCol;
    @FXML private TableColumn<PackageBookingAdminView, String> emailCol;
    @FXML private TableColumn<PackageBookingAdminView, String> idTypeCol;
    @FXML private TableColumn<PackageBookingAdminView, String> idNumberCol;
    @FXML private TableColumn<PackageBookingAdminView, Integer> personsCol;
    @FXML private TableColumn<PackageBookingAdminView, java.math.BigDecimal> priceCol;
    @FXML private TableColumn<PackageBookingAdminView, String> txnCol;
    @FXML private TableColumn<PackageBookingAdminView, java.time.Instant> canceledCol;

    private final ApiClient api = ApiClient.get();
    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private UUID packageId;
    private String packageName;

    @FXML
    private void initialize() {
        String rawId = AdminPackagesState.getPackageId();
        if (rawId != null) {
            try { packageId = UUID.fromString(rawId.trim()); } catch (Exception ignored) {}
        }
        packageName = AdminPackagesState.getPackageName();
        if (packageId == null) {
            Navigator.goAdminDashboard();
            return;
        }
        if (headerLabel != null) headerLabel.setText("Bookings for package");
        if (subHeaderLabel != null) subHeaderLabel.setText(packageName == null ? "" : packageName);

        if (bookingTable != null) {
            bookingTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
            createdCol.setCellValueFactory(new PropertyValueFactory<>("createdAt"));
            customerCol.setCellValueFactory(new PropertyValueFactory<>("customerName"));
            emailCol.setCellValueFactory(new PropertyValueFactory<>("userEmail"));
            idTypeCol.setCellValueFactory(new PropertyValueFactory<>("idType"));
            idNumberCol.setCellValueFactory(new PropertyValueFactory<>("idNumber"));
            personsCol.setCellValueFactory(new PropertyValueFactory<>("totalPersons"));
            priceCol.setCellValueFactory(new PropertyValueFactory<>("priceTotal"));
            txnCol.setCellValueFactory(new PropertyValueFactory<>("transactionId"));
            canceledCol.setCellValueFactory(new PropertyValueFactory<>("canceledAt"));
            applyWrap(customerCol);
            applyWrap(emailCol);
            applyWrap(txnCol);
            bookingTable.setRowFactory(tv -> new javafx.scene.control.TableRow<>() {
                @Override
                protected void updateItem(PackageBookingAdminView item, boolean empty) {
                    super.updateItem(item, empty);
                    getStyleClass().remove("canceledRow");
                    if (!empty && item != null && "CANCELED".equalsIgnoreCase(item.getStatus())) {
                        getStyleClass().add("canceledRow");
                    }
                }
            });
        }
        loadData();
    }

    @FXML
    private void onReload() {
        if (packageId == null) return;
        DataCache.remove("admin:packageBookings:" + packageId);
        loadData();
    }

    @FXML
    private void onBack() {
        Navigator.goAdminDashboard();
    }

    private void loadData() {
        status("Loading bookings...");
        new Thread(() -> {
            try {
                if (AdminSession.isAuthenticated()) {
                    Session.setToken(AdminSession.getToken());
                }
                String key = "admin:packageBookings:" + packageId;
                PackageBookingAdminResponse resp = DataCache.getOrLoad(key, this::fetchBookings);
                Platform.runLater(() -> {
                    bookingTable.getItems().setAll(resp.items == null ? List.of() : resp.items);
                    totalPersonsLabel.setText(String.valueOf(resp.totalPersons));
                    if (resp.bookingDeadline != null) {
                        deadlineLabel.setText(resp.bookingDeadline.format(DateTimeFormatter.ISO_DATE));
                    } else {
                        deadlineLabel.setText("None");
                    }
                    status("Loaded " + (resp.items == null ? 0 : resp.items.size()) + " bookings");
                });
            } catch (Exception e) {
                Platform.runLater(() -> status("Error: " + e.getMessage()));
            }
        }).start();
    }

    private PackageBookingAdminResponse fetchBookings() throws Exception {
        var res = api.rawGet("/admin/packages/" + packageId + "/bookings", true);
        if (res.statusCode() != 200) throw new RuntimeException("HTTP " + res.statusCode() + ": " + res.body());
        return mapper.readValue(res.body(), PackageBookingAdminResponse.class);
    }

    private void status(String msg) { if (statusLabel != null) statusLabel.setText(msg); }

    // Wrap long text to avoid truncation without scrollbars
    private void applyWrap(TableColumn<PackageBookingAdminView, String> col) {
        col.setCellFactory(tc -> new TableCell<>() {
            private final javafx.scene.control.Label label = new javafx.scene.control.Label();
            {
                label.setWrapText(true);
                label.setMaxWidth(Double.MAX_VALUE);
                label.setPadding(new javafx.geometry.Insets(6, 10, 6, 10));
            }
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    label.setText(item);
                    label.setPrefWidth(col.getWidth() - 16); // fit within column
                    setGraphic(label);
                }
            }
        });
    }
}
