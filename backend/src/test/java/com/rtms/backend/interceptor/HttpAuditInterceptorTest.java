package com.rtms.backend.interceptor;

import com.rtms.backend.security.AuthenticatedUser;
import com.rtms.backend.service.AuditLogService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;   
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.mockito.Mockito.*;

class HttpAuditInterceptorTest {

    private AuditLogService auditLogService;
    private HttpAuditInterceptor interceptor;
    private HttpServletRequest request;
    private HttpServletResponse response;

    @BeforeEach
    void setUp() {
        auditLogService = mock(AuditLogService.class);
        interceptor = new HttpAuditInterceptor(auditLogService);
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        SecurityContextHolder.clearContext();
    }

    // 1. GET → no audit
    @Test
    void givenGetRequest_thenNoAuditCall() throws Exception {
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/api/horses");
        when(response.getStatus()).thenReturn(200);

        interceptor.afterCompletion(request, response, null, null);

        verifyNoInteractions(auditLogService);
    }

    // 2. POST → audit called
    @Test
    void givenPostRequest_thenAuditCalled() throws Exception {
        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURI()).thenReturn("/api/horses");
        when(response.getStatus()).thenReturn(201);

        interceptor.afterCompletion(request, response, null, null);

        verify(auditLogService).recordHttpRequest(null, "POST", "/api/horses", 201);
    }

    // 3. PUT → audit called
    @Test
    void givenPutRequest_thenAuditCalled() throws Exception {
        when(request.getMethod()).thenReturn("PUT");
        when(request.getRequestURI()).thenReturn("/api/horses/1");
        when(response.getStatus()).thenReturn(200);

        interceptor.afterCompletion(request, response, null, null);

        verify(auditLogService).recordHttpRequest(null, "PUT", "/api/horses/1", 200);
    }

    // 4. PATCH → audit called
    @Test
    void givenPatchRequest_thenAuditCalled() throws Exception {
        when(request.getMethod()).thenReturn("PATCH");
        when(request.getRequestURI()).thenReturn("/api/manager/staff/15");
        when(response.getStatus()).thenReturn(200);

        interceptor.afterCompletion(request, response, null, null);

        verify(auditLogService).recordHttpRequest(null, "PATCH", "/api/manager/staff/15", 200);
    }

    // 5. DELETE → audit called
    @Test
    void givenDeleteRequest_thenAuditCalled() throws Exception {
        when(request.getMethod()).thenReturn("DELETE");
        when(request.getRequestURI()).thenReturn("/api/horses/1");
        when(response.getStatus()).thenReturn(204);

        interceptor.afterCompletion(request, response, null, null);

        verify(auditLogService).recordHttpRequest(null, "DELETE", "/api/horses/1", 204);
    }

    // 6. Authenticated principal → correct userId passed
    @Test
    void givenAuthenticatedUser_thenCorrectUserIdPassed() throws Exception {
        AuthenticatedUser principal = new AuthenticatedUser(42L, "user@example.com", "GROOM");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of())
        );

        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURI()).thenReturn("/api/groom/tasks");
        when(response.getStatus()).thenReturn(201);

        interceptor.afterCompletion(request, response, null, null);

        verify(auditLogService).recordHttpRequest(42L, "POST", "/api/groom/tasks", 201);
    }

    // 7. Unauthenticated principal → userId null
    @Test
    void givenNoAuthentication_thenUserIdNull() throws Exception {
        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURI()).thenReturn("/api/horses");
        when(response.getStatus()).thenReturn(401);

        interceptor.afterCompletion(request, response, null, null);

        verify(auditLogService).recordHttpRequest(null, "POST", "/api/horses", 401);
    }

    // 8. Response status 201 → 201 recorded
    @Test
    void givenStatus201_thenRecorded() throws Exception {
        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURI()).thenReturn("/api/admissions");
        when(response.getStatus()).thenReturn(201);

        interceptor.afterCompletion(request, response, null, null);

        verify(auditLogService).recordHttpRequest(null, "POST", "/api/admissions", 201);
    }

    // 9. Error status 400/403/500 → status recorded
    @Test
    void givenStatus403_thenRecorded() throws Exception {
        when(request.getMethod()).thenReturn("PATCH");
        when(request.getRequestURI()).thenReturn("/api/manager/staff/15");
        when(response.getStatus()).thenReturn(403);

        interceptor.afterCompletion(request, response, null, null);

        verify(auditLogService).recordHttpRequest(null, "PATCH", "/api/manager/staff/15", 403);
    }

    @Test
    void givenStatus500_thenRecorded() throws Exception {
        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURI()).thenReturn("/api/horses");
        when(response.getStatus()).thenReturn(500);

        interceptor.afterCompletion(request, response, null, null);

        verify(auditLogService).recordHttpRequest(null, "POST", "/api/horses", 500);
    }

    // 10. Excluded auth endpoint → no audit call
    @Test
    void givenLoginEndpoint_thenNoAuditCall() throws Exception {
        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURI()).thenReturn("/api/auth/login");
        when(response.getStatus()).thenReturn(200);

        interceptor.afterCompletion(request, response, null, null);

        verifyNoInteractions(auditLogService);
    }

    @Test
    void givenRefreshEndpoint_thenNoAuditCall() throws Exception {
        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURI()).thenReturn("/api/auth/refresh");
        when(response.getStatus()).thenReturn(200);

        interceptor.afterCompletion(request, response, null, null);

        verifyNoInteractions(auditLogService);
    }

    @Test
    void givenLogoutEndpoint_thenNoAuditCall() throws Exception {
        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURI()).thenReturn("/api/auth/logout");
        when(response.getStatus()).thenReturn(200);

        interceptor.afterCompletion(request, response, null, null);

        verifyNoInteractions(auditLogService);
    }

    // 11. AuditLogService throws → interceptor does not rethrow
    @Test
    void givenAuditServiceThrows_thenInterceptorDoesNotRethrow() throws Exception {
        doThrow(new RuntimeException("DB failure"))
                .when(auditLogService).recordHttpRequest(any(), any(), any(), anyInt());

        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURI()).thenReturn("/api/horses");
        when(response.getStatus()).thenReturn(201);

        // Must not throw
        interceptor.afterCompletion(request, response, null, null);
    }
}
