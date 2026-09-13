/**
 * SecurityConfig — REFERENCE IMPLEMENTATION (dùng chung toàn bộ nhóm)
 *
 * Cấu hình Spring Security baseline cho giai đoạn skeleton. Hiện tại:
 *  - CSRF disabled (API stateless, không cần)
 *  - CORS cho phép localhost:3000 (frontend dev) — khi deploy production phải đổi sang domain thật
 *  - /swagger-ui/**, /v3/api-docs/**, /api/** đều permitAll — để team test API không cần auth
 *    → Khi implement JWT thật, thay permitAll bằng .authenticated() hoặc .hasRole(...)
 *
 * ĐÃ FIX: Lỗi CORS khi FE (localhost:3000) gọi BE (localhost:8080) —
 *   phải dùng CorsConfigurationSource @Bean + cors.configurationSource(...),
 *   KHÔNG dùng @CrossOrigin trên từng controller (không hoạt động khi có SecurityFilterChain).
 *
 * ĐÃ FIX: Swagger UI redirect về /login do Security mặc định —
 *   phải requestMatcher /swagger-ui/** và /v3/api-docs/** trước .anyRequest().authenticated().
 */
package com.rtms.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**",
                                "/api/**")
                        .permitAll()
                        .anyRequest().authenticated());
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:3000"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}