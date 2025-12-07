/* Talks to the lightweight admin server over a direct socket connection so
   admins can log in and list, add, edit, or delete packages from the dashboard.
   Speaks simple JSON over TCP using Jackson, which keeps the admin tools
   decoupled from the main REST API. */
package com.travel.frontend.admin;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.*;
import java.math.BigDecimal;
import java.net.Socket;
import java.util.*;

public class AdminSocketClient {
    private final String host;
    private final int port;
    private final ObjectMapper mapper = new ObjectMapper();
    private String token;

    /* Default constructor reads host/port from system properties or
       environment variables so the admin channel can be pointed at staging or
       production without code edits. */
    public AdminSocketClient() {
        String envHost = System.getenv("ADMIN_SOCKET_HOST");
        String envPort = System.getenv("ADMIN_SOCKET_PORT");
        String sysHost = System.getProperty("admin.socket.host");
        String sysPort = System.getProperty("admin.socket.port");
        String h = (sysHost != null && !sysHost.isBlank()) ? sysHost
                : (envHost != null && !envHost.isBlank()) ? envHost
                : "127.0.0.1";
        int p;
        try {
            String raw = (sysPort != null && !sysPort.isBlank()) ? sysPort : envPort;
            p = raw == null ? 9090 : Integer.parseInt(raw.trim());
        } catch (Exception e) {
            p = 9090;
        }
        this.host = h; this.port = p;
    }
    /* Direct constructor, useful for tests or custom wiring when we want to
       mock out the socket server. */
    public AdminSocketClient(String host, int port) { this.host = host; this.port = port; }

    /* Sends an AUTH request with the provided email and password, storing the
       returned token on success so later LIST/CREATE calls stay authorized. */
    public boolean auth(String email, String password) throws IOException {
        Map<String, Object> req = new HashMap<>();
        req.put("type", "AUTH");
        req.put("email", email);
        req.put("password", password);
        Map<String, Object> res = call(req);
        if (Boolean.TRUE.equals(res.get("ok"))) {
            token = (String) res.get("token");
            AdminSession.setToken(token);
            return true;
        }
        return false;
    }

    /* Pulls the full package list by sending a LIST command over the socket.
       Uses Jackson’s TypeReference to convert the JSON array into Java objects
       before handing them to the UI. */
    public List<PackageVM> list() throws IOException {
        Map<String, Object> req = new HashMap<>();
        req.put("type", "LIST");
        req.put("token", effectiveToken());
        Map<String, Object> res = call(req);
        if (!Boolean.TRUE.equals(res.get("ok"))) throw new IOException("LIST failed: " + res.get("msg"));
        List<PackageVM> items = mapper.convertValue(res.get("items"), new TypeReference<List<PackageVM>>(){});
        return items == null ? List.of() : items;
    }

    /* Destination helpers so the admin can manage destinations over the same socket channel. */
    public List<DestinationVM> listDestinations() throws IOException {
        Map<String, Object> req = new HashMap<>();
        req.put("type", "DEST_LIST");
        req.put("token", effectiveToken());
        Map<String, Object> res = call(req);
        if (!Boolean.TRUE.equals(res.get("ok"))) throw new IOException("DEST_LIST failed: " + res.get("msg"));
        List<DestinationVM> items = mapper.convertValue(res.get("items"), new TypeReference<List<DestinationVM>>() {});
        return items == null ? List.of() : items;
    }

    // ===== Hotels =====
    public List<HotelVM> listHotels(String destinationId) throws IOException {
        Map<String, Object> req = new HashMap<>();
        req.put("type", "HOTEL_LIST");
        req.put("token", effectiveToken());
        req.put("destinationId", destinationId);
        Map<String, Object> res = call(req);
        if (!Boolean.TRUE.equals(res.get("ok"))) throw new IOException("HOTEL_LIST failed: " + res.get("msg"));
        List<HotelVM> items = mapper.convertValue(res.get("items"), new TypeReference<List<HotelVM>>() {});
        return items == null ? List.of() : items;
    }

    public String createHotel(HotelVM vm) throws IOException {
        Map<String, Object> req = new HashMap<>();
        req.put("type", "HOTEL_CREATE");
        req.put("token", effectiveToken());
        req.put("item", vm);
        Map<String, Object> res = call(req);
        if (!Boolean.TRUE.equals(res.get("ok"))) throw new IOException("HOTEL_CREATE failed: " + res.get("msg"));
        return (String) res.get("id");
    }

    public void updateHotel(String id, HotelVM vm) throws IOException {
        Map<String, Object> req = new HashMap<>();
        req.put("type", "HOTEL_UPDATE");
        req.put("token", effectiveToken());
        req.put("id", id);
        req.put("item", vm);
        Map<String, Object> res = call(req);
        if (!Boolean.TRUE.equals(res.get("ok"))) throw new IOException("HOTEL_UPDATE failed: " + res.get("msg"));
    }

    public void deleteHotel(String id) throws IOException {
        Map<String, Object> req = new HashMap<>();
        req.put("type", "HOTEL_DELETE");
        req.put("token", effectiveToken());
        req.put("id", id);
        Map<String, Object> res = call(req);
        if (!Boolean.TRUE.equals(res.get("ok"))) throw new IOException("HOTEL_DELETE failed: " + res.get("msg"));
    }

    public List<RoomVM> listRooms(String hotelId) throws IOException {
        Map<String, Object> req = new HashMap<>();
        req.put("type", "ROOM_LIST");
        req.put("token", effectiveToken());
        req.put("hotelId", hotelId);
        Map<String, Object> res = call(req);
        if (!Boolean.TRUE.equals(res.get("ok"))) throw new IOException("ROOM_LIST failed: " + res.get("msg"));
        List<RoomVM> items = mapper.convertValue(res.get("items"), new TypeReference<List<RoomVM>>() {});
        return items == null ? List.of() : items;
    }

    public void saveRooms(String hotelId, List<RoomVM> rooms) throws IOException {
        Map<String, Object> req = new HashMap<>();
        req.put("type", "ROOM_SAVE");
        req.put("token", effectiveToken());
        req.put("hotelId", hotelId);
        req.put("items", rooms);
        Map<String, Object> res = call(req);
        if (!Boolean.TRUE.equals(res.get("ok"))) throw new IOException("ROOM_SAVE failed: " + res.get("msg"));
    }

    public String createDestination(DestinationVM vm) throws IOException {
        Map<String, Object> req = new HashMap<>();
        req.put("type", "DEST_CREATE");
        req.put("token", effectiveToken());
        req.put("item", vm);
        Map<String, Object> res = call(req);
        if (!Boolean.TRUE.equals(res.get("ok"))) throw new IOException("DEST_CREATE failed: " + res.get("msg"));
        return (String) res.get("id");
    }

    public void updateDestination(String id, DestinationVM vm) throws IOException {
        Map<String, Object> req = new HashMap<>();
        req.put("type", "DEST_UPDATE");
        req.put("token", effectiveToken());
        req.put("id", id);
        req.put("item", vm);
        Map<String, Object> res = call(req);
        if (!Boolean.TRUE.equals(res.get("ok"))) throw new IOException("DEST_UPDATE failed: " + res.get("msg"));
    }

    public void deleteDestination(String id) throws IOException {
        Map<String, Object> req = new HashMap<>();
        req.put("type", "DEST_DELETE");
        req.put("token", effectiveToken());
        req.put("id", id);
        Map<String, Object> res = call(req);
        if (!Boolean.TRUE.equals(res.get("ok"))) throw new IOException("DEST_DELETE failed: " + res.get("msg"));
    }

    /* Sends a new package record to the server. Relies on the server to return
       a generated id so the UI can keep selections in sync. */
    public String create(PackageVM vm) throws IOException {
        Map<String, Object> req = new HashMap<>();
        req.put("type", "CREATE");
        req.put("token", effectiveToken());
        req.put("item", vm);
        Map<String, Object> res = call(req);
        if (!Boolean.TRUE.equals(res.get("ok"))) throw new IOException("CREATE failed: " + res.get("msg"));
        return (String) res.get("id");
    }

    /* Issues an UPDATE command with the chosen package id and the edited form
       values, leaning on the socket token to prove admin rights. */
    public void update(String id, PackageVM vm) throws IOException {
        Map<String, Object> req = new HashMap<>();
        req.put("type", "UPDATE");
        req.put("token", effectiveToken());
        req.put("id", id);
        req.put("item", vm);
        Map<String, Object> res = call(req);
        if (!Boolean.TRUE.equals(res.get("ok"))) throw new IOException("UPDATE failed: " + res.get("msg"));
    }

    /* Removes a package by id. The dashboard calls this when the admin presses
       Delete, and the server is expected to broadcast the change. */
    public void delete(String id) throws IOException {
        Map<String, Object> req = new HashMap<>();
        req.put("type", "DELETE");
        req.put("token", effectiveToken());
        req.put("id", id);
        Map<String, Object> res = call(req);
        if (!Boolean.TRUE.equals(res.get("ok"))) throw new IOException("DELETE failed: " + res.get("msg"));
    }

    /* Chooses between the token captured during this client’s auth() call and
       the shared AdminSession copy, so dashboard screens stay logged in even
       if they spin up a second client. */
    private String effectiveToken() {
        return token != null ? token : AdminSession.getToken();
    }

    /* Low-level helper: opens a Socket, writes a single JSON line, waits for a
       reply, and converts it back into a map. Covers connection timeouts and
       stream handling so each action method stays small. */
    private Map<String, Object> call(Map<String, Object> req) throws IOException {
        return callWithRetry(req, 2);
    }

    private Map<String, Object> callWithRetry(Map<String, Object> req, int retries) throws IOException {
        try (Socket s = new Socket()) {
            try {
                s.connect(new java.net.InetSocketAddress(host, port), 8000);
                s.setSoTimeout(30000);
            } catch (IOException ce) {
                throw new IOException("Connect failed to " + host + ":" + port + ": " + ce.getMessage(), ce);
            }
            try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(s.getOutputStream()));
                 BufferedReader br = new BufferedReader(new InputStreamReader(s.getInputStream()))) {
                bw.write(mapper.writeValueAsString(req));
                bw.write("\n");
                bw.flush();
                String line = br.readLine();
                if (line == null) throw new IOException("No response from admin socket " + host + ":" + port);
                try {
                    return mapper.readValue(line, new TypeReference<Map<String, Object>>(){});
                } catch (IOException parseEx) {
                    throw new IOException("Bad response from admin socket: " + line, parseEx);
                }
            }
        } catch (java.net.SocketTimeoutException ste) {
            if (retries > 0) {
                try { Thread.sleep(200); } catch (InterruptedException ignored) {}
                return callWithRetry(req, retries - 1);
            }
            throw ste;
        }
    }
    /* Lightweight view-model that mirrors what the admin socket sends back.
       Keeps only the fields needed on the dashboard list and edit form. */
    public static class PackageVM {
        public String id;
        public String name;
        public String location;
        public BigDecimal basePrice;
        public String destImageUrl;
        public String hotelImageUrl;
        public String image1;
        public String image2;
        public String image3;
        public String image4;
        public String image5;
        public String overview;
        public String locationPoints;
        public String timing;
        public List<ItineraryVM> itinerary;
        public String groupSize;
        public String bookingDeadline;
        public boolean active = true;
        public boolean packageAvailable;
        public String toString() { return name + " (" + location + ")"; }
    }

    public static class ItineraryVM {
        public int dayNumber;
        public String title;
        public String subtitle;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DestinationVM {
        public String id;
        public String name;
        public String region;
        public String tags;
        public String bestSeason;
        public String imageUrl;
        public boolean active = true;
        public boolean packageAvailable; // server computed for display
        public String packageId;
        public String toString() { return name + " (" + region + ")"; }
    }

    public static class HotelVM {
        public String id;
        public String destinationId;
        public String name;
        public String rating;
        public String realPrice;
        public String currentPrice;
        public String location;
        public String nearby;
        public String facilities;
        public String description;
        public Integer roomsCount;
        public Integer floorsCount;
        public String image1;
        public String image2;
        public String image3;
        public String image4;
        public String image5;
        public String gallery;
        public String toString() { return name == null ? "(untitled hotel)" : name; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class RoomVM {
        public String id;
        public String hotelId;
        public String name;
        public String price; // legacy
        public String realPrice;
        public String currentPrice;
        public Integer maxGuests;
        public Integer availableRooms; // legacy
        public Integer totalRooms;
        public String bedType;
        public String facilities;
        public String description;
        public String image1;
        public String image2;
        public String image3;
        public String image4;
        public String toString() { return name == null ? "(room)" : name; }
    }
}
