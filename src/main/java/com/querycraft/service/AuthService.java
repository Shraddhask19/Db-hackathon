package com.querycraft.service;

import com.querycraft.domain.Role;
import com.querycraft.domain.UserEntity;
import com.querycraft.domain.dto.AuthResponse;
import com.querycraft.domain.dto.LoginRequest;
import com.querycraft.domain.dto.RegisterRequest;
import com.querycraft.domain.dto.UserSummary;
import com.querycraft.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    // In-memory fallback user store for lightweight mode
    private final ConcurrentHashMap<String, UserEntity> inMemoryUsers = new ConcurrentHashMap<>();

    public AuthService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @PostConstruct
    public void seedDefaultUsers() {
        log.info("Initializing & Seeding Default QueryCraft User & Admin Accounts...");

        // Seed Default Admin Account: admin / admin123
        createAccountIfNotExists("admin", "admin@querycraft.ai", "admin123", Role.ROLE_ADMIN);

        // Seed Default User Account: user / user123
        createAccountIfNotExists("user", "user@querycraft.ai", "user123", Role.ROLE_USER);
    }

    public AuthResponse register(RegisterRequest request) {
        String username = request.getUsername().trim();
        String email = request.getEmail().trim().toLowerCase(Locale.ROOT);

        if (existsByUsername(username)) {
            throw new IllegalArgumentException("Username '" + username + "' is already taken.");
        }
        if (existsByEmail(email)) {
            throw new IllegalArgumentException("Email '" + email + "' is already registered.");
        }

        Role role = request.getRole() != null ? request.getRole() : Role.ROLE_USER;
        String userId = UUID.randomUUID().toString();
        String hash = hashPassword(request.getPassword());

        UserEntity user = UserEntity.builder()
                .id(userId)
                .username(username)
                .email(email)
                .passwordHash(hash)
                .role(role)
                .createdAt(Instant.now())
                .build();

        saveUser(user);
        log.info("Registered new account: Username={}, Role={}", username, role);

        String token = generateToken(user);
        return AuthResponse.builder()
                .token(token)
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole())
                .message("User registered successfully as " + user.getRole().name())
                .timestamp(Instant.now())
                .build();
    }

    public AuthResponse login(LoginRequest request) {
        String username = request.getUsername().trim();
        UserEntity user = findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Invalid username or password."));

        String inputHash = hashPassword(request.getPassword());
        if (!inputHash.equals(user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid username or password.");
        }

        String token = generateToken(user);
        log.info("Successful login: Username={}, Role={}", user.getUsername(), user.getRole());

        return AuthResponse.builder()
                .token(token)
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole())
                .message("Login successful")
                .timestamp(Instant.now())
                .build();
    }

    public List<UserSummary> getAllUsers() {
        List<UserEntity> users = new ArrayList<>();
        try {
            users.addAll(userRepository.findAll());
        } catch (Exception e) {
            users.addAll(inMemoryUsers.values());
        }

        return users.stream()
                .map(u -> UserSummary.builder()
                        .id(u.getId())
                        .username(u.getUsername())
                        .email(u.getEmail())
                        .role(u.getRole())
                        .createdAt(u.getCreatedAt())
                        .build())
                .toList();
    }

    private void createAccountIfNotExists(String username, String email, String password, Role role) {
        if (!existsByUsername(username)) {
            UserEntity user = UserEntity.builder()
                    .id(UUID.randomUUID().toString())
                    .username(username)
                    .email(email)
                    .passwordHash(hashPassword(password))
                    .role(role)
                    .createdAt(Instant.now())
                    .build();
            saveUser(user);
            log.info("Seeded default [{}] account -> Username: '{}', Password: '{}'", role.name(), username, password);
        }
    }

    private void saveUser(UserEntity user) {
        try {
            userRepository.save(user);
        } catch (Exception e) {
            inMemoryUsers.put(user.getUsername().toLowerCase(Locale.ROOT), user);
        }
    }

    private Optional<UserEntity> findByUsername(String username) {
        try {
            Optional<UserEntity> opt = userRepository.findByUsername(username);
            if (opt.isPresent()) return opt;
        } catch (Exception ignored) {}
        return Optional.ofNullable(inMemoryUsers.get(username.toLowerCase(Locale.ROOT)));
    }

    private boolean existsByUsername(String username) {
        return findByUsername(username).isPresent();
    }

    private boolean existsByEmail(String email) {
        try {
            if (userRepository.existsByEmail(email)) return true;
        } catch (Exception ignored) {}
        return inMemoryUsers.values().stream().anyMatch(u -> email.equalsIgnoreCase(u.getEmail()));
    }

    private String hashPassword(String rawPassword) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawPassword.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            return String.valueOf(rawPassword.hashCode());
        }
    }

    private String generateToken(UserEntity user) {
        String payload = user.getUsername() + ":" + user.getRole().name() + ":" + System.currentTimeMillis();
        return "qc_token_" + Base64.getUrlEncoder().withoutPadding().encodeToString(payload.getBytes(StandardCharsets.UTF_8));
    }
}
