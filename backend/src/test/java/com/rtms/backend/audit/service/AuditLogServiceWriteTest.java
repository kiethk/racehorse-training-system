package com.rtms.backend.audit.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rtms.backend.audit.entity.AuditLog;
import com.rtms.backend.audit.repository.AuditLogRepository;
import com.rtms.backend.identity.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuditLogServiceWriteTest {

    private AuditLogRepository auditLogRepository;
    private UserRepository userRepository;
    private AuditActivityResolver activityResolver;
    private ObjectMapper objectMapper;
    private AuditLogService auditLogService;

    @BeforeEach
    void setUp() {
        auditLogRepository = mock(AuditLogRepository.class);
        userRepository = mock(UserRepository.class);
        activityResolver = new AuditActivityResolver();
        objectMapper = new ObjectMapper();
        auditLogService = new AuditLogService(auditLogRepository, userRepository, activityResolver, objectMapper);
    }

    @Test
    void testRecordHttpRequestSuccess() {
        auditLogService.recordHttpRequest(1L, "DELETE", "/api/areas/12", 200);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());

        AuditLog saved = captor.getValue();
        assertEquals("Remove Barn Area", saved.getAction());
        assertEquals("Area", saved.getEntityName());
        assertEquals(12L, saved.getEntityId());
        assertEquals(200, saved.getStatusCode());
        assertEquals("Manager removed barn area #12", saved.getDetails());
    }

    @Test
    void testRecordHttpRequestFailureWithReason() throws Exception {
        auditLogService.recordHttpRequest(1L, "POST", "/api/accounts", 409, "Certification number already in use by another trainer");

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());

        AuditLog saved = captor.getValue();
        assertEquals("Create Account", saved.getAction());
        assertEquals("User", saved.getEntityName());
        assertEquals(409, saved.getStatusCode());

        JsonNode json = objectMapper.readTree(saved.getDetails());
        assertEquals("Manager created a new account (Attempt failed)", json.get("summary").asText());
        assertEquals("Certification number already in use by another trainer", json.get("reason").asText());
        assertEquals("STAFF", json.get("module").asText());
    }
}
