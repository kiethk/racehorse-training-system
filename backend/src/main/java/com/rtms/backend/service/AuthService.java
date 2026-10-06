package com.rtms.backend.service;
import com.rtms.backend.dto.LoginRequest;
import com.rtms.backend.dto.LoginResponse;
import com.rtms.backend.dto.OwnerRegistrationRequest;
import com.rtms.backend.dto.OwnerRegistrationResponse;
import com.rtms.backend.entity.RefreshToken;
import com.rtms.backend.entity.Role;
import com.rtms.backend.entity.User;
import com.rtms.backend.repository.GroomProfileRepository;
import com.rtms.backend.repository.RefreshTokenRepository;
import com.rtms.backend.repository.RoleRepository;
import com.rtms.backend.repository.TrainerProfileRepository;
import com.rtms.backend.repository.UserRepository;
import com.rtms.backend.repository.VeterinarianProfileRepository;
import com.rtms.backend.security.JwtUtil;
import com.rtms.backend.config.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    private final VeterinarianProfileRepository veterinarianProfileRepository;
    private final TrainerProfileRepository trainerProfileRepository;
    private final GroomProfileRepository groomProfileRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${jwt.refresh-expiration}")
    private long refreshExpirationMs;

    private final SecureRandom secureRandom = new SecureRandom();

    public AuthService(UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            JwtUtil jwtUtil,
            VeterinarianProfileRepository veterinarianProfileRepository,
            TrainerProfileRepository trainerProfileRepository,
            GroomProfileRepository groomProfileRepository,
            RefreshTokenRepository refreshTokenRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.veterinarianProfileRepository = veterinarianProfileRepository;
        this.trainerProfileRepository = trainerProfileRepository;
        this.groomProfileRepository = groomProfileRepository;
        this.refreshTokenRepository = refreshTokenRepository;
    }

    @Transactional
    public LoginResult login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> invalidCredentials());

        if (!user.isActive()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "USER_INACTIVE", "User is inactive");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw invalidCredentials();
        }

        String accessToken = jwtUtil.generateToken(user.getId(), user.getEmail(), user.getRole().getName());
        String rawRefreshToken = generateOpaqueToken();

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setTokenHash(hashToken(rawRefreshToken));
        refreshToken.setExpiresAt(LocalDateTime.now().plusNanos(refreshExpirationMs * 1000000));
        refreshTokenRepository.save(refreshToken);

        LoginResponse loginResponse = new LoginResponse(user.getId(), user.getFullName(),
                user.getEmail(), user.getRole().getName());

        return new LoginResult(accessToken, rawRefreshToken, loginResponse);
    }

    @Transactional
    public String refresh(String rawRefreshToken) {
        String hash = hashToken(rawRefreshToken);
        RefreshToken refreshToken = refreshTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_REFRESH_TOKEN", "Invalid refresh token"));

        if (refreshToken.getRevokedAt() != null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "REVOKED_REFRESH_TOKEN", "Refresh token has been revoked");
        }

        if (refreshToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "EXPIRED_REFRESH_TOKEN", "Refresh token has expired");
        }

        User user = refreshToken.getUser();
        if (!user.isActive()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "USER_INACTIVE", "User is inactive");
        }

        return jwtUtil.generateToken(user.getId(), user.getEmail(), user.getRole().getName());
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        if (rawRefreshToken != null && !rawRefreshToken.isBlank()) {
            String hash = hashToken(rawRefreshToken);
            refreshTokenRepository.findByTokenHash(hash).ifPresent(token -> {
                token.setRevokedAt(LocalDateTime.now());
                refreshTokenRepository.save(token);
            });
        }
    }

    private String generateOpaqueToken() {
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to hash token", e);
        }
    }

    private ApiException invalidCredentials() {
        return new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS",
                "Email or Password is incorrect");
    }

    public record LoginResult(String token, String refreshToken, LoginResponse loginResponse) {
    }

    public LoginResponse getMe(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        String role = user.getRole().getName();
        Object profile = switch (role) {
            case "VETERINARIAN" -> veterinarianProfileRepository.findById(userId).orElse(null);
            case "HEAD_TRAINER" -> trainerProfileRepository.findById(userId).orElse(null);
            case "GROOM" -> groomProfileRepository.findById(userId).orElse(null);
            default -> null; // HORSE_OWNER, ADMIN không có profile
        };

        return new LoginResponse(user.getId(), user.getFullName(), user.getEmail(), role, profile);
    }

    public OwnerRegistrationResponse registerOwner(OwnerRegistrationRequest request) {
        if (request.getFullName() == null || request.getFullName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Full name is required");
        }
        if (request.getEmail() == null || request.getEmail().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email is required");
        }
        if (request.getPassword() == null || request.getPassword().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password is required");
        }

        String email = request.getEmail().trim().toLowerCase();
        String fullName = request.getFullName().trim();
        String phone = request.getPhone() == null || request.getPhone().isBlank()
                ? null : request.getPhone().trim();
        String address = request.getAddress() == null || request.getAddress().isBlank()
                ? null : request.getAddress().trim();

        if (userRepository.findByEmail(email).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "An account with this email already exists");
        }

        Role ownerRole = roleRepository.findByName("HORSE_OWNER")
                .orElseThrow(() -> new IllegalStateException("HORSE_OWNER role not found in database"));

        User user = new User();
        user.setFullName(fullName);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole(ownerRole);
        user.setPhone(phone);
        user.setAddress(address);
        user.setActive(true);

        User saved = userRepository.save(user);

        return new OwnerRegistrationResponse(
                saved.getId(),
                saved.getFullName(),
                saved.getEmail(),
                saved.getRole().getName());
    }

}
