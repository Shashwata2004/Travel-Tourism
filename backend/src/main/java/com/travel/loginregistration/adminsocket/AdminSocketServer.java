package com.travel.loginregistration.adminsocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.travel.loginregistration.model.AdminUser;
import com.travel.loginregistration.model.TravelPackage;
import com.travel.loginregistration.repository.AdminUserRepository;
import com.travel.loginregistration.repository.TravelPackageRepository;
import jakarta.annotation.PostConstruct;
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
    private final BCryptPasswordEncoder encoder;
    private final ObjectMapper mapper = new ObjectMapper();
    private final Map<String, UUID> sessions = new ConcurrentHashMap<>();

    public AdminSocketServer(AdminUserRepository adminRepo, TravelPackageRepository pkgRepo, BCryptPasswordEncoder encoder) {
        this.adminRepo = adminRepo;
        this.pkgRepo = pkgRepo;
        this.encoder = encoder;
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
                case "CREATE" -> {
                    if (!authorized(req)) { res = err("UNAUTHORIZED"); break; }
                    res = createPackage(req);
                }
                case "UPDATE" -> {
                    if (!authorized(req)) { res = err("UNAUTHORIZED"); break; }
                    res = updatePackage(req);
                }
                case "DELETE" -> {
                    if (!authorized(req)) { res = err("UNAUTHORIZED"); break; }
                    res = deletePackage(req);
                }
                default -> res = err("UNKNOWN_TYPE");
            }
            bw.write(mapper.writeValueAsString(res));
            bw.write("\n");
            bw.flush();
        } catch (Exception e) {
            // ignore per-connection errors
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
        Map<String, Object> ok = ok();
        ok.put("items", items);
        return ok;
    }

    // Creates a new TravelPackage entity from the payload.
    // A payload is the actual data being sent in a request or response. 
    private Map<String, Object> createPackage(Map<String, Object> req) {
        Map<String, Object> item = (Map<String, Object>) req.get("item");
        TravelPackage p = new TravelPackage();
        apply(p, item);
        pkgRepo.save(p);
        Map<String, Object> ok = ok();
        ok.put("id", p.getId());
        return ok;
    }

    // Applies the payload to an existing TravelPackage and saves it.
    private Map<String, Object> updatePackage(Map<String, Object> req) {
        String idStr = (String) req.get("id");
        if (idStr == null) return err("MISSING_ID");
        UUID id = UUID.fromString(idStr);
        TravelPackage p = pkgRepo.findById(id).orElse(null);
        if (p == null) return err("NOT_FOUND");
        Map<String, Object> item = (Map<String, Object>) req.get("item");
        apply(p, item);
        pkgRepo.save(p);
        return ok();
    }

    // Deletes the package with the provided id (if it exists).
    private Map<String, Object> deletePackage(Map<String, Object> req) {
        String idStr = (String) req.get("id");
        if (idStr == null) return err("MISSING_ID");
        UUID id = UUID.fromString(idStr);
        if (pkgRepo.existsById(id)) pkgRepo.deleteById(id);
        return ok();
    }

    // Copies allowed fields from the arbitrary map into the entity.
    private void apply(TravelPackage p, Map<String, Object> item) {
        if (item == null) return;
        if (item.containsKey("name")) p.setName(str(item.get("name")));
        if (item.containsKey("location")) p.setLocation(str(item.get("location")));
        if (item.containsKey("basePrice")) p.setBasePrice(toBigDecimal(item.get("basePrice")));
        if (item.containsKey("destImageUrl")) p.setDestImageUrl(str(item.get("destImageUrl")));
        if (item.containsKey("hotelImageUrl")) p.setHotelImageUrl(str(item.get("hotelImageUrl")));
        if (item.containsKey("overview")) p.setOverview(str(item.get("overview")));
        if (item.containsKey("locationPoints")) p.setLocationPoints(str(item.get("locationPoints")));
        if (item.containsKey("timing")) p.setTiming(str(item.get("timing")));
        if (item.containsKey("active")) p.setActive(Boolean.TRUE.equals(item.get("active")) || "true".equalsIgnoreCase(str(item.get("active"))));
    }

    // Helper to convert any object to String while tolerating nulls.
    private String str(Object o) { return o == null ? null : String.valueOf(o); }
    // Helper to convert payload values into BigDecimal.
    private BigDecimal toBigDecimal(Object o) {
        if (o == null) return null;
        if (o instanceof Number n) return new BigDecimal(n.toString());
        return new BigDecimal(o.toString());
    }

    // Builds a success response map with ok=true.
    private Map<String, Object> ok() { Map<String, Object> m = new HashMap<>(); m.put("ok", true); return m; }
    // Builds an error response map with ok=false and a message.
    private Map<String, Object> err(String msg) { Map<String, Object> m = new HashMap<>(); m.put("ok", false); m.put("msg", msg); return m; }
}
