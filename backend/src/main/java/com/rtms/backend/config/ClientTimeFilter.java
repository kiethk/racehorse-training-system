package com.rtms.backend.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ClientTimeFilter extends OncePerRequestFilter {

    private static final String CLIENT_TIME_HEADER = "X-Client-Time";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String clientTimeHeader = request.getHeader(CLIENT_TIME_HEADER);
        if (clientTimeHeader != null && !clientTimeHeader.isBlank()) {
            try {
                LocalDateTime ldt;
                if (clientTimeHeader.endsWith("Z") || clientTimeHeader.contains("+") || clientTimeHeader.lastIndexOf('-') > 10) {
                    Instant instant = Instant.parse(clientTimeHeader);
                    ldt = LocalDateTime.ofInstant(instant, ClientTimeContext.DEFAULT_ZONE);
                } else {
                    ldt = LocalDateTime.parse(clientTimeHeader);
                }
                ClientTimeContext.set(ldt);
            } catch (Exception ignored) {
                // Ignore parse errors, fallback to default clock
            }
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            ClientTimeContext.clear();
        }
    }
}
