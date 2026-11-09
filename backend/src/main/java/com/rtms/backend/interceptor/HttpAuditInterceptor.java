package com.rtms.backend.interceptor;

import com.rtms.backend.security.AuthenticatedUser;
import com.rtms.backend.audit.service.AuditLogService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.List;
import java.util.Set;

public class HttpAuditInterceptor implements HandlerInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(HttpAuditInterceptor.class);

    private static final Set<String> WRITE_METHODS = Set.of("POST", "PUT", "PATCH", "DELETE");

    /**
     * Endpoints excluded from audit logging to avoid noise, transient processing, and personal UI state:
     * - Authentication credential handshakes (/api/auth/...)
     * - Personal notification reading (/api/notifications/...)
     * - AI conversation streaming and chat state (/api/v1/ai/chat/..., /api/v1/ai/conversations/...)
     * - Transient multipart OCR scanning (/api/owner/admissions/ocr-scan)
     * - Course run preview simulation (/api/course-runs/preview; the old /api/group-plans/preview stays excluded)
     */
    private static final List<String> EXCLUDED_PREFIXES = List.of(
            "/api/auth/login",
            "/api/auth/refresh",
            "/api/auth/logout",
            "/api/auth/register",
            "/api/notifications",
            "/api/v1/ai/chat",
            "/api/v1/ai/conversations",
            "/api/owner/admissions/ocr-scan",
            "/api/course-runs/preview",
            "/api/group-plans/preview"
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
        if (isExcluded(path)) {
            return;
        }

        // Tier-1 deduplication: skip HTTP interceptor logging if a Tier-2 domain audit was recorded
        if (Boolean.TRUE.equals(request.getAttribute("DOMAIN_AUDIT_LOGGED"))) {
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

    private boolean isExcluded(String path) {
        if (path == null) {
            return true;
        }
        for (String prefix : EXCLUDED_PREFIXES) {
            if (path.equals(prefix) || path.startsWith(prefix + "/") || path.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    private Long resolveUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof AuthenticatedUser authenticatedUser) {
            return authenticatedUser.getUserId();
        }
        return null;
    }
}
