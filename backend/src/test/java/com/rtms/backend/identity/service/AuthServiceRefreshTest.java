package com.rtms.backend.identity.service;

import com.rtms.backend.config.ApiException;
import com.rtms.backend.identity.dto.ChangePasswordRequest;
import com.rtms.backend.identity.dto.LoginRequest;
import com.rtms.backend.identity.entity.RefreshToken;
import com.rtms.backend.identity.entity.Role;
import com.rtms.backend.identity.entity.User;
import com.rtms.backend.identity.repository.GroomProfileRepository;
import com.rtms.backend.identity.repository.RefreshTokenRepository;
import com.rtms.backend.identity.repository.RoleRepository;
import com.rtms.backend.identity.repository.TrainerProfileRepository;
import com.rtms.backend.identity.repository.UserRepository;
import com.rtms.backend.identity.repository.VeterinarianProfileRepository;
import com.rtms.backend.security.JwtUtil;
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

        verify(refreshTokenRepository).revokeAllActiveTokensByUser(eq(user), any(LocalDateTime.class));

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(captor.capture());

        RefreshToken savedToken = captor.getValue();
        assertEquals(user, savedToken.getUser());
        assertNotNull(savedToken.getTokenHash());
        assertNotEquals(result.refreshToken(), savedToken.getTokenHash()); // Hash is not raw token
        assertNotNull(savedToken.getExpiresAt());
    }

    @Test
    void testLogin_RevokesExistingActiveTokensForUser_EnforcesSingleSession() {
        User user = new User();
        user.setId(2L);
        user.setEmail("user2@test.com");
        user.setPasswordHash("hashed_pw");
        user.setActive(true);
        Role role = new Role();
        role.setName("CLUB_MANAGER");
        user.setRole(role);

        when(userRepository.findByEmail("user2@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password", "hashed_pw")).thenReturn(true);
        when(jwtUtil.generateToken(2L, "user2@test.com", "CLUB_MANAGER")).thenReturn("access_token_2");

        LoginRequest request = new LoginRequest();
        request.setEmail("user2@test.com");
        request.setPassword("password");

        authService.login(request);

        ArgumentCaptor<LocalDateTime> cutoffCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(refreshTokenRepository).revokeAllActiveTokensByUser(eq(user), cutoffCaptor.capture());

        // Revocation cutoff must be in the past beyond grace period (30s) so old tokens fail immediately
        assertTrue(cutoffCaptor.getValue().isBefore(LocalDateTime.now().minusSeconds(30)));
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

        AuthService.RefreshResult result = authService.refresh("some_raw_token");
        assertEquals("new_access_token", result.accessToken());
        assertNotNull(result.refreshToken());
    }

    @Test
    void testRefresh_UnknownTokenRejected() {
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());

        ApiException ex = assertThrows(ApiException.class, () -> authService.refresh("unknown_token"));
        assertTrue(ex.getMessage().contains("Invalid refresh token"));
    }

    @Test
    void testRefresh_WithinGracePeriod_Success() {
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
        LocalDateTime initialRevokedAt = LocalDateTime.now().minusSeconds(10);
        refreshToken.setRevokedAt(initialRevokedAt);

        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(refreshToken));
        when(jwtUtil.generateToken(1L, "test@test.com", "CLUB_MANAGER")).thenReturn("new_access_token");

        AuthService.RefreshResult result = authService.refresh("some_raw_token");
        assertEquals("new_access_token", result.accessToken());
        assertNotNull(result.refreshToken());
        assertEquals(initialRevokedAt, refreshToken.getRevokedAt());
    }

    @Test
    void testRefresh_BeyondGracePeriod_RevokedTokenRejected() {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setRevokedAt(LocalDateTime.now().minusSeconds(35)); // Beyond 30s grace period
        refreshToken.setExpiresAt(LocalDateTime.now().plusDays(1));

        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(refreshToken));

        ApiException ex = assertThrows(ApiException.class, () -> authService.refresh("revoked_token"));
        assertTrue(ex.getMessage().contains("revoked"));
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

    @Test
    void testChangePassword_Success() {
        User user = new User();
        user.setId(5L);
        user.setEmail("user5@test.com");
        user.setPasswordHash("hashed_old");
        Role role = new Role();
        role.setName("CLUB_MANAGER");
        user.setRole(role);

        when(userRepository.findById(5L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("OldPassword@123", "hashed_old")).thenReturn(true);
        when(passwordEncoder.matches("NewPassword@456", "hashed_old")).thenReturn(false);
        when(passwordEncoder.encode("NewPassword@456")).thenReturn("hashed_new");
        when(userRepository.saveAndFlush(user)).thenReturn(user);
        when(jwtUtil.generateToken(5L, "user5@test.com", "CLUB_MANAGER")).thenReturn("new_access_token");

        ChangePasswordRequest request = new ChangePasswordRequest("OldPassword@123", "NewPassword@456");
        AuthService.LoginResult result = authService.changePassword(5L, request);

        assertNotNull(result);
        assertEquals("new_access_token", result.token());
        assertNotNull(result.refreshToken());
        assertEquals("hashed_new", user.getPasswordHash());
        verify(userRepository).saveAndFlush(user);
        verify(refreshTokenRepository).revokeAllActiveTokensByUser(eq(user), any(LocalDateTime.class));
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void testChangePassword_WrongCurrentPassword_Throws() {
        User user = new User();
        user.setId(5L);
        user.setPasswordHash("hashed_old");

        when(userRepository.findById(5L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("WrongPassword@123", "hashed_old")).thenReturn(false);

        ChangePasswordRequest request = new ChangePasswordRequest("WrongPassword@123", "NewPassword@456");
        ApiException ex = assertThrows(ApiException.class, () -> authService.changePassword(5L, request));
        assertTrue(ex.getMessage().contains("Current password is incorrect"));
    }

    @Test
    void testChangePassword_SamePassword_Throws() {
        User user = new User();
        user.setId(5L);
        user.setPasswordHash("hashed_old");

        when(userRepository.findById(5L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("SamePassword@123", "hashed_old")).thenReturn(true);

        ChangePasswordRequest request = new ChangePasswordRequest("SamePassword@123", "SamePassword@123");
        ApiException ex = assertThrows(ApiException.class, () -> authService.changePassword(5L, request));
        assertTrue(ex.getMessage().contains("different from current password"));
    }

    @Test
    void testChangePassword_TooShort_Throws() {
        User user = new User();
        user.setId(5L);
        user.setPasswordHash("hashed_old");

        when(userRepository.findById(5L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("OldPassword@123", "hashed_old")).thenReturn(true);

        ChangePasswordRequest request = new ChangePasswordRequest("OldPassword@123", "short");
        ApiException ex = assertThrows(ApiException.class, () -> authService.changePassword(5L, request));
        assertTrue(ex.getMessage().contains("8-72 characters"));
    }
}
