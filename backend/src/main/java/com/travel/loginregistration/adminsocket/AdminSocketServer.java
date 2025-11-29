package com.travel.loginregistration.adminsocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.travel.loginregistration.model.AdminUser;
import com.travel.loginregistration.model.TravelPackage;
import com.travel.loginregistration.model.PackageItinerary;
import com.travel.loginregistration.model.Destination;
import com.travel.loginregistration.model.Hotel;
import com.travel.loginregistration.model.HotelRoom;
import com.travel.loginregistration.repository.AdminUserRepository;
import com.travel.loginregistration.repository.TravelPackageRepository;
import com.travel.loginregistration.repository.PackageItineraryRepository;
import com.travel.loginregistration.repository.DestinationRepository;
import com.travel.loginregistration.repository.HotelRepository;
import com.travel.loginregistration.repository.HotelRoomRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.stereotype.Component;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.io.*;
import java.math.BigDecimal;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/*
 * A simple socket server for admin operations on travel packages.
 * This listens on a TCP port (default 9090) for JSON requests from the AdminSocketClient, allowing admins to authenticate and perform
 * the admin operations of listing, creating, updating, and deleting travel packages (CRUD).
 */

@Component
public class AdminSocketServer {
    private final AdminUserRepository adminRepo;
    private final TravelPackageRepository pkgRepo;
    private final PackageItineraryRepository itineraryRepo;
    private final DestinationRepository destinationRepo;
    private final HotelRepository hotelRepo;
    private final HotelRoomRepository roomRepo;
    private final BCryptPasswordEncoder encoder;
    private final TransactionTemplate txTemplate;
    private final ObjectMapper mapper = new ObjectMapper();
    private final Map<String, UUID> sessions = new ConcurrentHashMap<>();

    public AdminSocketServer(AdminUserRepository adminRepo, TravelPackageRepository pkgRepo,
                             PackageItineraryRepository itineraryRepo, DestinationRepository destinationRepo,
                             HotelRepository hotelRepo, HotelRoomRepository roomRepo,
                             BCryptPasswordEncoder encoder,
                             PlatformTransactionManager txManager) {
        this.adminRepo = adminRepo;
        this.pkgRepo = pkgRepo;
        this.itineraryRepo = itineraryRepo;
        this.destinationRepo = destinationRepo;
        this.hotelRepo = hotelRepo;
        this.roomRepo = roomRepo;
        this.encoder = encoder;
        this.txTemplate = new TransactionTemplate(txManager);
    }

    // Starts the socket server thread as soon as Spring finishes wiring this bean.
    @PostConstruct
    public void start() {
        Thread t = new Thread(this::listen, "admin-socket-server");
        t.setDaemon(true);
        t.start();
    }

    // Binds ServerSocket to port 9090 and spawns a worker thread per incoming connection.
    private void listen() {
        int port = 9090;
        try (ServerSocket ss = new ServerSocket(port)) {
            System.out.println("[AdminSocket] Listening on port " + port);
            while (true) {
                Socket s = ss.accept();
                new Thread(() -> handle(s)).start();
            }
        } catch (IOException e) {
            System.err.println("[AdminSocket] Failed to bind: " + e.getMessage());
        }
    }

    // Parses a request line, routes by type, and writes the JSON response.
    private void handle(Socket s) {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(s.getInputStream()));
             BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(s.getOutputStream()))) {
            String line = br.readLine();
            if (line == null) return;
            Map<String, Object> req = mapper.readValue(line, Map.class);
            String type = String.valueOf(req.getOrDefault("type", ""));
            Map<String, Object> res;
            switch (type) {
                case "AUTH" -> res = doAuth(req);
                case "LIST" -> {
                    if (!authorized(req)) { res = err("UNAUTHORIZED"); break; }
                    res = listPackages();
                }
                case "DEST_LIST" -> {
                    if (!authorized(req)) { res = err("UNAUTHORIZED"); break; }
                    res = listDestinations();
                }
                case "CREATE" -> {
                    if (!authorized(req)) { res = err("UNAUTHORIZED"); break; }
                    res = txTemplate.execute(status -> createPackage(req));
                }
                case "DEST_CREATE" -> {
                    if (!authorized(req)) { res = err("UNAUTHORIZED"); break; }
                    res = txTemplate.execute(status -> createDestination(req));
                }
                case "UPDATE" -> {
                    if (!authorized(req)) { res = err("UNAUTHORIZED"); break; }
                    res = txTemplate.execute(status -> updatePackage(req));
                }
                case "DEST_UPDATE" -> {
                    if (!authorized(req)) { res = err("UNAUTHORIZED"); break; }
                    res = txTemplate.execute(status -> updateDestination(req));
                }
                case "DELETE" -> {
                    if (!authorized(req)) { res = err("UNAUTHORIZED"); break; }
                    res = txTemplate.execute(status -> deletePackage(req));
                }
                case "DEST_DELETE" -> {
                    if (!authorized(req)) { res = err("UNAUTHORIZED"); break; }
                    res = txTemplate.execute(status -> deleteDestination(req));
                }
                case "HOTEL_LIST" -> {
                    if (!authorized(req)) { res = err("UNAUTHORIZED"); break; }
                    res = listHotels(req);
                }
                case "HOTEL_CREATE" -> {
                    if (!authorized(req)) { res = err("UNAUTHORIZED"); break; }
                    res = txTemplate.execute(status -> createHotel(req));
                }
                case "HOTEL_UPDATE" -> {
                    if (!authorized(req)) { res = err("UNAUTHORIZED"); break; }
                    res = txTemplate.execute(status -> updateHotel(req));
                }
                case "HOTEL_DELETE" -> {
                    if (!authorized(req)) { res = err("UNAUTHORIZED"); break; }
                    res = txTemplate.execute(status -> deleteHotel(req));
                }
                case "ROOM_LIST" -> {
                    if (!authorized(req)) { res = err("UNAUTHORIZED"); break; }
                    res = listRooms(req);
                }
                case "ROOM_SAVE" -> {
                    if (!authorized(req)) { res = err("UNAUTHORIZED"); break; }
                    res = txTemplate.execute(status -> saveRooms(req));
                }
                default -> res = err("UNKNOWN_TYPE");
            }
            bw.write(mapper.writeValueAsString(res));
            bw.write("\n");
            bw.flush();
        } catch (Exception e) {
            System.err.println("[AdminSocket] Error handling request: " + e.getMessage());
        } finally {
            try { s.close(); } catch (IOException ignore) {}
        }
    }

    // Checks whether the request contains a valid session token.
    private boolean authorized(Map<String, Object> req) {
        String token = (String) req.get("token");
        return token != null && sessions.containsKey(token);
    }

    // Validates admin credentials and issues a session token.
    private Map<String, Object> doAuth(Map<String, Object> req) {
        String email = (String) req.get("email");
        String password = (String) req.get("password");
        if (email == null || password == null) return err("MISSING_CREDENTIALS");
        Optional<AdminUser> ou = adminRepo.findByEmail(email.toLowerCase(Locale.ROOT));
        if (ou.isEmpty()) return err("NO_SUCH_ADMIN");
        AdminUser u = ou.get();
        if (!encoder.matches(password, u.getPasswordHash())) return err("BAD_PASSWORD");
        String token = UUID.randomUUID().toString();
        sessions.put(token, u.getId());
        Map<String, Object> ok = ok();
        ok.put("msg", "AUTH_OK");
        ok.put("token", token);
        return ok;
    }

    // Returns every travel package without filtering (admin view).
    private Map<String, Object> listPackages() {
        List<TravelPackage> items = pkgRepo.findAll();
        for (TravelPackage p : items) {
            boolean match = hasMatchingDestination(p.getLocation());
            p.setPackageAvailable(match);
        }
        List<Map<String, Object>> decorated = new ArrayList<>();
        for (TravelPackage p : items) {
            decorated.add(toPackagePayload(p));
        }
        Map<String, Object> ok = ok();
        ok.put("items", decorated);
        return ok;
    }

    private Map<String, Object> listDestinations() {
        List<Destination> items = destinationRepo.findAll();
        List<Map<String,Object>> decorated = new ArrayList<>();
        for (Destination d : items) {
            Map<String,Object> m = new HashMap<>();
            m.put("id", d.getId());
            m.put("name", d.getName());
            m.put("region", d.getRegion());
            m.put("tags", d.getTags());
            m.put("bestSeason", d.getBestSeason());
            m.put("imageUrl", d.getImageUrl());
            m.put("hotelsCount", hotelRepo.countByDestinationId(d.getId()));
            m.put("active", d.isActive());
            Optional<UUID> pkgId = findMatchingPackageId(d.getName());
            m.put("packageAvailable", pkgId.isPresent());
            pkgId.ifPresent(id -> m.put("packageId", id));
            decorated.add(m);
        }
        Map<String, Object> ok = ok();
        ok.put("items", decorated);
        return ok;
    }

    // Creates a new TravelPackage entity from the payload.
    // A payload is the actual data being sent in a request or response. 
    @Transactional
    private Map<String, Object> createPackage(Map<String, Object> req) {
        Map<String, Object> item = (Map<String, Object>) req.get("item");
        TravelPackage p = new TravelPackage();
        apply(p, item);
        pkgRepo.save(p);
        applyItinerary(p, item);
        
        Map<String, Object> ok = ok();
        ok.put("id", p.getId());
        return ok;
    }

    // Applies the payload to an existing TravelPackage and saves it.
    @Transactional
    private Map<String, Object> updatePackage(Map<String, Object> req) {
        String idStr = (String) req.get("id");
        if (idStr == null) return err("MISSING_ID");
        UUID id = UUID.fromString(idStr);
        TravelPackage p = pkgRepo.findById(id).orElse(null);
        if (p == null) return err("NOT_FOUND");
        Map<String, Object> item = (Map<String, Object>) req.get("item");
        apply(p, item);
        pkgRepo.save(p);
        applyItinerary(p, item);
        return ok();
    }

    // Deletes the package with the provided id (if it exists).
    @Transactional
    private Map<String, Object> deletePackage(Map<String, Object> req) {
        String idStr = (String) req.get("id");
        if (idStr == null) return err("MISSING_ID");
        UUID id = UUID.fromString(idStr);
        if (pkgRepo.existsById(id)) pkgRepo.deleteById(id);
        return ok();
    }

    @Transactional
    private Map<String, Object> createDestination(Map<String, Object> req) {
        Map<String, Object> item = (Map<String, Object>) req.get("item");
        Destination d = new Destination();
        applyDest(d, item);
        destinationRepo.save(d);
        Map<String, Object> ok = ok();
        ok.put("id", d.getId());
        return ok;
    }

    @Transactional
    private Map<String, Object> updateDestination(Map<String, Object> req) {
        String idStr = (String) req.get("id");
        if (idStr == null) return err("MISSING_ID");
        UUID id = UUID.fromString(idStr);
        Destination d = destinationRepo.findById(id).orElse(null);
        if (d == null) return err("NOT_FOUND");
        Map<String, Object> item = (Map<String, Object>) req.get("item");
        applyDest(d, item);
        destinationRepo.save(d);
        return ok();
    }

    @Transactional
    private Map<String, Object> deleteDestination(Map<String, Object> req) {
        String idStr = (String) req.get("id");
        if (idStr == null) return err("MISSING_ID");
        UUID id = UUID.fromString(idStr);
        if (destinationRepo.existsById(id)) destinationRepo.deleteById(id);
        return ok();
    }

    private Map<String, Object> listHotels(Map<String, Object> req) {
        String destStr = (String) req.get("destinationId");
        if (destStr == null) return err("MISSING_DESTINATION");
        UUID destId = UUID.fromString(destStr);
        List<Hotel> hotels = hotelRepo.findByDestinationIdOrderByNameAsc(destId);
        Map<String, Object> ok = ok();
        ok.put("items", hotels);
        return ok;
    }

    private Map<String, Object> createHotel(Map<String, Object> req) {
        Map<String, Object> item = (Map<String, Object>) req.get("item");
        Hotel h = new Hotel();
        applyHotel(h, item);
        hotelRepo.save(h);
        Map<String, Object> ok = ok();
        ok.put("id", h.getId());
        return ok;
    }

    private Map<String, Object> updateHotel(Map<String, Object> req) {
        String idStr = (String) req.get("id");
        if (idStr == null) return err("MISSING_ID");
        UUID id = UUID.fromString(idStr);
        Hotel h = hotelRepo.findById(id).orElse(null);
        if (h == null) return err("NOT_FOUND");
        Map<String, Object> item = (Map<String, Object>) req.get("item");
        applyHotel(h, item);
        hotelRepo.save(h);
        return ok();
    }

    private Map<String, Object> deleteHotel(Map<String, Object> req) {
        String idStr = (String) req.get("id");
        if (idStr == null) return err("MISSING_ID");
        UUID id = UUID.fromString(idStr);
        if (hotelRepo.existsById(id)) hotelRepo.deleteById(id);
        return ok();
    }

    private Map<String, Object> listRooms(Map<String, Object> req) {
        String hotelStr = (String) req.get("hotelId");
        if (hotelStr == null) return err("MISSING_HOTEL");
        UUID hotelId = UUID.fromString(hotelStr);
        List<HotelRoom> rooms = roomRepo.findByHotelIdOrderByNameAsc(hotelId);
        Map<String, Object> ok = ok();
        ok.put("items", rooms);
        return ok;
    }

    // Saves all rooms for a hotel by replacing existing ones with provided list.
    private Map<String, Object> saveRooms(Map<String, Object> req) {
        String hotelStr = (String) req.get("hotelId");
        if (hotelStr == null) return err("MISSING_HOTEL");
        UUID hotelId = UUID.fromString(hotelStr);
        Object rawList = req.get("items");
        if (!(rawList instanceof List<?> list)) return err("NO_ITEMS");
        roomRepo.deleteByHotelId(hotelId);
        List<HotelRoom> toSave = new ArrayList<>();
        for (Object o : list) {
            if (!(o instanceof Map<?,?> any)) continue;
            @SuppressWarnings("unchecked")
            Map<String,Object> m = (Map<String,Object>) any;
            HotelRoom r = new HotelRoom();
            r.setHotelId(hotelId);
            applyRoom(r, m);
            toSave.add(r);
        }
        if (!toSave.isEmpty()) roomRepo.saveAll(toSave);
        return ok();
    }

    // Copies allowed fields from the arbitrary map into the entity.
    private void apply(TravelPackage p, Map<String, Object> item) {
        if (item == null) return;
        if (item.containsKey("name")) p.setName(norm(str(item.get("name"))));
        if (item.containsKey("location")) p.setLocation(norm(str(item.get("location"))));
        if (item.containsKey("basePrice")) p.setBasePrice(toBigDecimal(item.get("basePrice")));
        if (item.containsKey("destImageUrl")) p.setDestImageUrl(str(item.get("destImageUrl")));
        if (item.containsKey("hotelImageUrl")) p.setHotelImageUrl(str(item.get("hotelImageUrl")));
        if (item.containsKey("image1")) p.setImage1(str(item.get("image1")));
        if (item.containsKey("image2")) p.setImage2(str(item.get("image2")));
        if (item.containsKey("image3")) p.setImage3(str(item.get("image3")));
        if (item.containsKey("image4")) p.setImage4(str(item.get("image4")));
        if (item.containsKey("image5")) p.setImage5(str(item.get("image5")));
        // Default destination/hotel images from first/last if not explicitly provided
        if (!item.containsKey("destImageUrl") && p.getDestImageUrl() == null) p.setDestImageUrl(p.getImage1());
        if (!item.containsKey("hotelImageUrl") && p.getHotelImageUrl() == null) p.setHotelImageUrl(p.getImage5());
        if (item.containsKey("destImageUrl")) p.setDestImageUrl(str(item.get("destImageUrl")));
        if (item.containsKey("hotelImageUrl")) p.setHotelImageUrl(str(item.get("hotelImageUrl")));
        if (item.containsKey("overview")) p.setOverview(str(item.get("overview")));
        if (item.containsKey("locationPoints")) p.setLocationPoints(str(item.get("locationPoints")));
        if (item.containsKey("timing")) p.setTiming(str(item.get("timing")));
        if (item.containsKey("groupSize")) p.setGroupSize(str(item.get("groupSize")));
        if (item.containsKey("active")) p.setActive(Boolean.TRUE.equals(item.get("active")) || "true".equalsIgnoreCase(str(item.get("active"))));
        if (item.containsKey("packageAvailable")) p.setPackageAvailable(bool(item.get("packageAvailable")));
    }

    private void applyDest(Destination d, Map<String, Object> item) {
        if (item == null) return;
        if (item.containsKey("name")) d.setName(norm(str(item.get("name"))));
        if (item.containsKey("region")) d.setRegion(norm(str(item.get("region"))));
        if (item.containsKey("tags")) d.setTags(str(item.get("tags")));
        if (item.containsKey("bestSeason")) d.setBestSeason(str(item.get("bestSeason")));
        if (item.containsKey("imageUrl")) d.setImageUrl(str(item.get("imageUrl")));
        if (item.containsKey("active")) d.setActive(bool(item.get("active")));
    }

    private void applyHotel(Hotel h, Map<String, Object> item) {
        if (item == null) return;
        if (item.containsKey("destinationId")) {
            String dest = str(item.get("destinationId"));
            if (dest != null && !dest.isBlank()) h.setDestinationId(UUID.fromString(dest));
        }
        if (item.containsKey("name")) h.setName(norm(str(item.get("name"))));
        if (item.containsKey("rating")) h.setRating(toBigDecimal(item.get("rating")));
        if (item.containsKey("realPrice")) h.setRealPrice(toBigDecimal(item.get("realPrice")));
        if (item.containsKey("currentPrice")) h.setCurrentPrice(toBigDecimal(item.get("currentPrice")));
        if (item.containsKey("location")) h.setLocation(str(item.get("location")));
        if (item.containsKey("nearby")) h.setNearby(str(item.get("nearby")));
        if (item.containsKey("facilities")) h.setFacilities(str(item.get("facilities")));
        if (item.containsKey("description")) h.setDescription(str(item.get("description")));
        if (item.containsKey("roomsCount")) h.setRoomsCount(intVal(item.get("roomsCount")));
        if (item.containsKey("floorsCount")) h.setFloorsCount(intVal(item.get("floorsCount")));
        if (item.containsKey("image1")) h.setImage1(str(item.get("image1")));
        if (item.containsKey("image2")) h.setImage2(str(item.get("image2")));
        if (item.containsKey("image3")) h.setImage3(str(item.get("image3")));
        if (item.containsKey("image4")) h.setImage4(str(item.get("image4")));
        if (item.containsKey("image5")) h.setImage5(str(item.get("image5")));
        if (item.containsKey("gallery")) h.setGallery(str(item.get("gallery")));
    }

    private void applyRoom(HotelRoom r, Map<String, Object> item) {
        if (item == null) return;
        if (item.containsKey("id")) {
            String idStr = str(item.get("id"));
            if (idStr != null && !idStr.isBlank()) {
                try { r.setId(UUID.fromString(idStr)); } catch (Exception ignore) {}
            }
        }
        if (item.containsKey("name")) r.setName(str(item.get("name")));
        if (item.containsKey("price")) r.setPrice(toBigDecimal(item.get("price"))); // legacy support
        if (item.containsKey("currentPrice")) r.setCurrentPrice(toBigDecimal(item.get("currentPrice")));
        if (item.containsKey("realPrice")) r.setRealPrice(toBigDecimal(item.get("realPrice")));
        if (item.containsKey("maxGuests")) r.setMaxGuests(intVal(item.get("maxGuests")));
        if (item.containsKey("availableRooms")) r.setAvailableRooms(intVal(item.get("availableRooms"))); // legacy
        if (item.containsKey("totalRooms")) r.setTotalRooms(intVal(item.get("totalRooms")));
        if (r.getTotalRooms() == null) r.setTotalRooms(r.getAvailableRooms()); // backfill
        if (item.containsKey("bedType")) r.setBedType(str(item.get("bedType")));
        if (item.containsKey("facilities")) r.setFacilities(str(item.get("facilities")));
        if (item.containsKey("image1")) r.setImage1(str(item.get("image1")));
        if (item.containsKey("image2")) r.setImage2(str(item.get("image2")));
        if (item.containsKey("image3")) r.setImage3(str(item.get("image3")));
        if (item.containsKey("image4")) r.setImage4(str(item.get("image4")));
        if (item.containsKey("description")) r.setDescription(str(item.get("description")));
    }

    // Handles itineraries: clears old rows for this package and inserts the new list if provided.
    private void applyItinerary(TravelPackage p, Map<String, Object> item) {
        if (p == null || item == null) return;
        Object rawList = item.get("itinerary");
        if (!(rawList instanceof List<?> list)) return;
        itineraryRepo.deleteByTravelPackageId(p.getId());
        List<PackageItinerary> saved = new ArrayList<>();
        for (Object o : list) {
            if (!(o instanceof Map<?,?> m)) continue;
            PackageItinerary it = new PackageItinerary();
            it.setTravelPackage(p);
            Object dn = m.get("dayNumber");
            int day = 0;
            if (dn instanceof Number n) { day = n.intValue(); }
            else {
                try { day = Integer.parseInt(String.valueOf(dn)); } catch (Exception ignore) {}
            }
            it.setDayNumber(day <= 0 ? 1 : day);
            it.setTitle(str(m.get("title")));
            it.setSubtitle(str(m.get("subtitle")));
            saved.add(it);
        }
        if (!saved.isEmpty()) itineraryRepo.saveAll(saved);
    }

    // Helper to convert any object to String while tolerating nulls.
    private String str(Object o) { return o == null ? null : String.valueOf(o); }
    private String norm(String s) { return s == null ? null : s.trim(); }
    // Helper to convert payload values into BigDecimal.
    private BigDecimal toBigDecimal(Object o) {
        if (o == null) return null;
        if (o instanceof Number n) return new BigDecimal(n.toString());
        return new BigDecimal(o.toString());
    }

    private int intVal(Object o) { try { return o == null ? 0 : Integer.parseInt(String.valueOf(o)); } catch (Exception e) { return 0; } }
    private boolean bool(Object o) { return o != null && Boolean.parseBoolean(String.valueOf(o)); }
    private boolean hasMatchingDestination(String location) {
        String loc = norm(location);
        if (loc == null || loc.isEmpty()) return false;
        return destinationRepo.existsByNameIgnoreCaseAndActiveTrue(loc);
    }
    private boolean hasMatchingPackage(String destinationName) {
        return findMatchingPackageId(destinationName).isPresent();
    }

    private Optional<UUID> findMatchingPackageId(String destinationName) {
        String name = norm(destinationName);
        if (name == null || name.isEmpty()) return Optional.empty();
        return pkgRepo.findFirstByLocationIgnoreCaseAndActiveTrueOrderByNameAsc(name)
                .map(TravelPackage::getId);
    }

    // Builds a map representing a TravelPackage including its itinerary for admin UI.
    private Map<String, Object> toPackagePayload(TravelPackage p) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", p.getId());
        m.put("name", p.getName());
        m.put("location", p.getLocation());
        m.put("basePrice", p.getBasePrice());
        m.put("destImageUrl", p.getDestImageUrl());
        m.put("hotelImageUrl", p.getHotelImageUrl());
        m.put("image1", p.getImage1());
        m.put("image2", p.getImage2());
        m.put("image3", p.getImage3());
        m.put("image4", p.getImage4());
        m.put("image5", p.getImage5());
        m.put("overview", p.getOverview());
        m.put("locationPoints", p.getLocationPoints());
        m.put("timing", p.getTiming());
        m.put("groupSize", p.getGroupSize());
        m.put("active", p.isActive());
        m.put("packageAvailable", p.isPackageAvailable());
        List<Map<String, Object>> its = new ArrayList<>();
        List<PackageItinerary> steps = itineraryRepo.findByTravelPackageIdOrderByDayNumberAsc(p.getId());
        for (PackageItinerary it : steps) {
            Map<String, Object> im = new HashMap<>();
            im.put("dayNumber", it.getDayNumber());
            im.put("title", it.getTitle());
            im.put("subtitle", it.getSubtitle());
            its.add(im);
        }
        m.put("itinerary", its);
        return m;
    }

    // Builds a success response map with ok=true.
    private Map<String, Object> ok() { Map<String, Object> m = new HashMap<>(); m.put("ok", true); return m; }
    // Builds an error response map with ok=false and a message.
    private Map<String, Object> err(String msg) { Map<String, Object> m = new HashMap<>(); m.put("ok", false); m.put("msg", msg); return m; }
}
