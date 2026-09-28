package com.rtms.backend.service;

import com.rtms.backend.dto.LoginRequest;
import com.rtms.backend.dto.LoginResponse;
import com.rtms.backend.dto.OwnerRegistrationRequest;
import com.rtms.backend.dto.OwnerRegistrationResponse;
import com.rtms.backend.entity.Role;
import com.rtms.backend.entity.User;
import com.rtms.backend.repository.GroomProfileRepository;
import com.rtms.backend.repository.RoleRepository;
import com.rtms.backend.repository.TrainerProfileRepository;
import com.rtms.backend.repository.UserRepository;
import com.rtms.backend.repository.VeterinarianProfileRepository;
import com.rtms.backend.security.JwtUtil;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    private final VeterinarianProfileRepository veterinarianProfileRepository;
    private final TrainerProfileRepository trainerProfileRepository;
    private final GroomProfileRepository groomProfileRepository;

    public AuthService(UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            JwtUtil jwtUtil,
            VeterinarianProfileRepository veterinarianProfileRepository,
            TrainerProfileRepository trainerProfileRepository,
            GroomProfileRepository groomProfileRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.veterinarianProfileRepository = veterinarianProfileRepository;
        this.trainerProfileRepository = trainerProfileRepository;
        this.groomProfileRepository = groomProfileRepository;
    }

    /**
     * Xac minh thong tin dang nhap, tra ve LoginResult chua (token, loginResponse).
     * Controller se lay token gan vao Cookie, con loginResponse tra ve body.
     * Nem RuntimeException neu sai thong tin.
     */
    public LoginResult login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Email or Password is incorrect"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new RuntimeException("Email or Password is incorrect");
        }

        String token = jwtUtil.generateToken(user.getId(), user.getEmail(), user.getRole().getName());
        LoginResponse loginResponse = new LoginResponse(user.getId(), user.getFullName(),
                user.getEmail(), user.getRole().getName());

        return new LoginResult(token, loginResponse);
    }

    // Inner record de tra ve ca token lan response body cung luc
    public record LoginResult(String token, LoginResponse loginResponse) {
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

    /**
     * Đăng ký tài khoản Horse Owner mới.
     * Client không được phép chỉ định role — role luôn là HORSE_OWNER.
     */
    public OwnerRegistrationResponse registerOwner(OwnerRegistrationRequest request) {
        // Validate required fields
        if (request.getFullName() == null || request.getFullName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Full name is required");
        }
        if (request.getEmail() == null || request.getEmail().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email is required");
        }
        if (request.getPassword() == null || request.getPassword().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password is required");
        }

        // Normalize fields
        String email = request.getEmail().trim().toLowerCase();
        String fullName = request.getFullName().trim();
        String phone = request.getPhone() == null || request.getPhone().isBlank()
                ? null : request.getPhone().trim();
        String address = request.getAddress() == null || request.getAddress().isBlank()
                ? null : request.getAddress().trim();

        // Reject duplicate email
        if (userRepository.findByEmail(email).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "An account with this email already exists");
        }

        // Load HORSE_OWNER role from DB
        Role ownerRole = roleRepository.findByName("HORSE_OWNER")
                .orElseThrow(() -> new IllegalStateException("HORSE_OWNER role not found in database"));

        // Create user
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