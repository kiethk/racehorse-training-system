package com.rtms.backend.service;
import com.rtms.backend.dto.OwnerRegistrationRequest;
import com.rtms.backend.dto.OwnerRegistrationResponse;
import com.rtms.backend.entity.Role;
import com.rtms.backend.entity.User;
import com.rtms.backend.repository.GroomProfileRepository;
import com.rtms.backend.repository.RefreshTokenRepository;
import com.rtms.backend.repository.RoleRepository;
import com.rtms.backend.repository.TrainerProfileRepository;
import com.rtms.backend.repository.UserRepository;
import com.rtms.backend.repository.VeterinarianProfileRepository;
import com.rtms.backend.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AuthServiceRegistrationTest {

    private UserRepository userRepository;
    private RoleRepository roleRepository;
    private PasswordEncoder passwordEncoder;
    private AuthService authService;

    private Role horseOwnerRole;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        roleRepository = mock(RoleRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        JwtUtil jwtUtil = mock(JwtUtil.class);
        VeterinarianProfileRepository vetRepo = mock(VeterinarianProfileRepository.class);
        TrainerProfileRepository trainerRepo = mock(TrainerProfileRepository.class);
        GroomProfileRepository groomRepo = mock(GroomProfileRepository.class);

        RefreshTokenRepository refreshTokenRepository = mock(RefreshTokenRepository.class);

        authService = new AuthService(userRepository, roleRepository, passwordEncoder,
                jwtUtil, vetRepo, trainerRepo, groomRepo, refreshTokenRepository);

        horseOwnerRole = new Role();
        horseOwnerRole.setId(1L);
        horseOwnerRole.setName("HORSE_OWNER");

        when(roleRepository.findByName("HORSE_OWNER")).thenReturn(Optional.of(horseOwnerRole));
        when(passwordEncoder.encode(anyString())).thenReturn("encoded_password");

        // Default: no duplicate email
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        // save() returns the user with an ID
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            if (u.getId() == null) {
                // simulate DB id assignment
                try {
                    var f = User.class.getDeclaredField("id");
                    f.setAccessible(true);
                    f.set(u, 42L);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
            return u;
        });
    }

    // ─── Helper ──────────────────────────────────────────────────────────────

    private OwnerRegistrationRequest validRequest() {
        OwnerRegistrationRequest req = new OwnerRegistrationRequest();
        req.setFullName("Nguyen Van A");
        req.setEmail("nguyenvana@example.com");
        req.setPassword("Secret123!");
        req.setPhone("0901234567");
        req.setAddress("123 Nguyen Hue, HCM");
        return req;
    }

    // ─── Tests ───────────────────────────────────────────────────────────────

    @Test
    void successfulRegistration_createsHorseOwner() {
        OwnerRegistrationResponse response = authService.registerOwner(validRequest());

        assertNotNull(response);
        assertEquals("Nguyen Van A", response.getFullName());
        assertEquals("nguyenvana@example.com", response.getEmail());
        assertEquals("HORSE_OWNER", response.getRole());
        assertNotNull(response.getUserId());
    }

    @Test
    void successfulRegistration_emailIsNormalizedToLowercase() {
        OwnerRegistrationRequest req = validRequest();
        req.setEmail("  NguyenVanA@Example.COM  ");

        authService.registerOwner(req);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertEquals("nguyenvana@example.com", captor.getValue().getEmail());
    }

    @Test
    void successfulRegistration_passwordIsEncodedNotRaw() {
        OwnerRegistrationRequest req = validRequest();
        req.setPassword("plaintext_password");

        authService.registerOwner(req);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());

        String storedHash = captor.getValue().getPasswordHash();
        assertNotEquals("plaintext_password", storedHash,
                "Raw password must never be stored");
        assertEquals("encoded_password", storedHash,
                "Password must be encoded through PasswordEncoder");
        verify(passwordEncoder).encode("plaintext_password");
    }

    @Test
    void duplicateEmail_isRejectedWithConflict() {
        when(userRepository.findByEmail("nguyenvana@example.com"))
                .thenReturn(Optional.of(new User()));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> authService.registerOwner(validRequest()));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        assertTrue(ex.getReason().contains("already exists"));
        verify(userRepository, never()).save(any());
    }

    @Test
    void requestCannotChooseRole_roleIsAlwaysHorseOwner() {
        // OwnerRegistrationRequest has no role field — this test confirms that
        // regardless of what the service loads, the role is always HORSE_OWNER.
        authService.registerOwner(validRequest());

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertEquals("HORSE_OWNER", captor.getValue().getRole().getName());

        // Only HORSE_OWNER was ever fetched from role repo
        verify(roleRepository, times(1)).findByName("HORSE_OWNER");
        verify(roleRepository, never()).findByName(argThat(name -> !"HORSE_OWNER".equals(name)));
    }

    @Test
    void missingFullName_throwsBadRequest() {
        OwnerRegistrationRequest req = validRequest();
        req.setFullName("   ");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> authService.registerOwner(req));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void missingEmail_throwsBadRequest() {
        OwnerRegistrationRequest req = validRequest();
        req.setEmail(null);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> authService.registerOwner(req));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void missingPassword_throwsBadRequest() {
        OwnerRegistrationRequest req = validRequest();
        req.setPassword("");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> authService.registerOwner(req));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void optionalFields_phone_address_areAllowedToBeNull() {
        OwnerRegistrationRequest req = validRequest();
        req.setPhone(null);
        req.setAddress(null);

        assertDoesNotThrow(() -> authService.registerOwner(req));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertNull(captor.getValue().getPhone());
        assertNull(captor.getValue().getAddress());
    }
}
