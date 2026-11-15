package com.rtms.backend.security;

import com.rtms.backend.identity.entity.Role;
import com.rtms.backend.identity.repository.RoleRepository;
import com.rtms.backend.identity.repository.UserRepository;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class JwtAuthenticationFilterTest {

    private JwtUtil jwtUtil;
    private RoleRepository roleRepository;
    private UserRepository userRepository;
    private JwtAuthenticationFilter filter;

    private HttpServletRequest request;
    private HttpServletResponse response;
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        jwtUtil = mock(JwtUtil.class);
        roleRepository = mock(RoleRepository.class);
        userRepository = mock(UserRepository.class);
        filter = new JwtAuthenticationFilter(jwtUtil, roleRepository, userRepository);

        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        filterChain = mock(FilterChain.class);

        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void doFilterInternal_validTokenAndActiveUser_authenticates() throws ServletException, IOException {
        String token = "valid-token";
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtUtil.isTokenValid(token)).thenReturn(true);

        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn("groom@rts.com");
        when(claims.get("role", String.class)).thenReturn("GROOM");
        when(claims.get("userId", Number.class)).thenReturn(10L);
        when(jwtUtil.extractClaims(token)).thenReturn(claims);

        when(userRepository.existsByIdAndIsActiveTrue(10L)).thenReturn(true);
        Role role = new Role();
        role.setName("GROOM");
        role.setPermissions(java.util.Set.of());
        when(roleRepository.findByName("GROOM")).thenReturn(Optional.of(role));

        filter.doFilterInternal(request, response, filterChain);

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals("groom@rts.com", ((AuthenticatedUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getEmail());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_validTokenButInactiveUser_doesNotAuthenticate() throws ServletException, IOException {
        String token = "valid-token";
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtUtil.isTokenValid(token)).thenReturn(true);

        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn("disabled@rts.com");
        when(claims.get("role", String.class)).thenReturn("GROOM");
        when(claims.get("userId", Number.class)).thenReturn(10L);
        when(jwtUtil.extractClaims(token)).thenReturn(claims);

        when(userRepository.existsByIdAndIsActiveTrue(10L)).thenReturn(false);

        filter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verifyNoInteractions(roleRepository);
        verify(filterChain).doFilter(request, response);
    }
}
