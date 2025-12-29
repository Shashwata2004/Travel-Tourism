package com.travel.loginregistration.service;

import com.travel.loginregistration.dto.RegisterRequest;
import com.travel.loginregistration.dto.ForgotPasswordVerifyRequest;
import com.travel.loginregistration.dto.ForgotPasswordResetRequest;
import com.travel.loginregistration.model.User;
import com.travel.loginregistration.model.UserProfile;
import com.travel.loginregistration.repository.UserRepository;
import com.travel.loginregistration.repository.UserProfileRepository;
import com.travel.loginregistration.security.JwtUtil;
import com.travel.loginregistration.exception.UserNotFoundException;
import com.travel.loginregistration.exception.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import java.util.regex.Pattern;

/*
    registers new users and logs in existing users, returning JWTs upon successful login
    called by AuthController to handle registration and login requests
    returns success messages or JWT tokens, or throws exceptions on errors
*/

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    public AuthService(UserRepository userRepository,
                       UserProfileRepository userProfileRepository,
                       BCryptPasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.userProfileRepository = userProfileRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    // ---------- REGISTER ----------
    @Transactional
    public String registerUser(RegisterRequest req) {
        // Basic field validation before touching DB
        if (req.getEmail() == null || req.getEmail().isBlank()) {
            throw new IllegalArgumentException("Email is required");
        }
        if (!isValidEmail(req.getEmail())) {
            throw new IllegalArgumentException("Invalid email format");
        }
        if (req.getPassword() == null || req.getPassword().isBlank()) {
            throw new IllegalArgumentException("Password is required");
        }
        if (req.getUsername() == null || req.getUsername().isBlank()) {
            throw new IllegalArgumentException("Username is required");
        }

        String emailNorm = req.getEmail().trim().toLowerCase();

        if (userRepository.findByEmail(emailNorm).isPresent()) {
            throw new IllegalArgumentException("Email already exists");
        }

        // Create new user with hashed password and save
        User user = new User();
        user.setEmail(emailNorm);
        user.setUsername(req.getUsername().trim());
        user.setLocation(req.getLocation());
        user.setPasswordHash(passwordEncoder.encode(req.getPassword()));

        userRepository.save(user);
        return "User registered successfully!";
    }

    // ---------- LOGIN ----------
    public String loginUser(String email, String password) {
        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            throw new IllegalArgumentException("Email and password are required");
        }
        if (!isValidEmail(email)) {
            throw new IllegalArgumentException("Invalid email format");
        }

        String emailNorm = email.trim().toLowerCase();

        // Check if user exists
        User user = userRepository.findByEmail(emailNorm)
                .orElseThrow(() -> new UserNotFoundException("No account found for this email"));

        // Check password match
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new BadCredentialsException("Incorrect password");
        }

        // Return JWT on success
        return jwtUtil.generateToken(emailNorm);
    }

    private boolean isValidEmail(String email) {
        return EMAIL_PATTERN.matcher(email.trim()).matches();
    }

    public void verifyIdentity(ForgotPasswordVerifyRequest req) {
        validateIdentity(req == null ? null : req.email,
                req == null ? null : req.idType,
                req == null ? null : req.idNumber);
    }

    @Transactional
    public void resetPassword(ForgotPasswordResetRequest req) {
        VerifiedIdentity identity = validateIdentity(req == null ? null : req.email,
                req == null ? null : req.idType,
                req == null ? null : req.idNumber);
        if (req == null || req.newPassword == null || req.newPassword.isBlank()) {
            throw new IllegalArgumentException("New password is required");
        }
        User user = identity.user();
        user.setPasswordHash(passwordEncoder.encode(req.newPassword));
        userRepository.save(user);
    }

    private VerifiedIdentity validateIdentity(String emailRaw, String idTypeRaw, String idNumberRaw) {
        String email = emailRaw == null ? null : emailRaw.trim().toLowerCase();
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email is required");
        }
        if (!isValidEmail(email)) {
            throw new IllegalArgumentException("Invalid email format");
        }
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("No account found for this email"));
        UserProfile profile = userProfileRepository.findByUserId(user.getId()).orElse(null);
        if (profile == null || profile.getIdType() == null || profile.getIdType().isBlank()
                || profile.getIdNumber() == null || profile.getIdNumber().isBlank()) {
            throw new IllegalArgumentException("This operation can't be done in this account");
        }
        String reqType = normalizeIdType(idTypeRaw);
        if (!profile.getIdType().equalsIgnoreCase(reqType)) {
            throw new IllegalArgumentException("ID Type does not match");
        }
        String reqNumber = idNumberRaw == null ? "" : idNumberRaw.trim();
        if (reqNumber.isEmpty() || !profile.getIdNumber().equals(reqNumber)) {
            throw new IllegalArgumentException("ID Number does not match");
        }
        return new VerifiedIdentity(user, profile);
    }

    private String normalizeIdType(String idType) {
        if (idType == null || idType.isBlank()) {
            throw new IllegalArgumentException("ID Type is required");
        }
        String v = idType.trim().toUpperCase().replace(' ', '_');
        return switch (v) {
            case "NID", "BIRTH_CERTIFICATE", "PASSPORT" -> v;
            default -> throw new IllegalArgumentException("Invalid idType");
        };
    }

    private record VerifiedIdentity(User user, UserProfile profile) { }
}
