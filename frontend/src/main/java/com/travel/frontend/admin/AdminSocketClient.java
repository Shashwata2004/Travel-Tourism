package com.travel.frontend.admin;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.math.BigDecimal;
import java.net.Socket;
import java.util.*;

public class AdminSocketClient {
    private final String host;
    private final int port;
    private final ObjectMapper mapper = new ObjectMapper();
    private String token;

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
    public AdminSocketClient(String host, int port) { this.host = host; this.port = port; }

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

    public List<PackageVM> list() throws IOException {
        Map<String, Object> req = new HashMap<>();
        req.put("type", "LIST");
        req.put("token", effectiveToken());
        Map<String, Object> res = call(req);
        if (!Boolean.TRUE.equals(res.get("ok"))) throw new IOException("LIST failed: " + res.get("msg"));
        List<PackageVM> items = mapper.convertValue(res.get("items"), new TypeReference<List<PackageVM>>(){});
        return items == null ? List.of() : items;
    }

    public String create(PackageVM vm) throws IOException {
        Map<String, Object> req = new HashMap<>();
        req.put("type", "CREATE");
        req.put("token", effectiveToken());
        req.put("item", vm);
        Map<String, Object> res = call(req);
        if (!Boolean.TRUE.equals(res.get("ok"))) throw new IOException("CREATE failed: " + res.get("msg"));
        return (String) res.get("id");
    }

    public void update(String id, PackageVM vm) throws IOException {
        Map<String, Object> req = new HashMap<>();
        req.put("type", "UPDATE");
        req.put("token", effectiveToken());
        req.put("id", id);
        req.put("item", vm);
        Map<String, Object> res = call(req);
        if (!Boolean.TRUE.equals(res.get("ok"))) throw new IOException("UPDATE failed: " + res.get("msg"));
    }

    public void delete(String id) throws IOException {
        Map<String, Object> req = new HashMap<>();
        req.put("type", "DELETE");
        req.put("token", effectiveToken());
        req.put("id", id);
        Map<String, Object> res = call(req);
        if (!Boolean.TRUE.equals(res.get("ok"))) throw new IOException("DELETE failed: " + res.get("msg"));
    }

    private String effectiveToken() {
        return token != null ? token : AdminSession.getToken();
    }

    private Map<String, Object> call(Map<String, Object> req) throws IOException {
        try (Socket s = new Socket()) {
            try {
                s.connect(new java.net.InetSocketAddress(host, port), 3000);
                s.setSoTimeout(5000);
            } catch (IOException ce) {
                throw new IOException("Connect failed to " + host + ":" + port + ": " + ce.getMessage(), ce);
            }
            try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(s.getOutputStream()));
                 BufferedReader br = new BufferedReader(new InputStreamReader(s.getInputStream()))) {
            bw.write(mapper.writeValueAsString(req));
            bw.write("\n");
            bw.flush();
            String line = br.readLine();
            if (line == null) throw new IOException("No response");
            return mapper.readValue(line, new TypeReference<Map<String, Object>>(){});
            }
        }
    }

    public static class PackageVM {
        public String id;
        public String name;
        public String location;
        public BigDecimal basePrice;
        public String destImageUrl;
        public String hotelImageUrl;
        public String overview;
        public String locationPoints;
        public String timing;
        public boolean active = true;
        public String toString() { return name + " (" + location + ")"; }
    }
}
