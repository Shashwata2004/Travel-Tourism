package com.travel.loginregistration.service;

import com.travel.loginregistration.dto.RegisterRequest;
import com.travel.loginregistration.model.User;
import com.travel.loginregistration.repository.UserRepository;
import com.travel.loginregistration.security.JwtUtil;
import com.travel.loginregistration.exception.UserNotFoundException;
import com.travel.loginregistration.exception.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthService(UserRepository userRepository,
                       BCryptPasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    // ---------- REGISTER ----------
    @Transactional
    public String registerUser(RegisterRequest req) {
        if (req.getEmail() == null || req.getEmail().isBlank()) {
            throw new IllegalArgumentException("Email is required");
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
}
