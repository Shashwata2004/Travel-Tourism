package com.travel.loginregistration.config;

import com.travel.loginregistration.model.AdminUser;
import com.travel.loginregistration.repository.AdminUserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import io.github.cdimascio.dotenv.Dotenv;
import java.util.Locale;
import java.util.Optional;

/*
 * create or update an admin user based on environment variables:  (.env file)
 * ADMIN_EMAIL - email address of the admin user to create or update (if not set, no admin user is created)
 * ADMIN_USERNAME - username of the admin user to create or update (default: "admin")
 * ADMIN_PASSWORD - password of the admin user to create or update (if not set, no admin user is created)
 */

@Configuration
public class AdminSeeder implements CommandLineRunner {
    private final AdminUserRepository repo;
    private final BCryptPasswordEncoder encoder;

    public AdminSeeder(AdminUserRepository repo, BCryptPasswordEncoder encoder) {
        this.repo = repo;
        this.encoder = encoder;
    }

    @Override
    public void run(String... args) {
        String email = resolve("ADMIN_EMAIL");
        String username = resolveOr("ADMIN_USERNAME", "admin");
        String password = resolve("ADMIN_PASSWORD");

        if (email == null || email.isBlank()) {
            System.out.println("[AdminSeeder] ADMIN_EMAIL not set; skipping admin bootstrap.");
            return;
        }

        String normEmail = email.toLowerCase(Locale.ROOT);
        Optional<AdminUser> existing = repo.findByEmail(normEmail);

        if (existing.isEmpty()) {
            if (password == null || password.isBlank()) {
                System.out.println("[AdminSeeder] ADMIN_PASSWORD not set; cannot create admin for " + normEmail);
                return;
            }
            AdminUser u = new AdminUser();
            u.setEmail(normEmail);
            u.setUsername(username);
            u.setPasswordHash(encoder.encode(password));
            u.setRole("ADMIN");
            repo.save(u);
            System.out.println("[AdminSeeder] Ensured admin account for " + normEmail);
        } else {
            AdminUser u = existing.get();
            boolean changed = false;
            if (username != null && !username.isBlank() && !username.equals(u.getUsername())) {
                u.setUsername(username);
                changed = true;
            }
            if (password != null && !password.isBlank()) {
                u.setPasswordHash(encoder.encode(password));
                changed = true;
                System.out.println("[AdminSeeder] Updated admin password for " + normEmail);
            }
            if (changed) repo.save(u);
            if (!changed) {
                System.out.println("[AdminSeeder] Admin account already exists for " + normEmail);
            }
        }
    }

    private static String resolve(String key) {
        String v = System.getProperty(key);
        if (v == null || v.isBlank()) v = System.getenv(key);
        if (v == null || v.isBlank()) {
            try {
                Dotenv base = Dotenv.configure().ignoreIfMalformed().ignoreIfMissing().load();
                v = base.get(key);
                if (v == null || v.isBlank()) {
                    Dotenv backend = Dotenv.configure().directory("backend").ignoreIfMalformed().ignoreIfMissing().load();
                    v = backend.get(key);
                }
            } catch (Exception ignored) {}
        }
        return (v == null || v.isBlank()) ? null : v;
    }

    private static String resolveOr(String key, String def) {
        String v = resolve(key);
        return (v == null || v.isBlank()) ? def : v;
    }
}