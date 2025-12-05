package com.travel.frontend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ArrayList;
import java.util.Locale;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;


/**
 * History page: renders booking cards and a slide-in detail panel using real data
 * fetched from the backend /history endpoint.
 */
public class HistoryController {

    @FXML private VBox listContainer;
    @FXML private StackPane detailHolder;
    @FXML private NavbarController navbarController;
    @FXML private ToggleButton upcomingToggle;
    @FXML private ToggleButton pastToggle;
    @FXML private ToggleButton canceledToggle;
    @FXML private TextField searchField;

    private static final double DETAIL_WIDTH = 460;
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

    public static void markDirty() { dirty = true; }

    @FXML private void reloadHistory() { fetchHistory(true); }

    private void setupFilters() {
        if (upcomingToggle != null && pastToggle != null && canceledToggle != null) {
            ToggleGroup tg = new ToggleGroup();
            upcomingToggle.setToggleGroup(tg);
            pastToggle.setToggleGroup(tg);
            canceledToggle.setToggleGroup(tg);
            upcomingToggle.setSelected(true);
            upcomingToggle.setOnAction(e -> renderFiltered());
            pastToggle.setOnAction(e -> renderFiltered());
            canceledToggle.setOnAction(e -> renderFiltered());
        }
        if (searchField != null) {
            searchField.textProperty().addListener((obs, o, n) -> renderFiltered());
        }
    }

    private void loadHistory() {
        fetchHistory(false);
    }

    private void fetchHistory(boolean force) {
        detailHolder.setVisible(false);
        detailHolder.getChildren().clear();
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
                if (res.statusCode() != 200) throw new RuntimeException("HTTP " + res.statusCode());
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
                if (!matchesSearch(r)) continue;
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
                if (!matchesSearch(p)) continue;
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
                case "upcoming" -> { listContainer.getChildren().addAll(upcoming); any |= !upcoming.isEmpty(); }
                case "past" -> { listContainer.getChildren().addAll(past); any |= !past.isEmpty(); }
                case "canceled" -> { listContainer.getChildren().addAll(canceled); any |= !canceled.isEmpty(); }
            }
        }
        if (!any) listContainer.getChildren().add(emptyCard());
    }

    private boolean shouldShow(String status) {
        if (upcomingToggle != null && pastToggle != null && canceledToggle != null) {
            if (upcomingToggle.isSelected()) return "Upcoming".equalsIgnoreCase(status);
            if (pastToggle.isSelected()) return "Past".equalsIgnoreCase(status);
            if (canceledToggle.isSelected()) return "Canceled".equalsIgnoreCase(status);
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
                        new KeyValue(listContainer.translateXProperty(), 0)
                ),
                new KeyFrame(Duration.millis(120),
                        new KeyValue(listContainer.scaleXProperty(), 0.97, Interpolator.EASE_BOTH),
                        new KeyValue(listContainer.translateXProperty(), -8, Interpolator.EASE_BOTH)
                ),
                new KeyFrame(Duration.millis(320),
                        new KeyValue(detailHolder.opacityProperty(), 1, Interpolator.EASE_BOTH),
                        new KeyValue(detailHolder.translateXProperty(), 0, Interpolator.EASE_OUT),
                        new KeyValue(detailHolder.prefWidthProperty(), DETAIL_WIDTH, Interpolator.EASE_BOTH),
                        new KeyValue(listContainer.scaleXProperty(), 0.96, Interpolator.EASE_BOTH),
                        new KeyValue(listContainer.translateXProperty(), -16, Interpolator.EASE_BOTH)
                )
        );
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
        HBox headRow = new HBox(8, header, spacer(), pill(pillText, "pillStatus"));
        header.getChildren().addAll(title, subtitle);
        headRow.setAlignment(Pos.CENTER_LEFT);
        root.getChildren().add(headRow);

        root.getChildren().add(section("Stay overview",
                kv("Check-in", fmt(r.checkIn)),
                kv("Check-out", fmt(r.checkOut)),
                kv("Room type", n(r.roomName)),
                kv("Guests / Rooms", (r.totalGuests == null ? "—" : r.totalGuests + " guests") +
                        " · " + (r.roomsBooked == null ? "— rooms" : r.roomsBooked + " room(s)"))
        ));

        root.getChildren().add(section("Rooms & pricing",
                kv("Room", n(r.roomName)),
                kv("Total stay", stayDays(r.checkIn, r.checkOut)),
                kv("Total paid", r.totalPrice == null ? "BDT —" : "BDT " + r.totalPrice)
        ));

        root.getChildren().add(section("Payment",
                kv("Status", "Paid"),
                kv("Method", "Card"),
                kv("Paid on", fmt(r.createdAt == null ? null : r.createdAt.atZone(java.time.ZoneId.systemDefault()).toLocalDate()))
        ));

        root.getChildren().add(section("Cancellation policy",
                paragraph("This hotel booking can be canceled free of charge up to 24 hours before check-in.")
        ));

        root.getChildren().add(timeline(
                kv("Booked", fmt(r.createdAt == null ? null : r.createdAt.atZone(java.time.ZoneId.systemDefault()).toLocalDate())),
                kv("Check-in", fmt(r.checkIn)),
                kv("Check-out", fmt(r.checkOut))
        ));

        root.getChildren().add(section("Documents",
                pillButton("Download invoice (PDF)"),
                pillButton("Download receipt")
        ));

        root.getChildren().add(section("Need help?",
                pillButton("Contact support about this booking")
        ));

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
        pillsBox.getChildren().addAll(pill("Package", "pillType"), pill(pillText, "pillStatus"));

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
                kv("Guests", p.totalPersons == null ? "—" : p.totalPersons + " persons")
        ));

        root.getChildren().add(section("Package & pricing",
                kv("Package", n(p.packageName)),
                kv("Location", n(p.location)),
                kv("Total paid", p.totalPrice == null ? "BDT —" : "BDT " + p.totalPrice),
                kv("Breakdown", p.totalPrice == null || p.totalPersons == null
                        ? "—"
                        : "BDT " + p.totalPrice.divide(java.math.BigDecimal.valueOf(Math.max(1, p.totalPersons))) + " × " + p.totalPersons + " guests")
        ));

        root.getChildren().add(section("Payment",
                kv("Status", "Paid"),
                kv("Method", "Card"),
                kv("Paid on", fmt(p.createdAt == null ? null : p.createdAt.atZone(java.time.ZoneId.systemDefault()).toLocalDate()))
        ));

        root.getChildren().add(section("Cancellation policy",
                paragraph("This package can be canceled free of charge within 24 hours after it is booked. After that, the booking becomes non-refundable.")
        ));

        root.getChildren().add(timeline(
                kv("Booked on", fmt(p.createdAt == null ? null : p.createdAt.atZone(java.time.ZoneId.systemDefault()).toLocalDate())),
                kv("Start date", fmt(start)),
                kv("End date", fmt(end))
        ));

        root.getChildren().add(section("Documents",
                pillButton("Invoice (PDF)"),
                pillButton("Receipt (PDF)")
        ));

        root.getChildren().add(section("Help",
                pillButton("Contact support")
        ));

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
            Label bookedOn = new Label("Booked on " + fmt(r.createdAt.atZone(java.time.ZoneId.systemDefault()).toLocalDate()));
            bookedOn.getStyleClass().add("cardNote");
            info.getChildren().add(bookedOn);
        }

        VBox right = new VBox(6);
        right.setAlignment(Pos.CENTER_RIGHT);
        right.getChildren().add(pill(status, "statusPill"));
        Label price = new Label(r.totalPrice == null ? "BDT —" : "BDT " + r.totalPrice);
        price.getStyleClass().add("cardPrice");
        right.getChildren().add(price);
        right.getChildren().add(labelMeta("ID · " + (r.id == null ? "-" : r.id.toString())));

        top.getChildren().addAll(info, spacer(), right);
        card.getChildren().add(top);

        HBox actions = new HBox(10);
        actions.setAlignment(Pos.CENTER_RIGHT);
        Button view = new Button("View details");
        view.getStyleClass().add("ghostButton");
        view.setOnAction(e -> setDetail(buildHotelDetail(r, status)));
        Button cancel = new Button("Cancel booking");
        cancel.getStyleClass().add("accentButton");
        if ("Past".equalsIgnoreCase(status)) {
            actions.getChildren().add(view);
        } else {
            actions.getChildren().addAll(view, cancel);
        }
        card.getChildren().add(actions);
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
        info.getChildren().add(labelNote("Package booked on " + fmt(p.createdAt == null ? null : p.createdAt.atZone(java.time.ZoneId.systemDefault()).toLocalDate())));
        info.setPrefWidth(0);
        HBox.setHgrow(info, javafx.scene.layout.Priority.ALWAYS);
        info.setFillWidth(true);

        VBox right = new VBox(6);
        right.setAlignment(Pos.TOP_RIGHT);
        // Stack pills to free horizontal space
        right.setMinWidth(140);
        right.setMaxWidth(200);
        right.setFillWidth(false);
        VBox pills = new VBox(4, pill("Package", "packagePill"), pill(status, "statusPill"));
        pills.setAlignment(Pos.TOP_RIGHT);
        right.getChildren().add(pills);
        Label price = new Label(p.totalPrice == null ? "BDT —" : "BDT " + p.totalPrice);
        price.getStyleClass().add("cardPrice");
        right.getChildren().add(price);
        right.getChildren().add(labelMeta("ID · " + (p.id == null ? "-" : p.id.toString())));

        top.getChildren().addAll(info, spacer(), right);
        card.getChildren().add(top);

        HBox actions = new HBox(10);
        actions.setAlignment(Pos.CENTER_RIGHT);
        Button view = new Button("View details");
        view.getStyleClass().add("ghostButton");
        view.setOnAction(e -> setDetail(buildPackageDetail(p, status)));
        Button cancel = new Button("Cancel booking");
        cancel.getStyleClass().add("accentButton");
        if ("Past".equalsIgnoreCase(status)) {
            actions.getChildren().add(view);
        } else {
            actions.getChildren().addAll(view, cancel);
        }
        card.getChildren().add(actions);
        return card;
    }

    private String roomStatus(HistoryRoomItem r, LocalDate today) {
        // Consider past if check-in is today or earlier (midnight cutoff), or check-out is today/past
        if (r.checkIn != null && !r.checkIn.isAfter(today)) return "Past";
        if (r.checkOut != null && !r.checkOut.isAfter(today)) return "Past";
        return "Upcoming";
    }

    private String packageStatus(HistoryPackageItem p, LocalDate today) {
        LocalDate start = p.bookingDeadline != null ? p.bookingDeadline.plusDays(2) : null;
        if (start != null && !start.isAfter(today)) return "Past";
        return "Upcoming";
    }

    private boolean matchesSearch(HistoryRoomItem r) {
        String q = searchQuery();
        if (q.isEmpty()) return true;
        return (r.hotelName != null && r.hotelName.toLowerCase(Locale.ROOT).contains(q))
                || (r.roomName != null && r.roomName.toLowerCase(Locale.ROOT).contains(q))
                || (r.id != null && r.id.toString().toLowerCase(Locale.ROOT).contains(q));
    }

    private boolean matchesSearch(HistoryPackageItem p) {
        String q = searchQuery();
        if (q.isEmpty()) return true;
        return (p.packageName != null && p.packageName.toLowerCase(Locale.ROOT).contains(q))
                || (p.location != null && p.location.toLowerCase(Locale.ROOT).contains(q))
                || (p.id != null && p.id.toString().toLowerCase(Locale.ROOT).contains(q));
    }

    private String searchQuery() {
        if (searchField == null || searchField.getText() == null) return "";
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
        Label b = new Label("Hotel bookings can be canceled up to 24 hours before check-in. Package bookings can be canceled within 24 hours after booking.");
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
        Label t = new Label(title);
        t.getStyleClass().add("sectionHeader");
        box.getChildren().add(t);
        VBox inner = new VBox(4);
        for (Node r : rows) inner.getChildren().add(r);
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

    private Label paragraph(String text) {
        Label p = new Label(text);
        p.setWrapText(true);
        p.getStyleClass().add("paragraph");
        return p;
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

    private String fmt(LocalDate d) {
        if (d == null) return "—";
        try { return d.format(df); } catch (Exception e) { return d.toString(); }
    }

    private String n(String s) {
        return (s == null || s.isBlank()) ? "—" : s;
    }

    private String stayDays(LocalDate in, LocalDate out) {
        if (in == null || out == null || !out.isAfter(in)) return "—";
        long days = java.time.temporal.ChronoUnit.DAYS.between(in, out);
        return days + " day" + (days == 1 ? "" : "s");
    }
}
