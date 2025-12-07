package com.travel.frontend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.travel.frontend.cache.DataCache;
import com.travel.frontend.cache.FileCache;
import com.travel.frontend.model.HistoryPackageItem;
import com.travel.frontend.model.HistoryResponse;
import com.travel.frontend.model.HistoryRoomItem;
import com.travel.frontend.net.ApiClient;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.control.Button;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.TextField;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextFlow;
import javafx.util.Duration;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ArrayList;
import java.util.Locale;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.math.BigDecimal;

/**
 * History page: renders booking cards and a slide-in detail panel using real
 * data
 * fetched from the backend /history endpoint.
 */
public class HistoryController {

    @FXML
    private VBox listContainer;
    @FXML
    private StackPane detailHolder;
    @FXML
    private NavbarController navbarController;
    @FXML
    private ToggleButton upcomingToggle;
    @FXML
    private ToggleButton pastToggle;
    @FXML
    private ToggleButton canceledToggle;
    @FXML
    private TextField searchField;
    @FXML
    private StackPane cancelOverlay;
    @FXML
    private StackPane cancelModalHolder;
    @FXML
    private Node cancelBackdrop;
    @FXML
    private javafx.scene.control.ScrollPane mainScroll;
    @FXML
    private Button reloadButton;
    @FXML
    private ScrollPane listScroll;

    private static final double DETAIL_WIDTH = 460;
    private static final String HOTEL_CACHE_VERSION = "v2";
    private final ApiClient api = ApiClient.get();
    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private final DateTimeFormatter df = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH);
    private HistoryResponse loaded;
    private static HistoryResponse cached;
    private static Instant cachedAt;
    private static boolean dirty = true;

    @FXML
    private void initialize() {
        if (navbarController != null) {
            navbarController.setActive(NavbarController.ActivePage.HISTORY);
        }
        setupFilters();
        loadHistory();
    }

    public static void markDirty() {
        dirty = true;
    }

    @FXML
    private void reloadHistory() {
        fetchHistory(true);
    }

    private void setupFilters() {
        if (upcomingToggle != null && pastToggle != null && canceledToggle != null) {
            ToggleGroup tg = new ToggleGroup();
            upcomingToggle.setToggleGroup(tg);
            pastToggle.setToggleGroup(tg);
            canceledToggle.setToggleGroup(tg);
            upcomingToggle.setSelected(true);
            upcomingToggle.setOnAction(e -> {
                renderFiltered();
                scrollCardsTop();
            });
            pastToggle.setOnAction(e -> {
                renderFiltered();
                scrollCardsTop();
            });
            canceledToggle.setOnAction(e -> {
                renderFiltered();
                scrollCardsTop();
            });
        }
        if (searchField != null) {
            searchField.textProperty().addListener((obs, o, n) -> renderFiltered());
        }
    }

    private void loadHistory() {
        fetchHistory(false);
    }

    private void fetchHistory(boolean force) {
        closeCancelFlow();
        resetDetailPane();
        Instant now = Instant.now();
        if (!force && !dirty && cached != null && cachedAt != null &&
                java.time.Duration.between(cachedAt, now).toMinutes() < 45) {
            loaded = cached;
            renderFiltered();
            return;
        }
        new Thread(() -> {
            try {
                var res = api.rawGet("/history", true);
                if (res.statusCode() != 200)
                    throw new RuntimeException("HTTP " + res.statusCode());
                HistoryResponse data = mapper.readValue(res.body(), HistoryResponse.class);
                Platform.runLater(() -> {
                    cached = data;
                    cachedAt = Instant.now();
                    dirty = false;
                    loaded = data;
                    renderFiltered();
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    listContainer.getChildren().clear();
                    listContainer.getChildren().add(errorCard("Failed to load history: " + e.getMessage()));
                });
            }
        }).start();
    }

    private void resetDetailPane() {
        detailHolder.getChildren().clear();
        detailHolder.setVisible(false);
        detailHolder.setOpacity(0);
        detailHolder.setTranslateX(DETAIL_WIDTH);
        detailHolder.setPrefWidth(0);
        listContainer.setScaleX(1);
        listContainer.setTranslateX(0);
    }

    private void renderFiltered() {
        HistoryResponse data = loaded;
        listContainer.getChildren().clear();

        LocalDate today = LocalDate.now();
        List<Node> upcoming = new ArrayList<>();
        List<Node> past = new ArrayList<>();
        List<Node> canceled = new ArrayList<>();

        if (data != null && data.rooms != null) {
            for (HistoryRoomItem r : data.rooms) {
                String status = roomStatus(r, today);
                if (!matchesSearch(r))
                    continue;
                Node card = buildRoomCard(r, status);
                switch (status.toLowerCase(Locale.ROOT)) {
                    case "past" -> past.add(card);
                    case "canceled" -> canceled.add(card);
                    default -> upcoming.add(card);
                }
            }
        }
        if (data != null && data.packages != null) {
            for (HistoryPackageItem p : data.packages) {
                String status = packageStatus(p, today);
                if (!matchesSearch(p))
                    continue;
                Node card = buildPackageCard(p, status);
                switch (status.toLowerCase(Locale.ROOT)) {
                    case "past" -> past.add(card);
                    case "canceled" -> canceled.add(card);
                    default -> upcoming.add(card);
                }
            }
        }

        List<String> order = statusPriority();
        boolean any = false;
        for (String s : order) {
            switch (s.toLowerCase(Locale.ROOT)) {
                case "upcoming" -> {
                    listContainer.getChildren().addAll(upcoming);
                    any |= !upcoming.isEmpty();
                }
                case "past" -> {
                    listContainer.getChildren().addAll(past);
                    any |= !past.isEmpty();
                }
                case "canceled" -> {
                    listContainer.getChildren().addAll(canceled);
                    any |= !canceled.isEmpty();
                }
            }
        }
        if (!any)
            listContainer.getChildren().add(emptyCard());
        scrollCardsTop();
    }

    private boolean shouldShow(String status) {
        if (upcomingToggle != null && pastToggle != null && canceledToggle != null) {
            if (upcomingToggle.isSelected())
                return "Upcoming".equalsIgnoreCase(status);
            if (pastToggle.isSelected())
                return "Past".equalsIgnoreCase(status);
            if (canceledToggle.isSelected())
                return "Canceled".equalsIgnoreCase(status);
        }
        return true;
    }

    private void setDetail(Node content) {
        detailHolder.getChildren().setAll(content);
        detailHolder.setVisible(true);

        detailHolder.setOpacity(0);
        detailHolder.setTranslateX(DETAIL_WIDTH);
        detailHolder.setPrefWidth(0);

        Timeline tl = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(detailHolder.opacityProperty(), 0),
                        new KeyValue(detailHolder.translateXProperty(), DETAIL_WIDTH),
                        new KeyValue(detailHolder.prefWidthProperty(), 0),
                        new KeyValue(listContainer.scaleXProperty(), 1),
                        new KeyValue(listContainer.translateXProperty(), 0)),
                new KeyFrame(Duration.millis(120),
                        new KeyValue(listContainer.scaleXProperty(), 0.97, Interpolator.EASE_BOTH),
                        new KeyValue(listContainer.translateXProperty(), -8, Interpolator.EASE_BOTH)),
                new KeyFrame(Duration.millis(320),
                        new KeyValue(detailHolder.opacityProperty(), 1, Interpolator.EASE_BOTH),
                        new KeyValue(detailHolder.translateXProperty(), 0, Interpolator.EASE_OUT),
                        new KeyValue(detailHolder.prefWidthProperty(), DETAIL_WIDTH, Interpolator.EASE_BOTH),
                        new KeyValue(listContainer.scaleXProperty(), 0.96, Interpolator.EASE_BOTH),
                        new KeyValue(listContainer.translateXProperty(), -16, Interpolator.EASE_BOTH)));
        tl.play();
    }

    private VBox buildHotelDetail(HistoryRoomItem r, String status) {
        VBox root = new VBox(12);
        root.getStyleClass().add("detailCard");
        root.setFillWidth(true);
        root.setPadding(new Insets(14));

        VBox header = new VBox(4);
        Label title = new Label(r.hotelName == null ? "Hotel booking" : r.hotelName);
        title.getStyleClass().add("detailTitle");
        Label subtitle = new Label("ID · " + (r.id == null ? "-" : r.id.toString()));
        subtitle.getStyleClass().add("detailSubtitle");
        String pillText = status == null ? "Upcoming" : status;
        HBox headRow = new HBox(8, header, spacer(), statusPill(pillText));
        header.getChildren().addAll(title, subtitle);
        headRow.setAlignment(Pos.CENTER_LEFT);
        root.getChildren().add(headRow);

        root.getChildren().add(section("Stay overview",
                kv("Check-in", fmt(r.checkIn)),
                kv("Check-out", fmt(r.checkOut)),
                kv("Room type", n(r.roomName)),
                kv("Guests / Rooms", (r.totalGuests == null ? "—" : r.totalGuests + " guests") +
                        " · " + (r.roomsBooked == null ? "— rooms" : r.roomsBooked + " room(s)"))));

        root.getChildren().add(section("Rooms & pricing",
                kv("Room", n(r.roomName)),
                kv("Total stay", stayDays(r.checkIn, r.checkOut)),
                kv("Total paid", r.totalPrice == null ? "BDT —" : "BDT " + r.totalPrice)));

        root.getChildren().add(section("Payment",
                kv("Status", "Paid"),
                kv("Method", "Card"),
                kv("Paid on", fmt(r.createdAt == null ? null
                        : r.createdAt.atZone(java.time.ZoneId.systemDefault()).toLocalDate()))));

        root.getChildren().add(section("Cancellation policy",
                paragraph(
                        "This hotel booking can be canceled free of charge up to 24 hours before check-in. Cancellations within the final 24 hours may incur a significant fee.")));

        root.getChildren().add(timeline(
                kv("Booked",
                        fmt(r.createdAt == null ? null
                                : r.createdAt.atZone(java.time.ZoneId.systemDefault()).toLocalDate())),
                kv("Check-in", fmt(r.checkIn)),
                kv("Check-out", fmt(r.checkOut))));

        root.getChildren().add(section("Documents",
                pillButton("Download invoice (PDF)"),
                pillButton("Download receipt")));

        root.getChildren().add(section("Need help?",
                pillButton("Contact support about this booking")));

        return root;
    }

    private VBox buildPackageDetail(HistoryPackageItem p, String status) {
        VBox root = new VBox(12);
        root.getStyleClass().add("detailCard");
        root.setFillWidth(true);
        root.setPadding(new Insets(14));

        VBox header = new VBox(4);
        Label title = new Label(p.packageName == null ? "Package booking" : p.packageName);
        title.getStyleClass().add("detailTitle");
        Label subtitle = new Label("Booking ID · " + (p.id == null ? "-" : p.id.toString()));
        subtitle.getStyleClass().add("detailSubtitle");
        header.getChildren().addAll(title, subtitle);

        VBox pillsBox = new VBox(6);
        pillsBox.setAlignment(Pos.TOP_RIGHT);
        String pillText = status == null ? "Upcoming" : status;
        pillsBox.getChildren().addAll(pill("Package", "pillType"), statusPill(pillText));

        HBox headRow = new HBox(8, header, spacer(), pillsBox);
        headRow.setAlignment(Pos.CENTER_LEFT);
        root.getChildren().add(headRow);

        LocalDate start = p.bookingDeadline != null ? p.bookingDeadline.plusDays(2) : null;
        LocalDate end = start;
        if (start != null && p.durationDays != null && p.durationDays > 0) {
            end = start.plusDays(p.durationDays);
        }

        root.getChildren().add(section("Trip dates",
                kv("Start date", fmt(start)),
                kv("End date", fmt(end)),
                kv("Guests", p.totalPersons == null ? "—" : p.totalPersons + " persons")));

        root.getChildren().add(section("Package & pricing",
                kv("Package", n(p.packageName)),
                kv("Location", n(p.location)),
                kv("Total paid", p.totalPrice == null ? "BDT —" : "BDT " + p.totalPrice),
                kv("Breakdown", p.totalPrice == null || p.totalPersons == null
                        ? "—"
                        : "BDT " + p.totalPrice.divide(java.math.BigDecimal.valueOf(Math.max(1, p.totalPersons)))
                                + " × " + p.totalPersons + " guests")));

        root.getChildren().add(section("Payment",
                kv("Status", "Paid"),
                kv("Method", "Card"),
                kv("Paid on", fmt(p.createdAt == null ? null
                        : p.createdAt.atZone(java.time.ZoneId.systemDefault()).toLocalDate()))));

        root.getChildren().add(section("Cancellation policy",
                paragraph(
                        "This package can be canceled free of charge within 24 hours after it is booked. After that, the booking becomes non-refundable.")));

        root.getChildren().add(timeline(
                kv("Booked on",
                        fmt(p.createdAt == null ? null
                                : p.createdAt.atZone(java.time.ZoneId.systemDefault()).toLocalDate())),
                kv("Start date", fmt(start)),
                kv("End date", fmt(end))));

        root.getChildren().add(section("Documents",
                pillButton("Invoice (PDF)"),
                pillButton("Receipt (PDF)")));

        root.getChildren().add(section("Help",
                pillButton("Contact support")));

        return root;
    }

    private VBox buildRoomCard(HistoryRoomItem r, String status) {
        VBox card = new VBox(10);
        card.getStyleClass().add("bookingCard");
        HBox top = new HBox(10);
        top.setAlignment(Pos.CENTER_LEFT);

        VBox info = new VBox(4);
        Label name = new Label(n(r.hotelName));
        name.getStyleClass().add("cardTitle");
        Label sub = new Label(n(r.roomName));
        sub.getStyleClass().add("cardSubtitle");
        info.getChildren().addAll(name, sub);
        info.getChildren().add(row("Check-in", fmt(r.checkIn), "Check-out", fmt(r.checkOut)));
        info.getChildren().add(row("Guests", r.totalGuests == null ? "—" : r.totalGuests.toString(),
                "Rooms", r.roomsBooked == null ? "—" : r.roomsBooked.toString()));
        if (r.totalPrice != null) {
            Label note = new Label("Total paid: BDT " + r.totalPrice);
            note.getStyleClass().add("cardNote");
            info.getChildren().add(note);
        }
        if (r.createdAt != null) {
            Label bookedOn = new Label(
                    "Booked on " + fmt(r.createdAt.atZone(java.time.ZoneId.systemDefault()).toLocalDate()));
            bookedOn.getStyleClass().add("cardNote");
            info.getChildren().add(bookedOn);
        }

        VBox right = new VBox(6);
        right.setAlignment(Pos.CENTER_RIGHT);
        if (!"Canceled".equalsIgnoreCase(status)) {
            right.getChildren().add(statusPill(status));
        }
        Label price = new Label(r.totalPrice == null ? "BDT —" : "BDT " + r.totalPrice);
        price.getStyleClass().add("cardPrice");
        right.getChildren().add(price);
        right.getChildren().add(labelMeta("ID · " + (r.id == null ? "-" : r.id.toString())));
        if ("Canceled".equalsIgnoreCase(status) && r.canceledAt != null) {
            Label canceledOn = new Label("Canceled on " + fmt(r.canceledAt.atZone(java.time.ZoneId.systemDefault()).toLocalDate()));
            canceledOn.getStyleClass().add("cardNote");
            right.getChildren().add(canceledOn);
        }

        top.getChildren().addAll(info, spacer(), right);
        card.getChildren().add(top);

        HBox actions = new HBox(10);
        actions.setAlignment(Pos.CENTER_RIGHT);
        if ("Canceled".equalsIgnoreCase(status)) {
            Button canceled = new Button("Canceled");
            canceled.getStyleClass().addAll("cancel-accent", "canceledPillAction");
            canceled.setDisable(true);
            canceled.setMouseTransparent(true);
            canceled.setMinWidth(160);
            actions.getChildren().add(canceled);
            card.getStyleClass().add("canceledCard");
        } else {
            Button view = new Button("View details");
            view.getStyleClass().add("ghostButton");
            view.setOnAction(e -> setDetail(buildHotelDetail(r, status)));
            Button cancel = new Button("Cancel booking");
            cancel.getStyleClass().add("accentButton");
            cancel.setOnAction(e -> openCancelFlow(r));
            if ("Past".equalsIgnoreCase(status)) {
                actions.getChildren().add(view);
            } else {
                actions.getChildren().addAll(view, cancel);
            }
        }
        card.getChildren().add(actions);
        if ("Canceled".equalsIgnoreCase(status)) {
            card.setOpacity(0.82);
        }
        return card;
    }

    private VBox buildPackageCard(HistoryPackageItem p, String status) {
        VBox card = new VBox(10);
        card.getStyleClass().add("bookingCard");
        HBox top = new HBox(10);
        top.setAlignment(Pos.CENTER_LEFT);

        VBox info = new VBox(4);
        Label name = new Label(n(p.packageName));
        name.getStyleClass().add("cardTitle");
        name.setWrapText(false);
        name.setMaxWidth(Double.MAX_VALUE);
        Label sub = new Label(n(p.location));
        sub.getStyleClass().add("cardSubtitle");
        sub.setWrapText(false);
        sub.setMaxWidth(Double.MAX_VALUE);
        info.getChildren().addAll(name, sub);
        LocalDate start = p.bookingDeadline != null ? p.bookingDeadline.plusDays(2) : null;
        LocalDate end = start;
        if (start != null && p.durationDays != null && p.durationDays > 0) {
            end = start.plusDays(p.durationDays);
        }
        info.getChildren().add(row("Start", fmt(start), "End", fmt(end)));
        info.getChildren().add(row("Guests", p.totalPersons == null ? "—" : p.totalPersons.toString(), "", ""));
        info.getChildren().add(labelNote("Package booked on " + fmt(
                p.createdAt == null ? null : p.createdAt.atZone(java.time.ZoneId.systemDefault()).toLocalDate())));
        info.setPrefWidth(0);
        HBox.setHgrow(info, javafx.scene.layout.Priority.ALWAYS);
        info.setFillWidth(true);

        VBox right = new VBox(6);
        right.setAlignment(Pos.TOP_RIGHT);
        right.setMinWidth(140);
        right.setMaxWidth(200);
        right.setFillWidth(false);
        VBox pills = new VBox(4);
        pills.getChildren().add(pill("Package", "packagePill"));
        if (!"Canceled".equalsIgnoreCase(status)) {
            pills.getChildren().add(statusPill(status));
        }
        pills.setAlignment(Pos.TOP_RIGHT);
        right.getChildren().add(pills);
        Label price = new Label(p.totalPrice == null ? "BDT —" : "BDT " + p.totalPrice);
        price.getStyleClass().add("cardPrice");
        right.getChildren().add(price);
        right.getChildren().add(labelMeta("ID · " + (p.id == null ? "-" : p.id.toString())));
        if ("Canceled".equalsIgnoreCase(status) && p.canceledAt != null) {
            Label canceledOn = new Label("Canceled on " + fmt(p.canceledAt.atZone(java.time.ZoneId.systemDefault()).toLocalDate()));
            canceledOn.getStyleClass().add("cardNote");
            right.getChildren().add(canceledOn);
        }

        top.getChildren().addAll(info, spacer(), right);
        card.getChildren().add(top);

        HBox actions = new HBox(10);
        actions.setAlignment(Pos.CENTER_RIGHT);
        if ("Canceled".equalsIgnoreCase(status)) {
            Button canceled = new Button("Canceled");
            canceled.getStyleClass().addAll("cancel-accent", "canceledPillAction");
            canceled.setDisable(true);
            canceled.setMouseTransparent(true);
            canceled.setMinWidth(160);
            actions.getChildren().add(canceled);
            card.getStyleClass().add("canceledCard");
        } else {
            Button view = new Button("View details");
            view.getStyleClass().add("ghostButton");
            view.setOnAction(e -> setDetail(buildPackageDetail(p, status)));
            Button cancel = new Button("Cancel booking");
            cancel.getStyleClass().add("accentButton");
            cancel.setOnAction(e -> openCancelFlow(p));
            if ("Past".equalsIgnoreCase(status)) {
                actions.getChildren().add(view);
            } else {
                actions.getChildren().addAll(view, cancel);
            }
        }
        card.getChildren().add(actions);
        if ("Canceled".equalsIgnoreCase(status)) {
            card.setOpacity(0.82);
        }
        return card;
    }

    private String roomStatus(HistoryRoomItem r, LocalDate today) {
        if (r.status != null && "CANCELED".equalsIgnoreCase(r.status)) return "Canceled";
        if (r.canceledAt != null) return "Canceled";
        // Consider past if check-in is today or earlier (midnight cutoff), or check-out
        // is today/past
        if (r.checkIn != null && !r.checkIn.isAfter(today))
            return "Past";
        if (r.checkOut != null && !r.checkOut.isAfter(today))
            return "Past";
        return "Upcoming";
    }

    private String packageStatus(HistoryPackageItem p, LocalDate today) {
        if (p.status != null && "CANCELED".equalsIgnoreCase(p.status)) return "Canceled";
        if (p.canceledAt != null) return "Canceled";
        LocalDate start = p.bookingDeadline != null ? p.bookingDeadline.plusDays(2) : null;
        if (start != null && !start.isAfter(today))
            return "Past";
        return "Upcoming";
    }

    private boolean matchesSearch(HistoryRoomItem r) {
        String q = searchQuery();
        if (q.isEmpty())
            return true;
        return (r.hotelName != null && r.hotelName.toLowerCase(Locale.ROOT).contains(q))
                || (r.roomName != null && r.roomName.toLowerCase(Locale.ROOT).contains(q))
                || (r.id != null && r.id.toString().toLowerCase(Locale.ROOT).contains(q));
    }

    private boolean matchesSearch(HistoryPackageItem p) {
        String q = searchQuery();
        if (q.isEmpty())
            return true;
        return (p.packageName != null && p.packageName.toLowerCase(Locale.ROOT).contains(q))
                || (p.location != null && p.location.toLowerCase(Locale.ROOT).contains(q))
                || (p.id != null && p.id.toString().toLowerCase(Locale.ROOT).contains(q));
    }

    private String searchQuery() {
        if (searchField == null || searchField.getText() == null)
            return "";
        return searchField.getText().trim().toLowerCase(Locale.ROOT);
    }

    private List<String> statusPriority() {
        if (upcomingToggle != null && upcomingToggle.isSelected()) {
            return List.of("Upcoming", "Past", "Canceled");
        }
        if (pastToggle != null && pastToggle.isSelected()) {
            return List.of("Past", "Upcoming", "Canceled");
        }
        if (canceledToggle != null && canceledToggle.isSelected()) {
            return List.of("Canceled", "Upcoming", "Past");
        }
        return List.of("Upcoming", "Past", "Canceled");
    }

    private HBox row(String k1, String v1, String k2, String v2) {
        HBox row = new HBox(6);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getChildren().add(labelMeta(k1));
        row.getChildren().add(labelValue(v1));
        if (k2 != null && !k2.isBlank()) {
            row.getChildren().add(labelMeta("· " + k2));
            row.getChildren().add(labelValue(v2));
        }
        return row;
    }

    private Label labelMeta(String t) {
        Label l = new Label(t);
        l.getStyleClass().add("cardMeta");
        return l;
    }

    private Label labelValue(String t) {
        Label l = new Label(t);
        l.getStyleClass().add("cardValue");
        return l;
    }

    private Label labelNote(String t) {
        Label l = new Label(t);
        l.getStyleClass().add("cardNote");
        return l;
    }

    private VBox noteCard() {
        VBox v = new VBox(6);
        v.getStyleClass().add("noteCard");
        Label t = new Label("Cancellation rules");
        t.getStyleClass().add("noteTitle");
        Label b = new Label(
                "Hotel bookings can be canceled up to 24 hours before check-in. Package bookings can be canceled within 24 hours after booking.");
        b.setWrapText(true);
        b.getStyleClass().add("noteBody");
        v.getChildren().addAll(t, b);
        return v;
    }

    private VBox emptyCard() {
        VBox v = new VBox(10);
        v.setAlignment(Pos.CENTER);
        v.getStyleClass().add("emptyCard");
        Label t = new Label("No bookings yet");
        t.getStyleClass().add("cardTitle");
        Label b = new Label("When you book a stay or package, it will appear here with its cancellation rules.");
        b.setWrapText(true);
        b.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        b.getStyleClass().add("cardNote");
        v.getChildren().addAll(t, b);
        return v;
    }

    private VBox errorCard(String msg) {
        VBox v = new VBox(8);
        v.setAlignment(Pos.CENTER_LEFT);
        v.getStyleClass().add("bookingCard");
        Label t = new Label("Error");
        t.getStyleClass().add("cardTitle");
        Label b = new Label(msg);
        b.setWrapText(true);
        b.getStyleClass().add("cardNote");
        v.getChildren().addAll(t, b);
        return v;
    }

    private VBox section(String title, Node... rows) {
        VBox box = new VBox(6);
        box.getStyleClass().add("detailSection");
        box.setFillWidth(true);
        box.setMaxWidth(Double.MAX_VALUE);
        Label t = new Label(title);
        t.getStyleClass().add("sectionHeader");
        box.getChildren().add(t);
        VBox inner = new VBox(4);
        inner.setFillWidth(true);
        inner.setMaxWidth(Double.MAX_VALUE);
        for (Node r : rows) {
            if (r instanceof Label lbl && lbl.isWrapText()) {
                lbl.setTextOverrun(javafx.scene.control.OverrunStyle.CLIP);
                lbl.setMaxWidth(Double.MAX_VALUE);
                lbl.setPrefHeight(Region.USE_COMPUTED_SIZE);
                lbl.prefWidthProperty().bind(box.widthProperty().subtract(24));
            } else if (r instanceof TextFlow flow) {
                flow.setTextAlignment(javafx.scene.text.TextAlignment.LEFT);
                flow.setLineSpacing(2);
                flow.prefWidthProperty().bind(box.widthProperty().subtract(24));
                flow.maxWidthProperty().bind(box.widthProperty().subtract(24));
            }
            inner.getChildren().add(r);
        }
        box.getChildren().add(inner);
        return box;
    }

    private HBox kv(String key, String value) {
        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);
        Label k = new Label(key);
        k.getStyleClass().add("kvKey");
        Label v = new Label(value);
        v.getStyleClass().add("kvValue");
        v.setWrapText(true);
        Region spacer = new Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
        row.getChildren().addAll(k, spacer, v);
        return row;
    }

    private VBox timeline(Node... items) {
        VBox box = new VBox(6);
        box.getStyleClass().add("detailSection");
        Label t = new Label("Timeline");
        t.getStyleClass().add("sectionHeader");
        box.getChildren().add(t);
        FlowPane line = new FlowPane();
        line.setHgap(20);
        line.setVgap(10);
        line.setPrefWrapLength(600);
        line.setRowValignment(javafx.geometry.VPos.TOP);
        line.setAlignment(Pos.TOP_LEFT);
        line.setMaxWidth(Double.MAX_VALUE);
        for (Node n : items) {
            line.getChildren().add(n);
        }
        box.getChildren().add(line);
        return box;
    }

    private Node paragraph(String text) {
        javafx.scene.text.Text t = new javafx.scene.text.Text(text);
        t.getStyleClass().add("paragraph");
        javafx.scene.text.TextFlow flow = new javafx.scene.text.TextFlow(t);
        flow.getStyleClass().add("paragraphFlow");
        flow.setMaxWidth(Double.MAX_VALUE);
        flow.setPrefWidth(Double.MAX_VALUE);
        t.wrappingWidthProperty().bind(flow.widthProperty());
        return flow;
    }

    private Label pill(String text, String style) {
        Label l = new Label(text);
        l.getStyleClass().add(style);
        l.setWrapText(false);
        l.setMinWidth(Region.USE_PREF_SIZE);
        l.setMaxWidth(Region.USE_PREF_SIZE);
        l.setTextOverrun(javafx.scene.control.OverrunStyle.CLIP);
        return l;
    }

    private Label statusPill(String status) {
        return statusPill(status, false);
    }

    private Label statusPill(String status, boolean emphasized) {
        String s = status == null ? "Status" : status;
        String cls = "statusPill";
        if ("Canceled".equalsIgnoreCase(s)) cls = "canceledPill";
        Label l = pill(s, cls);
        if (emphasized) l.getStyleClass().add("canceledPillEmphasis");
        return l;
    }

    private Button pillButton(String text) {
        Button b = new Button(text);
        b.getStyleClass().add("pillButton");
        return b;
    }

    private Region spacer() {
        Region r = new Region();
        HBox.setHgrow(r, javafx.scene.layout.Priority.ALWAYS);
        return r;
    }

    private void openCancelFlow(HistoryRoomItem r) {
        CancelContext ctx = buildCancelContext(r);
        showCancelStep1(ctx);
    }

    private void openCancelFlow(HistoryPackageItem p) {
        CancelContext ctx = buildCancelContext(p);
        showCancelStep1(ctx);
    }

    private CancelContext buildCancelContext(HistoryRoomItem r) {
        String txn = resolveTxn(r.id, r.transactionId);
        FeeBreakdown fb = calcFee(r.totalPrice, r.checkIn);
        return new CancelContext(CancelKind.ROOM, r.id, r.hotelId, r.roomId, txn, fb.refundStr, fb.feeStr,
                cardMethod(r.cardLast4), false);
    }

    private CancelContext buildCancelContext(HistoryPackageItem p) {
        String txn = resolveTxn(p.id, p.transactionId);
        boolean withinWindow = true;
        if (p.createdAt != null) {
            long hoursSince = ChronoUnit.HOURS.between(p.createdAt, Instant.now());
            withinWindow = hoursSince <= 24;
        }
        BigDecimal total = p.totalPrice == null ? BigDecimal.ZERO : p.totalPrice;
        final boolean isWithin = withinWindow;
        FeeBreakdown fb;
        if (isWithin) {
            fb = new FeeBreakdown(formatMoney(total), formatMoney(BigDecimal.ZERO));
        } else {
            fb = new FeeBreakdown(formatMoney(BigDecimal.ZERO), formatMoney(total));
        }
        return new CancelContext(CancelKind.PACKAGE, p.id, null, null, txn, fb.refundStr, fb.feeStr,
                cardMethod(p.cardLast4), !isWithin);
    }

    private String resolveTxn(java.util.UUID id, String txn) {
        return txn == null || txn.isBlank()
                ? "TXN-" + (id == null ? "000000" : id.toString().replace("-", "").substring(0, 6).toUpperCase(Locale.ENGLISH))
                : txn;
    }

    private void showCancelStep1(CancelContext ctx) {
        String infoText = ctx.nonRefundable
                ? "This booking is non-refundable. If you cancel now, no refund will be issued."
                : "Are you sure you want to cancel this booking? You will receive your refund within 3–7 business days.";
        String subtitle = ctx.nonRefundable
                ? "This booking is non-refundable"
                : "Please confirm your cancellation";
        VBox modal = new VBox(16);
        modal.getStyleClass().add("cancel-modal");

        StackPane header = new StackPane();
        header.getStyleClass().add("cancel-header");
        VBox headerText = new VBox(4);
        headerText.setAlignment(Pos.CENTER_LEFT);
        headerText.getChildren().addAll(
                new Label("Cancel Booking?"),
                new Label(subtitle));
        headerText.getChildren().forEach(n -> n.getStyleClass().add("cancel-header-text"));
        Button close = new Button("✕");
        close.getStyleClass().add("cancel-close");
        close.setOnAction(e -> closeCancelFlow());
        StackPane.setAlignment(close, Pos.TOP_RIGHT);
        header.getChildren().addAll(headerText, close);

        Label info = new Label(infoText);
        info.getStyleClass().add("cancel-body");
        info.setWrapText(true);

        VBox detailCard = new VBox(10);
        detailCard.getStyleClass().add("cancel-detail");
        detailCard.getChildren().addAll(
                cancelRow("Transaction ID", ctx.txn),
                cancelRow("Refund amount", ctx.refund),
                cancelRow("Payment method", ctx.method),
                cancelRow("Cancellation fee", ctx.fee));

        HBox actions = new HBox(10);
        actions.setAlignment(Pos.CENTER);
        actions.setMaxWidth(Double.MAX_VALUE);
        Button keep = new Button("Keep Booking");
        keep.getStyleClass().add("cancel-ghost");
        keep.setOnAction(e -> closeCancelFlow());
        Button confirm = new Button("Cancel Booking");
        confirm.getStyleClass().add("cancel-accent");
        confirm.setOnAction(e -> beginCancellation(ctx));
        actions.getChildren().addAll(keep, confirm);
        HBox.setHgrow(keep, javafx.scene.layout.Priority.ALWAYS);
        HBox.setHgrow(confirm, javafx.scene.layout.Priority.ALWAYS);
        VBox.setMargin(actions, new Insets(6, 0, 0, 0));

        modal.getChildren().addAll(header, info, detailCard, actions);
        showModal(modal);
    }

    private void beginCancellation(CancelContext ctx) {
        showCancelStep2(ctx);
    }

    private void showCancelStep2(CancelContext ctx) {
        VBox modal = new VBox(16);
        modal.getStyleClass().add("cancel-modal");

        StackPane header = new StackPane();
        header.getStyleClass().add("cancel-header");
        VBox headerText = new VBox(4);
        headerText.setAlignment(Pos.CENTER_LEFT);
        headerText.getChildren().addAll(
                new Label("Processing your cancellation..."),
                new Label("Please wait while we process your request"));
        headerText.getChildren().forEach(n -> n.getStyleClass().add("cancel-header-text"));
        header.getChildren().add(headerText);

        Label desc = new Label(ctx.nonRefundable
                ? "We're canceling your booking. This booking is non-refundable, so no refund will be issued."
                : "We're canceling your booking and starting your refund request.");
        desc.setWrapText(true);
        desc.getStyleClass().add("cancel-body");

        VBox status = new VBox(12);
        status.getChildren().addAll(
                statusRow("Booking canceled", "Your reservation has been canceled", true),
                statusRow(ctx.nonRefundable ? "No refund" : "Refund initiated",
                        ctx.nonRefundable ? "This booking is non-refundable." : "Processing your refund...", false));

        ProgressBar bar = new ProgressBar(0);
        bar.getStyleClass().add("cancel-progress");
        bar.setPrefWidth(Double.MAX_VALUE);
        Label timing = new Label("Canceling...");
        timing.getStyleClass().add("status-desc");

        modal.getChildren().addAll(header, desc, status, bar, timing);
        showModal(modal);

        // Animate progress toward 90% while waiting; final value is set after API returns.
        Timeline progressAnim = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(bar.progressProperty(), 0)),
                new KeyFrame(Duration.seconds(3), new KeyValue(bar.progressProperty(), 0.9, Interpolator.EASE_BOTH)));
        progressAnim.setCycleCount(Timeline.INDEFINITE);
        progressAnim.play();

        new Thread(() -> {
            Instant started = Instant.now();
            Instant canceledAt = null;
            String error = null;
            try {
                if (ctx.kind == CancelKind.ROOM) {
                    var res = api.cancelRoomBooking(ctx.bookingId);
                    canceledAt = res.canceledAt;
                } else {
                    var res = api.cancelPackageBooking(ctx.bookingId);
                    canceledAt = res.canceledAt;
                }
            } catch (ApiClient.ApiException ex) {
                error = ex.getMessage();
            } catch (Exception ex) {
                error = "Unexpected error: " + ex.getMessage();
            }
            long elapsed = java.time.Duration.between(started, Instant.now()).toMillis();
            Instant finalCanceledAt = canceledAt == null ? Instant.now() : canceledAt;
            final String errorMsg = error;
            if (errorMsg != null) {
                Platform.runLater(() -> {
                    progressAnim.stop();
                    showCancelError(errorMsg);
                });
            } else {
                Platform.runLater(() -> {
                    progressAnim.stop();
                    bar.setProgress(1);
                    timing.setText("Completed in " + prettyMillis(elapsed));
                    afterCancellation(ctx);
                    Timeline tl = new Timeline(new KeyFrame(Duration.millis(220), ev -> showCancelStep3(ctx, finalCanceledAt, elapsed)));
                    tl.play();
                });
            }
        }).start();
    }

    private void showCancelStep3(CancelContext ctx, Instant canceledAt, long elapsedMs) {
        VBox modal = new VBox(16);
        modal.getStyleClass().add("cancel-modal");

        StackPane header = new StackPane();
        header.getStyleClass().add("success-header");
        VBox headerText = new VBox(4);
        headerText.setAlignment(Pos.CENTER_LEFT);
        headerText.getChildren().addAll(
                new Label("Booking Canceled Successfully"),
                new Label("Your refund has been initiated"));
        headerText.getChildren().forEach(n -> n.getStyleClass().add("cancel-header-text"));
        header.getChildren().add(headerText);

        StackPane tickWrap = new StackPane();
        tickWrap.setPrefHeight(120);
        tickWrap.setMinHeight(120);
        tickWrap.setMaxHeight(140);
        tickWrap.getStyleClass().add("tick-wrap");
        javafx.scene.shape.Circle glow = new javafx.scene.shape.Circle(36);
        glow.getStyleClass().add("tick-glow");
        javafx.scene.shape.SVGPath tick = new javafx.scene.shape.SVGPath();
        tick.setContent("M9 16.2 4.8 12l-1.4 1.4L9 19 21 7l-1.4-1.4z");
        tick.getStyleClass().add("tick-icon");
        tickWrap.getChildren().addAll(glow, tick);
        tickWrap.setAlignment(Pos.CENTER);
        // Pop animation
        Timeline pop = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(glow.scaleXProperty(), 0),
                        new KeyValue(glow.scaleYProperty(), 0),
                        new KeyValue(tick.scaleXProperty(), 0),
                        new KeyValue(tick.scaleYProperty(), 0),
                        new KeyValue(tick.opacityProperty(), 0)),
                new KeyFrame(Duration.millis(450),
                        new KeyValue(glow.scaleXProperty(), 1, Interpolator.EASE_OUT),
                        new KeyValue(glow.scaleYProperty(), 1, Interpolator.EASE_OUT),
                        new KeyValue(tick.scaleXProperty(), 2.5, Interpolator.EASE_OUT),
                        new KeyValue(tick.scaleYProperty(), 2.5, Interpolator.EASE_OUT),
                        new KeyValue(tick.opacityProperty(), 1, Interpolator.EASE_OUT)));
        pop.play();

        String elapsedTxt = prettyMillis(elapsedMs);
        Label body = new Label(ctx.nonRefundable
                ? "Your booking has been canceled.\nThis booking was non-refundable, so no refund will be issued."
                : "Your refund of " + ctx.refund
                        + " has been initiated.\nYou'll receive the amount in 3–7 business days via " + ctx.method + ".");
        body.setWrapText(true);
        body.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        body.getStyleClass().add("cancel-body");
        body.setAlignment(Pos.CENTER);
        body.setMaxWidth(Double.MAX_VALUE);
        body.setPrefWidth(Double.MAX_VALUE);
        VBox.setMargin(body, new Insets(0, 12, 0, 12));

        Label txnLabel = new Label("Transaction ID: " + ctx.txn);
        txnLabel.getStyleClass().add("cancel-body");
        txnLabel.setAlignment(Pos.CENTER);
        txnLabel.setMaxWidth(Double.MAX_VALUE);
        txnLabel.setPrefWidth(Double.MAX_VALUE);
        txnLabel.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

        Label canceledOn = new Label("Canceled at " + fmt(canceledAt.atZone(java.time.ZoneId.systemDefault()).toLocalDate())
                + " • " + elapsedTxt);
        canceledOn.getStyleClass().add("status-desc");
        canceledOn.setAlignment(Pos.CENTER);
        canceledOn.setMaxWidth(Double.MAX_VALUE);

        Button done = new Button("Done");
        done.getStyleClass().add("cancel-accent");
        done.setMaxWidth(Double.MAX_VALUE);
        done.setOnAction(e -> closeCancelFlow());

        modal.getChildren().addAll(header, tickWrap, body, txnLabel, canceledOn, done);
        showModal(modal);
    }

    private void showCancelError(String msg) {
        closeCancelFlow();
        new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK).showAndWait();
    }

    private void afterCancellation(CancelContext ctx) {
        markDirty();
        fetchHistory(true);
        if (ctx.hotelId != null) {
            String key = "hotel:details:" + HOTEL_CACHE_VERSION + ":" + ctx.hotelId;
            DataCache.remove(key);
            FileCache.remove("hotel_details_" + HOTEL_CACHE_VERSION + "_" + ctx.hotelId);
        }
    }

    private HBox cancelRow(String key, String value) {
        Label k = new Label(key);
        k.getStyleClass().add("cancel-row-key");
        Label v = new Label(value);
        v.getStyleClass().add("cancel-row-val");
        if ("Refund amount".equalsIgnoreCase(key)) {
            v.getStyleClass().add("refund-amount");
        } else if ("Cancellation fee".equalsIgnoreCase(key)) {
            v.getStyleClass().add("fee-amount");
        }
        Button copyBtn = new Button("Copy");
        copyBtn.getStyleClass().add("cancel-copy");
        copyBtn.setVisible("Transaction ID".equalsIgnoreCase(key));
        copyBtn.setManaged(copyBtn.isVisible());
        copyBtn.setOnAction(e -> {
            ClipboardContent cc = new ClipboardContent();
            cc.putString(value);
            Clipboard.getSystemClipboard().setContent(cc);
        });
        HBox row = new HBox(10, k, spacer(), v, copyBtn);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("cancel-row");
        return row;
    }

    private VBox statusRow(String title, String desc, boolean done) {
        VBox box = new VBox(2);
        Label t = new Label(title);
        t.getStyleClass().add(done ? "status-done" : "status-pending");
        Label d = new Label(desc);
        d.getStyleClass().add("status-desc");
        box.getChildren().addAll(t, d);
        box.getStyleClass().add("status-row");
        return box;
    }

    private String cardMethod(String last4) {
        String suffix = (last4 == null || last4.isBlank()) ? "•••• 0000" : "•••• " + last4.trim();
        return "Visa " + suffix;
    }

    private FeeBreakdown calcFee(BigDecimal total, LocalDate startDate) {
        BigDecimal zero = BigDecimal.ZERO;
        if (total == null || startDate == null) {
            return new FeeBreakdown("BDT 0", "BDT 0");
        }
        ZoneId zone = ZoneId.systemDefault();
        long hoursLeft = Math.max(0, ChronoUnit.HOURS.between(Instant.now(), startDate.atStartOfDay(zone).toInstant()));
        double percent;
        if (hoursLeft >= 24) {
            percent = 0.0;
        } else {
            double ratio = 1 - (hoursLeft / 24.0);
            // Softer ramp: starts at ~15% fee at 24h, up to max 80% at 0h
            percent = 0.15 + 0.65 * ratio;
            percent = Math.min(1.0, Math.max(0.0, percent));
        }
        BigDecimal fee = total.multiply(BigDecimal.valueOf(percent));
        if (fee.compareTo(total) > 0)
            fee = total;
        BigDecimal refund = total.subtract(fee);
        if (refund.compareTo(zero) < 0)
            refund = zero;
        return new FeeBreakdown(formatMoney(refund), formatMoney(fee));
    }

    private String formatMoney(BigDecimal amount) {
        if (amount == null)
            return "BDT 0";
        return "BDT " + amount.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
    }

    private String prettyMillis(long ms) {
        if (ms < 1000) return ms + " ms";
        double seconds = ms / 1000.0;
        return String.format(Locale.ENGLISH, "%.1f s", seconds);
    }

    private record FeeBreakdown(String refundStr, String feeStr) {
    }

    private enum CancelKind { ROOM, PACKAGE }

    private static class CancelContext {
        final CancelKind kind;
        final java.util.UUID bookingId;
        final java.util.UUID hotelId;
        final java.util.UUID roomId;
        final String txn;
        final String refund;
        final String fee;
        final String method;
        final boolean nonRefundable;

        CancelContext(CancelKind kind, java.util.UUID bookingId, java.util.UUID hotelId, java.util.UUID roomId,
                      String txn, String refund, String fee, String method,
                      boolean nonRefundable) {
            this.kind = kind;
            this.bookingId = bookingId;
            this.hotelId = hotelId;
            this.roomId = roomId;
            this.txn = txn;
            this.refund = refund;
            this.fee = fee;
            this.method = method;
            this.nonRefundable = nonRefundable;
        }
    }

    private void showPackageTooLateModal() {
        VBox modal = new VBox(14);
        modal.getStyleClass().add("cancel-modal");

        StackPane header = new StackPane();
        header.getStyleClass().add("cancel-header");
        VBox headerText = new VBox(4);
        headerText.setAlignment(Pos.CENTER_LEFT);
        Label t1 = new Label("Cancellation not available");
        Label t2 = new Label("Packages can only be canceled within 24 hours of booking.");
        t1.getStyleClass().add("cancel-header-text");
        t2.getStyleClass().add("cancel-header-text");
        headerText.getChildren().addAll(t1, t2);
        header.getChildren().add(headerText);

        Label body = new Label("This package is now outside the 24-hour cancellation window and is non-refundable.");
        body.setWrapText(true);
        body.getStyleClass().add("cancel-body");

        Button ok = new Button("OK");
        ok.getStyleClass().add("cancel-accent");
        ok.setMaxWidth(Double.MAX_VALUE);
        ok.setOnAction(e -> closeCancelFlow());

        modal.getChildren().addAll(header, body, ok);
        showModal(modal);
    }

    private void scrollCardsTop() {
        if (listScroll != null) {
            listScroll.setVvalue(0);
        }
    }

    private void showModal(Node content) {
        if (cancelOverlay == null || cancelModalHolder == null)
            return;
        if (mainScroll != null) {
            mainScroll.setDisable(true);
            mainScroll.setEffect(new GaussianBlur(8));
        }
        if (reloadButton != null)
            reloadButton.setDisable(true);
        cancelModalHolder.getChildren().setAll(content);
        cancelOverlay.setVisible(true);
        cancelOverlay.setPickOnBounds(true);
        if (content instanceof Region region) {
            region.setMaxWidth(460);
            region.setPrefWidth(420);
            region.setMaxHeight(340);
        }
        content.setOpacity(0);
        content.setTranslateY(8);
        Timeline tl = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(content.opacityProperty(), 0),
                        new KeyValue(content.scaleXProperty(), 0.92),
                        new KeyValue(content.scaleYProperty(), 0.92),
                        new KeyValue(content.translateYProperty(), 12)),
                new KeyFrame(Duration.millis(220),
                        new KeyValue(content.opacityProperty(), 1, Interpolator.EASE_OUT),
                        new KeyValue(content.scaleXProperty(), 1, Interpolator.EASE_OUT),
                        new KeyValue(content.scaleYProperty(), 1, Interpolator.EASE_OUT),
                        new KeyValue(content.translateYProperty(), 0, Interpolator.EASE_OUT)));
        tl.play();
    }

    private void closeCancelFlow() {
        if (cancelOverlay != null)
            cancelOverlay.setVisible(false);
        if (cancelModalHolder != null)
            cancelModalHolder.getChildren().clear();
        if (mainScroll != null) {
            mainScroll.setDisable(false);
            mainScroll.setEffect(null);
        }
        if (reloadButton != null)
            reloadButton.setDisable(false);
    }

    private String fmt(LocalDate d) {
        if (d == null)
            return "—";
        try {
            return d.format(df);
        } catch (Exception e) {
            return d.toString();
        }
    }

    private String n(String s) {
        return (s == null || s.isBlank()) ? "—" : s;
    }

    private String stayDays(LocalDate in, LocalDate out) {
        if (in == null || out == null || !out.isAfter(in))
            return "—";
        long days = java.time.temporal.ChronoUnit.DAYS.between(in, out);
        return days + " day" + (days == 1 ? "" : "s");
    }
}
