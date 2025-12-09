package com.travel.loginregistration.service;

import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import java.awt.Color;
import com.travel.loginregistration.model.Booking;
import com.travel.loginregistration.model.Destination;
import com.travel.loginregistration.model.Hotel;
import com.travel.loginregistration.model.HotelRoom;
import com.travel.loginregistration.model.HotelRoomBooking;
import com.travel.loginregistration.model.TravelPackage;
import com.travel.loginregistration.model.User;
import com.travel.loginregistration.model.UserProfile;
import com.travel.loginregistration.repository.BookingRepository;
import com.travel.loginregistration.repository.DestinationRepository;
import com.travel.loginregistration.repository.HotelRepository;
import com.travel.loginregistration.repository.HotelRoomBookingRepository;
import com.travel.loginregistration.repository.HotelRoomRepository;
import com.travel.loginregistration.repository.TravelPackageRepository;
import com.travel.loginregistration.repository.UserProfileRepository;
import com.travel.loginregistration.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Service
public class InvoiceService {
    private final BookingRepository bookingRepository;
    private final HotelRoomBookingRepository roomBookingRepository;
    private final HotelRoomRepository hotelRoomRepository;
    private final HotelRepository hotelRepository;
    private final DestinationRepository destinationRepository;
    private final TravelPackageRepository travelPackageRepository;
    private final UserRepository userRepository;
    private final UserProfileRepository profileRepository;

    private static final DateTimeFormatter DATE_TIME_FORMAT =
            DateTimeFormatter.ofPattern("MMMM d, yyyy 'at' hh:mm a", Locale.ENGLISH)
                    .withZone(ZoneId.systemDefault());
    private static final DateTimeFormatter DATE_ONLY_FORMAT =
            DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.ENGLISH);

    public InvoiceService(BookingRepository bookingRepository,
                          HotelRoomBookingRepository roomBookingRepository,
                          TravelPackageRepository travelPackageRepository,
                          UserRepository userRepository,
                          UserProfileRepository profileRepository,
                          HotelRoomRepository hotelRoomRepository,
                          HotelRepository hotelRepository,
                          DestinationRepository destinationRepository) {
        this.bookingRepository = bookingRepository;
        this.roomBookingRepository = roomBookingRepository;
        this.hotelRoomRepository = hotelRoomRepository;
        this.hotelRepository = hotelRepository;
        this.destinationRepository = destinationRepository;
        this.travelPackageRepository = travelPackageRepository;
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
    }

    @Transactional(readOnly = true)
    public byte[] createInvoice(BookingKind kind, UUID bookingId, String requesterEmail) {
        String normalized = requesterEmail == null ? "" : requesterEmail.toLowerCase(Locale.ROOT);
        User user = userRepository.findByEmail(normalized)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        UserProfile profile = profileRepository.findByUserId(user.getId()).orElse(null);
        String customerName = profile != null && profile.getFullName() != null && !profile.getFullName().isBlank()
                ? profile.getFullName()
                : user.getUsername();
        if (customerName == null || customerName.isBlank()) {
            customerName = user.getEmail();
        }
        String customerAddress = user.getLocation() == null ? "—" : user.getLocation();

        switch (kind) {
            case PACKAGE:
                Booking booking = bookingRepository.findById(bookingId)
                        .orElseThrow(() -> new IllegalArgumentException("Package booking not found"));
                ensureOwner(booking.getUserId(), booking.getUserEmail(), user);
                TravelPackage travelPackage = booking.getPackageId() == null
                        ? null
                        : travelPackageRepository.findById(booking.getPackageId()).orElse(null);
                return renderPackageInvoice(booking, travelPackage, customerName, customerAddress, user.getEmail());
            case ROOM:
                HotelRoomBooking roomBooking = roomBookingRepository.findById(bookingId)
                        .orElseThrow(() -> new IllegalArgumentException("Room booking not found"));
                ensureOwner(roomBooking.getUserId(), roomBooking.getUserEmail(), user);
                return renderRoomInvoice(roomBooking, customerName, customerAddress, user.getEmail());
            default:
                throw new IllegalArgumentException("Unsupported booking kind");
        }
    }

    private void ensureOwner(UUID userId, String userEmail, User requester) {
        boolean match = false;
        if (userId != null && requester.getId() != null) {
            match = requester.getId().equals(userId);
        }
        if (!match && userEmail != null && requester.getEmail() != null) {
            match = userEmail.equalsIgnoreCase(requester.getEmail());
        }
        if (!match) {
            throw new IllegalArgumentException("Not authorized to view this invoice");
        }
    }

    private byte[] renderPackageInvoice(Booking booking,
                                        TravelPackage travelPackage,
                                        String customerName,
                                        String customerAddress,
                                        String customerEmail) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4);
            PdfWriter writer = PdfWriter.getInstance(doc, baos);
            doc.open();
            String docNumber = buildDocumentNumber("PKG", booking.getId());
            addHero(doc, writer, "Invoice", docNumber);
            doc.add(Chunk.NEWLINE);
            addCustomerBlock(doc, customerName, customerEmail, customerAddress);
            doc.add(Chunk.NEWLINE);
            LocalDate start = computePackageStart(travelPackage);
            LocalDate end = computePackageEnd(start, travelPackage);
            addSectionTitle(doc, "Booking details");
            PdfPTable bookingDetails = createTable();
            addKeyValue(bookingDetails, "Package", travelPackage == null ? "—" : travelPackage.getName());
            addKeyValue(bookingDetails, "Location", travelPackage == null ? "—" : travelPackage.getLocation());
            addKeyValue(bookingDetails, "Total persons", booking.getTotalPersons() + " person(s)");
            addKeyValue(bookingDetails, "Start date", formatDate(start));
            addKeyValue(bookingDetails, "End date", formatDate(end));
            addKeyValue(bookingDetails, "Booked on", formatDateTime(booking.getCreatedAt()));
            doc.add(bookingDetails);
            doc.add(Chunk.NEWLINE);
            addPaymentSummary(doc, booking.getTransactionId(), booking.getCardLast4(), booking.getCreatedAt(),
                    booking.getPriceTotal(), docNumber);
            doc.add(Chunk.NEWLINE);
            addThankYouBlock(doc);
            doc.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to render invoice", e);
        }
    }

    private byte[] renderRoomInvoice(HotelRoomBooking booking,
                                     String customerName,
                                     String customerAddress,
                                     String customerEmail) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4);
            PdfWriter writer = PdfWriter.getInstance(doc, baos);
            doc.open();
            String docNumber = buildDocumentNumber("ROOM", booking.getId());
            addHero(doc, writer, "Invoice", docNumber);
            doc.add(Chunk.NEWLINE);
            addCustomerBlock(doc, customerName, customerEmail, customerAddress);
            doc.add(Chunk.NEWLINE);
            LocationDetails location = resolveLocation(booking.getRoomId(), booking.getHotelName());
            addSectionTitle(doc, "Booking details");
            PdfPTable bookingDetails = createTable();
            addKeyValue(bookingDetails, "Destination", location.destination());
            addKeyValue(bookingDetails, "Hotel", location.hotel());
            addKeyValue(bookingDetails, "Room", booking.getRoomName() == null ? "—" : booking.getRoomName());
            addKeyValue(bookingDetails, "Total rooms", booking.getRoomsBooked() == null
                    ? "—"
                    : booking.getRoomsBooked() + " room(s)");
            addKeyValue(bookingDetails, "Check-in", formatDate(booking.getCheckIn()));
            addKeyValue(bookingDetails, "Check-out", formatDate(booking.getCheckOut()));
            addKeyValue(bookingDetails, "Booked on", formatDateTime(booking.getCreatedAt()));
            doc.add(bookingDetails);
            doc.add(Chunk.NEWLINE);
            addPaymentSummary(doc, booking.getTransactionId(), booking.getCardLast4(), booking.getCreatedAt(),
                    booking.getTotalPrice(), docNumber);
            doc.add(Chunk.NEWLINE);
            addThankYouBlock(doc);
            doc.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to render invoice", e);
        }
    }

    private void addHero(Document doc, PdfWriter writer, String label, String docNumber) throws DocumentException {
        PdfPTable hero = new PdfPTable(2);
        hero.setWidthPercentage(100);
        hero.setSpacingBefore(4);
        hero.setSpacingAfter(14);
        hero.getDefaultCell().setBorder(Rectangle.NO_BORDER);

        PdfPCell left = new PdfPCell();
        left.setPadding(0);
        left.setBorder(Rectangle.NO_BORDER);
        left.setBackgroundColor(Color.WHITE);
        Paragraph name = new Paragraph("Travel-Tourism", FontFactory.getFont(FontFactory.HELVETICA, 26, Font.BOLD, new Color(86, 0, 255)));
        Paragraph tagline = new Paragraph("Your Journey, Our Passion", FontFactory.getFont(FontFactory.HELVETICA, 12, Font.NORMAL, Color.GRAY));
        tagline.setSpacingBefore(4);
        left.addElement(name);
        left.addElement(tagline);

        PdfPCell right = new PdfPCell();
        right.setBorder(Rectangle.NO_BORDER);
        right.setBackgroundColor(Color.WHITE);
        PdfPTable rightTable = new PdfPTable(1);
        rightTable.setWidthPercentage(100);

        PdfPCell badge = new PdfPCell(new Phrase(label.toUpperCase(), FontFactory.getFont(FontFactory.HELVETICA, 14, Font.BOLD, Color.WHITE)));
        badge.setPadding(10);
        badge.setHorizontalAlignment(Element.ALIGN_CENTER);
        badge.setBackgroundColor(new Color(112, 70, 241));
        badge.setBorder(Rectangle.NO_BORDER);
        rightTable.addCell(badge);

        PdfPCell numberCell = new PdfPCell(new Phrase(docNumber,
                FontFactory.getFont(FontFactory.HELVETICA, 12, Font.NORMAL, Color.DARK_GRAY)));
        numberCell.setBorder(Rectangle.NO_BORDER);
        numberCell.setPaddingTop(8);
        numberCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        rightTable.addCell(numberCell);

        right.addElement(rightTable);

        hero.addCell(left);
        hero.addCell(right);
        doc.add(hero);
    }

    private void addCustomerBlock(Document doc, String name, String email, String address) throws DocumentException {
        addSectionTitle(doc, "Customer");
        PdfPTable customer = createTable();
        addKeyValue(customer, "Name", name, true);
        addKeyValue(customer, "Email", email, true);
        addKeyValue(customer, "Address", address, true);
        doc.add(customer);
    }

    private void addPaymentSummary(Document doc,
                                   String transactionId,
                                   String cardLast4,
                                   Instant bookedAt,
                                   BigDecimal total,
                                   String docNumber) throws DocumentException {
        addSectionTitle(doc, "Payment");
        PdfPTable payment = createTable();
        addKeyValue(payment, "Transaction ID", resolveTxn(transactionId, docNumber), true);
        addKeyValue(payment, "Method", "Card", true);
        addKeyValue(payment, "Paid on", formatDateTime(bookedAt), true);
        addKeyValue(payment, "Total", formatMoney(total), true);
        doc.add(payment);
    }

    private void addSectionTitle(Document doc, String text) throws DocumentException {
        Paragraph title = new Paragraph(text, FontFactory.getFont(FontFactory.HELVETICA, 16, Font.BOLD, new Color(82, 82, 91)));
        title.setSpacingAfter(6);
        doc.add(title);
    }

    private PdfPTable createTable() {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.getDefaultCell().setBorder(Rectangle.NO_BORDER);
        table.setSpacingAfter(8);
        table.setWidths(new float[]{1f, 2f});
        return table;
    }

    private void addKeyValue(PdfPTable table, String label, String value) {
        addKeyValue(table, label, value, false);
    }

    private void addKeyValue(PdfPTable table, String label, String value, boolean highlight) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, FontFactory.getFont(FontFactory.HELVETICA, 11, Font.NORMAL, Color.GRAY)));
        labelCell.setBorder(Rectangle.NO_BORDER);
        labelCell.setPadding(4);
        PdfPCell valueCell = new PdfPCell(new Phrase(value, FontFactory.getFont(FontFactory.HELVETICA, 12, Font.NORMAL, Color.DARK_GRAY)));
        valueCell.setBorder(Rectangle.NO_BORDER);
        valueCell.setPadding(4);
        if (highlight) {
            Color tint = new Color(245, 247, 249);
            labelCell.setBackgroundColor(tint);
            valueCell.setBackgroundColor(tint);
        }
        table.addCell(labelCell);
        table.addCell(valueCell);
    }

    private void addThankYouBlock(Document doc) throws DocumentException {
        PdfPTable thankYou = new PdfPTable(1);
        thankYou.setWidthPercentage(100);
        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(new Color(237, 245, 255));
        cell.setPadding(12);
        Paragraph main = new Paragraph("Thank you for booking with us!", FontFactory.getFont(FontFactory.HELVETICA, 16, Font.BOLD, new Color(21, 128, 61)));
        main.setAlignment(Element.ALIGN_CENTER);
        Paragraph sub = new Paragraph("We're excited to be part of your journey. support@travel-tourism.com",
                FontFactory.getFont(FontFactory.HELVETICA, 12, Font.NORMAL, Color.GRAY));
        sub.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(main);
        cell.addElement(sub);
        thankYou.addCell(cell);
        doc.add(thankYou);
        Paragraph generated = new Paragraph("Generated on " + formatDateTime(Instant.now()), FontFactory.getFont(FontFactory.HELVETICA, 10, Font.NORMAL, Color.GRAY));
        generated.setAlignment(Element.ALIGN_CENTER);
        generated.setSpacingBefore(8);
        doc.add(generated);
    }

    private String formatDate(LocalDate date) {
        return date == null ? "—" : date.format(DATE_ONLY_FORMAT);
    }

    private String formatDateTime(Instant instant) {
        return instant == null ? "—" : DATE_TIME_FORMAT.format(instant);
    }

    private String formatMoney(BigDecimal value) {
        if (value == null) {
            return "BDT —";
        }
        return "BDT " + value.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private String buildDocumentNumber(String prefix, UUID id) {
        String shortId = id == null ? "000000" : id.toString().replace("-", "").substring(0, Math.min(8, id.toString().length()));
        return prefix + "-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "-" + shortId.toUpperCase(Locale.ENGLISH);
    }

    private String resolveTxn(String txn, String fallback) {
        if (txn == null || txn.isBlank()) {
            return fallback;
        }
        return txn;
    }

    private LocationDetails resolveLocation(UUID roomId, String fallbackHotel) {
        String hotelName = fallbackHotel == null || fallbackHotel.isBlank() ? "—" : fallbackHotel;
        String destinationName = "—";
        if (roomId == null) {
            return new LocationDetails(hotelName, destinationName);
        }
        Optional<HotelRoom> roomOpt = hotelRoomRepository.findById(roomId);
        if (roomOpt.isEmpty()) {
            return new LocationDetails(hotelName, destinationName);
        }
        HotelRoom room = roomOpt.get();
        if (room.getHotelId() == null) {
            return new LocationDetails(hotelName, destinationName);
        }
        Optional<Hotel> hotelOpt = hotelRepository.findById(room.getHotelId());
        if (hotelOpt.isEmpty()) {
            return new LocationDetails(hotelName, destinationName);
        }
        Hotel hotel = hotelOpt.get();
        if (hotel.getName() != null && !hotel.getName().isBlank()) {
            hotelName = hotel.getName();
        }
        if (hotel.getDestinationId() != null) {
            Optional<Destination> destOpt = destinationRepository.findById(hotel.getDestinationId());
            if (destOpt.isPresent()) {
                Destination dest = destOpt.get();
                if (dest.getName() != null && !dest.getName().isBlank()) {
                    destinationName = dest.getName();
                }
            }
        }
        return new LocationDetails(hotelName, destinationName);
    }

    private record LocationDetails(String hotel, String destination) { }

    private LocalDate computePackageStart(TravelPackage travelPackage) {
        if (travelPackage == null || travelPackage.getBookingDeadline() == null) {
            return null;
        }
        return travelPackage.getBookingDeadline().plusDays(2);
    }

    private LocalDate computePackageEnd(LocalDate start, TravelPackage travelPackage) {
        if (start == null || travelPackage == null || travelPackage.getTiming() == null) {
            return start;
        }
        Integer days = parseDurationDays(travelPackage.getTiming());
        if (days == null || days <= 0) {
            return start;
        }
        return start.plusDays(days);
    }

    private Integer parseDurationDays(String timing) {
        if (timing == null) {
            return null;
        }
        String lower = timing.toLowerCase(Locale.ENGLISH);
        int idx = lower.indexOf("day");
        if (idx == -1) {
            return null;
        }
        String prefix = lower.substring(0, idx).replaceAll("[^0-9]", "");
        if (prefix.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(prefix);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public enum BookingKind {
        PACKAGE,
        ROOM;

        public static BookingKind from(String value) {
            try {
                return BookingKind.valueOf(value.trim().toUpperCase(Locale.ENGLISH));
            } catch (Exception e) {
                throw new IllegalArgumentException("Invalid booking kind");
            }
        }
    }
}
