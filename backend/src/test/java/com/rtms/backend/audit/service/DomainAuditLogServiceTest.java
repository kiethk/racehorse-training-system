package com.rtms.backend.audit.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rtms.backend.audit.dto.DomainAuditEvent;
import com.rtms.backend.audit.entity.AuditLog;
import com.rtms.backend.audit.repository.AuditLogRepository;
import com.rtms.backend.identity.entity.User;
import com.rtms.backend.identity.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DomainAuditLogServiceTest {

    private AuditLogRepository auditLogRepository;
    private UserRepository userRepository;
    private AuditActivityResolver activityResolver;
    private ObjectMapper objectMapper;
    private AuditLogService auditLogService;
    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        auditLogRepository = mock(AuditLogRepository.class);
        userRepository = mock(UserRepository.class);
        activityResolver = mock(AuditActivityResolver.class);
        objectMapper = new ObjectMapper();

        auditLogService = new AuditLogService(auditLogRepository, userRepository, activityResolver, objectMapper);

        request = new MockHttpServletRequest();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void recordDomainEvent_savesStructuredJsonDetailsAndMarksRequestContext() {
        User user = new User();
        user.setId(5L);
        when(userRepository.findById(5L)).thenReturn(Optional.of(user));

        DomainAuditEvent event = DomainAuditEvent.builder()
                .userId(5L)
                .action("VET_INTAKE_EXAM")
                .entityName("AdmissionApplication")
                .entityId(12L)
                .module("MEDICAL")
                .summary("Veterinarian completed intake exam and cleared horse for training")
                .reason("Clear lungs, sound gait, vaccinations up to date")
                .targetUrl("/manager/admissions/12")
                .before(Map.of("status", "WAITING_VET_REVIEW", "healthStatus", "PENDING"))
                .after(Map.of("status", "WAITING_TRAINER_REVIEW", "healthStatus", "CLEARED"))
                .build();

        auditLogService.recordDomainEvent(event);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());

        AuditLog saved = captor.getValue();
        assertThat(saved.getUser()).isEqualTo(user);
        assertThat(saved.getAction()).isEqualTo("VET_INTAKE_EXAM");
        assertThat(saved.getEntityName()).isEqualTo("AdmissionApplication");
        assertThat(saved.getEntityId()).isEqualTo(12L);
        assertThat(saved.getStatusCode()).isEqualTo(200);
        assertThat(saved.getHttpMethod()).isEqualTo("DOMAIN");
        assertThat(saved.getDetails()).contains("\"module\":\"MEDICAL\"");
        assertThat(saved.getDetails()).contains("\"reason\":\"Clear lungs, sound gait, vaccinations up to date\"");
        assertThat(saved.getDetails()).contains("\"before\":{");
        assertThat(saved.getDetails()).contains("\"after\":{");

        // Verify request context marker was set to prevent Tier-1 duplication
        assertThat(request.getAttribute("DOMAIN_AUDIT_LOGGED")).isEqualTo(Boolean.TRUE);
    }
}

