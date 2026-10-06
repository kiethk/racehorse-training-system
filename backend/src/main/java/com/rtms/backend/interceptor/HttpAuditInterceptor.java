package com.rtms.backend.interceptor;
import com.rtms.backend.service.AuditLogService;
import com.rtms.backend.controller.AuthController;
import com.rtms.backend.security.AuthenticatedUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Set;

public class HttpAuditInterceptor implements HandlerInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(HttpAuditInterceptor.class);

    private static final Set<String> WRITE_METHODS = Set.of("POST", "PUT", "PATCH", "DELETE");

    /**
     * Auth endpoints that handle credentials — excluded from audit to avoid noise
     * and accidental future handling of sensitive data.
     * Based on actual routes in AuthController (@RequestMapping("/api/auth")).
     */
    private static final Set<String> EXCLUDED_PATHS = Set.of(
            "/api/auth/login",
            "/api/auth/refresh",
            "/api/auth/logout",
            "/api/auth/register"
    );

    private final AuditLogService auditLogService;

    public HttpAuditInterceptor(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        String method = request.getMethod();
        if (!isWriteMethod(method)) {
            return;
        }

        String path = request.getRequestURI();
        if (EXCLUDED_PATHS.contains(path)) {
            return;
        }

        Long userId = resolveUserId();
        int statusCode = response.getStatus();

        try {
            auditLogService.recordHttpRequest(userId, method, path, statusCode);
        } catch (Exception e) {
            // Audit must never disrupt the business response
            logger.error("Unexpected error in HttpAuditInterceptor for {} {}: {}", method, path, e.getMessage(), e);
        }
    }

    private boolean isWriteMethod(String method) {
        return WRITE_METHODS.contains(method);
    }

    private Long resolveUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof AuthenticatedUser authenticatedUser) {
            return authenticatedUser.getUserId();
        }
        return null;
    }
}
