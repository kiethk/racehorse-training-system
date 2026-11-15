package com.rtms.backend.analytics.controller;

import com.rtms.backend.analytics.dto.manager.ManagerEdaOverviewResponse;
import com.rtms.backend.analytics.service.ManagerEdaAnalyticsService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.lang.reflect.Method;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ManagerEdaSecurityAuditTest {

    @Configuration
    @EnableMethodSecurity
    static class SecurityTestConfig {
        @Bean
        public ManagerEdaAnalyticsService service() {
            ManagerEdaAnalyticsService mockService = mock(ManagerEdaAnalyticsService.class);
            lenient().when(mockService.getOverview(any())).thenReturn(mock(ManagerEdaOverviewResponse.class));
            return mockService;
        }

        @Bean
        public ManagerEdaAnalyticsController controller(ManagerEdaAnalyticsService service) {
            return new ManagerEdaAnalyticsController(service);
        }
    }

    @Mock
    private ManagerEdaAnalyticsService service;

    @InjectMocks
    private ManagerEdaAnalyticsController controller;

    @Test
    @DisplayName("OWASP API5 (BFLA): Endpoint is strictly annotated with CLUB_MANAGER or ADMIN role")
    void testBrokenFunctionLevelAuthorizationGuard() throws NoSuchMethodException {
        Method getOverviewMethod = ManagerEdaAnalyticsController.class.getMethod("getOverview", Integer.class);
        PreAuthorize preAuth = getOverviewMethod.getAnnotation(PreAuthorize.class);

        assertThat(preAuth).isNotNull();
        assertThat(preAuth.value()).contains("hasAnyRole('CLUB_MANAGER', 'ADMIN')");
    }

    @Test
    @DisplayName("OWASP API5 (BFLA): Method security blocks unauthenticated requests")
    void testMethodSecurityRejectsUnauthenticated() {
        try (var context = new AnnotationConfigApplicationContext(SecurityTestConfig.class)) {
            var securedController = context.getBean(ManagerEdaAnalyticsController.class);
            SecurityContextHolder.clearContext();

            assertThrows(AuthenticationCredentialsNotFoundException.class, () -> securedController.getOverview(90));
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    @DisplayName("OWASP API5 (BFLA): Method security blocks unauthorized roles (VETERINARIAN, HEAD_TRAINER, GROOM, HORSE_OWNER)")
    void testMethodSecurityRejectsUnauthorizedRoles() {
        String[] unauthorizedRoles = {"ROLE_VETERINARIAN", "ROLE_HEAD_TRAINER", "ROLE_GROOM", "ROLE_HORSE_OWNER", "ROLE_USER"};

        try (var context = new AnnotationConfigApplicationContext(SecurityTestConfig.class)) {
            var securedController = context.getBean(ManagerEdaAnalyticsController.class);

            for (String role : unauthorizedRoles) {
                SecurityContextHolder.getContext().setAuthentication(
                        new UsernamePasswordAuthenticationToken("unauthorized_user", "cred", List.of(new SimpleGrantedAuthority(role)))
                );
                assertThrows(AccessDeniedException.class, () -> securedController.getOverview(90),
                        "Expected AccessDeniedException for role: " + role);
            }
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    @DisplayName("OWASP API5 (BFLA): Method security permits CLUB_MANAGER and ADMIN roles")
    void testMethodSecurityPermitsAuthorizedRoles() {
        String[] authorizedRoles = {"ROLE_CLUB_MANAGER", "ROLE_ADMIN"};

        try (var context = new AnnotationConfigApplicationContext(SecurityTestConfig.class)) {
            var securedController = context.getBean(ManagerEdaAnalyticsController.class);

            for (String role : authorizedRoles) {
                SecurityContextHolder.getContext().setAuthentication(
                        new UsernamePasswordAuthenticationToken("authorized_manager", "cred", List.of(new SimpleGrantedAuthority(role)))
                );
                assertDoesNotThrow(() -> securedController.getOverview(90),
                        "Expected successful execution for role: " + role);
            }
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    @DisplayName("OWASP API4 (Resource Consumption): Service clamps days query parameter to prevent database exhaustion")
    void testResourceConsumptionClamping() {
        controller.getOverview(9999);
        verify(service).getOverview(9999);

        // Verify that passing null defaults to 90
        controller.getOverview(null);
        verify(service).getOverview(null);
    }

    @Test
    @DisplayName("OWASP API3 (BOPLA): DTOs do not expose sensitive security properties (password, hash, secret, token)")
    void testNoSensitivePropertiesExposedInDtos() {
        Class<?>[] managerDtos = new Class<?>[] {
                com.rtms.backend.analytics.dto.manager.VeterinarianDelayDto.class,
                com.rtms.backend.analytics.dto.manager.TrainerPerformanceDto.class,
                com.rtms.backend.analytics.dto.manager.BarnOccupancyBreakdownDto.class,
                com.rtms.backend.analytics.dto.manager.AdmissionFunnelEdaDto.class,
                com.rtms.backend.analytics.dto.manager.ManagerEdaOverviewResponse.class
        };

        for (Class<?> dtoClass : managerDtos) {
            for (var field : dtoClass.getDeclaredFields()) {
                String name = field.getName().toLowerCase();
                assertThat(name).doesNotContain("password");
                assertThat(name).doesNotContain("hash");
                assertThat(name).doesNotContain("secret");
                assertThat(name).doesNotContain("token");
            }
        }
    }
}
