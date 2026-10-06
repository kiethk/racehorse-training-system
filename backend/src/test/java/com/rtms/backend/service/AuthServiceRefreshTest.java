package com.rtms.backend.service;
import com.rtms.backend.dto.LoginRequest;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AuthServiceRefreshTest {

    private UserRepository userRepository;
    private RoleRepository roleRepository;
    private PasswordEncoder passwordEncoder;
    private JwtUtil jwtUtil;
    private VeterinarianProfileRepository veterinarianProfileRepository;
    private TrainerProfileRepository trainerProfileRepository;
    private GroomProfileRepository groomProfileRepository;
    private RefreshTokenRepository refreshTokenRepository;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        roleRepository = mock(RoleRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        jwtUtil = mock(JwtUtil.class);
        veterinarianProfileRepository = mock(VeterinarianProfileRepository.class);
        trainerProfileRepository = mock(TrainerProfileRepository.class);
        groomProfileRepository = mock(GroomProfileRepository.class);
        refreshTokenRepository = mock(RefreshTokenRepository.class);

        authService = new AuthService(
                userRepository, roleRepository, passwordEncoder, jwtUtil,
                veterinarianProfileRepository, trainerProfileRepository,
                groomProfileRepository, refreshTokenRepository
        );

        ReflectionTestUtils.setField(authService, "refreshExpirationMs", 604800000L); // 7 days
    }

    @Test
    void testLoginGeneratesAccessAndRefreshToken() {
        User user = new User();
        user.setId(1L);
        user.setEmail("test@test.com");
        user.setPasswordHash("hashed_pw");
        user.setActive(true);
        Role role = new Role();
        role.setName("CLUB_MANAGER");
        user.setRole(role);

        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password", "hashed_pw")).thenReturn(true);
        when(jwtUtil.generateToken(1L, "test@test.com", "CLUB_MANAGER")).thenReturn("access_token");

        LoginRequest request = new LoginRequest();
        request.setEmail("test@test.com");
        request.setPassword("password");

        AuthService.LoginResult result = authService.login(request);

        assertNotNull(result);
        assertEquals("access_token", result.token());
        assertNotNull(result.refreshToken());
        assertFalse(result.refreshToken().isBlank());

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(captor.capture());

        RefreshToken savedToken = captor.getValue();
        assertEquals(user, savedToken.getUser());
        assertNotNull(savedToken.getTokenHash());
        assertNotEquals(result.refreshToken(), savedToken.getTokenHash()); // Hash is not raw token
        assertNotNull(savedToken.getExpiresAt());
    }

    @Test
    void testRefresh_Success() {
        User user = new User();
        user.setId(1L);
        user.setEmail("test@test.com");
        user.setActive(true);
        Role role = new Role();
        role.setName("CLUB_MANAGER");
        user.setRole(role);

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setExpiresAt(LocalDateTime.now().plusDays(1));
        refreshToken.setRevokedAt(null);

        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(refreshToken));
        when(jwtUtil.generateToken(1L, "test@test.com", "CLUB_MANAGER")).thenReturn("new_access_token");

        String newAccessToken = authService.refresh("some_raw_token");
        assertEquals("new_access_token", newAccessToken);
    }

    @Test
    void testRefresh_UnknownTokenRejected() {
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());

        ApiException ex = assertThrows(ApiException.class, () -> authService.refresh("unknown_token"));
        assertTrue(ex.getMessage().contains("Invalid refresh token"));
    }

    @Test
    void testRefresh_RevokedTokenRejected() {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setRevokedAt(LocalDateTime.now().minusDays(1)); // Revoked
        refreshToken.setExpiresAt(LocalDateTime.now().plusDays(1));

        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(refreshToken));

        ApiException ex = assertThrows(ApiException.class, () -> authService.refresh("revoked_token"));
        assertTrue(ex.getMessage().contains("revoked"));
    }

    @Test
    void testRefresh_ExpiredTokenRejected() {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setRevokedAt(null);
        refreshToken.setExpiresAt(LocalDateTime.now().minusDays(1)); // Expired

        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(refreshToken));

        ApiException ex = assertThrows(ApiException.class, () -> authService.refresh("expired_token"));
        assertTrue(ex.getMessage().contains("expired"));
    }

    @Test
    void testRefresh_InactiveUserRejected() {
        User user = new User();
        user.setActive(false); // Inactive
        
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setRevokedAt(null);
        refreshToken.setExpiresAt(LocalDateTime.now().plusDays(1));

        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(refreshToken));

        ApiException ex = assertThrows(ApiException.class, () -> authService.refresh("token_for_inactive_user"));
        assertTrue(ex.getMessage().contains("inactive"));
    }

    @Test
    void testLogout_MatchingTokenRevoked() {
        RefreshToken refreshToken = new RefreshToken();
        assertNull(refreshToken.getRevokedAt());

        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(refreshToken));

        authService.logout("raw_token");

        assertNotNull(refreshToken.getRevokedAt());
        verify(refreshTokenRepository).save(refreshToken);
    }

    @Test
    void testLogout_MissingTokenIsSafe() {
        authService.logout(null);
        authService.logout("");
        verify(refreshTokenRepository, never()).save(any());
    }
}
