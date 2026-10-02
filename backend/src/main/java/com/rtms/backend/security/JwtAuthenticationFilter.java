package com.rtms.backend.security;

import com.rtms.backend.repository.RoleRepository;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final RoleRepository roleRepository;

    public JwtAuthenticationFilter(JwtUtil jwtUtil, RoleRepository roleRepository) {
        this.jwtUtil = jwtUtil;
        this.roleRepository = roleRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String token = extractTokenFromHeader(request);

        if (token != null && jwtUtil.isTokenValid(token)) {
            Claims claims = jwtUtil.extractClaims(token);
            String email = claims.getSubject();
            String role = claims.get("role", String.class);
            Long userId = claims.get("userId", Number.class).longValue();

            AuthenticatedUser authenticatedUser = new AuthenticatedUser(userId, email, role);

            List<GrantedAuthority> authorities = roleRepository.findByName(role)
                    .map(r -> {
                        List<GrantedAuthority> auths = r.getPermissions().stream()
                                .map(p -> (GrantedAuthority) new SimpleGrantedAuthority(p.getCode()))
                                .collect(Collectors.toList());
                        if (role != null) {
                            auths.add(new SimpleGrantedAuthority("ROLE_" + role));
                        }
                        return auths;
                    })
                    .orElseGet(() -> role != null ? List.of(new SimpleGrantedAuthority("ROLE_" + role)) : List.of());

            var authentication = new UsernamePasswordAuthenticationToken(authenticatedUser, null, authorities);
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }

    private String extractTokenFromHeader(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}