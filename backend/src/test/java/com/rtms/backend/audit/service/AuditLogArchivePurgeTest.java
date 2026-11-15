package com.rtms.backend.audit.service;

import com.rtms.backend.audit.dto.AuditArchiveStatusResponse;
import com.rtms.backend.audit.dto.AuditPurgeRequest;
import com.rtms.backend.audit.dto.AuditPurgeResponse;
import com.rtms.backend.audit.entity.AuditLog;
import com.rtms.backend.audit.repository.AuditLogRepository;
import com.rtms.backend.identity.entity.Role;
import com.rtms.backend.identity.entity.User;
import com.rtms.backend.identity.repository.UserRepository;
import com.rtms.backend.config.ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuditLogArchivePurgeTest {

    private AuditLogRepository repository;
    private UserRepository userRepository;
    private AuditActivityResolver activityResolver;
    private AuditLogService service;

    @BeforeEach
    void setUp() {
        repository = mock(AuditLogRepository.class);
        userRepository = mock(UserRepository.class);
        activityResolver = new AuditActivityResolver();
        service = new AuditLogService(repository, userRepository, activityResolver);
    }

    @Test
    void getArchiveStatus_returnsAccurateAgedCountsAndMaintenanceFlag() {
        LocalDateTime oldestDate = LocalDateTime.now().minusDays(45);
        AuditLog oldestLog = new AuditLog();
        oldestLog.setCreatedAt(oldestDate);

        when(repository.count()).thenReturn(1500L);
        when(repository.countByCreatedAtBefore(any(LocalDateTime.class))).thenReturn(450L);
        when(repository.findTopByOrderByCreatedAtAsc()).thenReturn(Optional.of(oldestLog));

        AuditArchiveStatusResponse status = service.getArchiveStatus();

        assertEquals(1500L, status.totalCount());
        assertEquals(450L, status.agedCount());
        assertEquals(oldestDate, status.oldestLogDate());
        assertTrue(status.requiresMaintenance());
    }

    @Test
    void getArchiveStatus_requiresNoMaintenanceWhenUnderThresholds() {
        when(repository.count()).thenReturn(200L);
        when(repository.countByCreatedAtBefore(any(LocalDateTime.class))).thenReturn(0L);
        when(repository.findTopByOrderByCreatedAtAsc()).thenReturn(Optional.empty());

        AuditArchiveStatusResponse status = service.getArchiveStatus();

        assertEquals(200L, status.totalCount());
        assertEquals(0L, status.agedCount());
        assertFalse(status.requiresMaintenance());
    }

    @Test
    void exportArchiveCsv_producesProperCsvContent() {
        User manager = new User();
        manager.setFullName("John Doe");
        Role role = new Role();
        role.setName("CLUB_MANAGER");
        manager.setRole(role);

        AuditLog log1 = new AuditLog();
        log1.setCreatedAt(LocalDateTime.of(2026, 10, 1, 10, 0, 0));
        log1.setUser(manager);
        log1.setAction("UPDATE_HORSE_STATUS");
        log1.setEntityName("Horse");
        log1.setEntityId(42L);
        log1.setDetails("{\"summary\":\"Updated status to ACTIVE\"}");

        when(repository.findByCreatedAtBeforeOrderByCreatedAtAsc(any(LocalDateTime.class)))
                .thenReturn(List.of(log1));

        String csv = service.exportArchiveCsv(null, null, LocalDateTime.now().minusDays(30));

        assertNotNull(csv);
        assertTrue(csv.contains("\"Timestamp\",\"Actor\",\"Role\",\"Module\",\"Action\",\"Target Entity\",\"Target ID\",\"Details / Summary\""));
        assertTrue(csv.contains("\"John Doe\""));
        assertTrue(csv.contains("\"CLUB_MANAGER\""));
        assertTrue(csv.contains("\"Horse\""));
        assertTrue(csv.contains("\"42\""));
    }

    @Test
    void purgeAuditLogs_deletesLogsAndRecordsDomainAuditEvent() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(30);
        when(repository.deleteByCreatedAtBefore(any(LocalDateTime.class))).thenReturn(125);

        AuditPurgeRequest request = new AuditPurgeRequest(cutoff, null, "Routine archive and purge");
        AuditPurgeResponse response = service.purgeAuditLogs(10L, request);

        assertEquals(125, response.purgedCount());
        assertTrue(response.message().contains("125"));

        ArgumentCaptor<AuditLog> auditCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(repository).save(auditCaptor.capture());

        AuditLog domainEvent = auditCaptor.getValue();
        assertEquals("ARCHIVE_AND_PURGE_AUDIT_LOGS", domainEvent.getAction());
        assertEquals("AuditLog", domainEvent.getEntityName());
        assertTrue(domainEvent.getDetails().contains("Routine archive and purge"));
        assertTrue(domainEvent.getDetails().contains("125 records removed"));
    }

    @Test
    void exportArchiveCsv_withCustomRange_usesBetweenQuery() {
        LocalDateTime from = LocalDateTime.of(2026, 10, 1, 0, 0);
        LocalDateTime before = LocalDateTime.of(2026, 10, 5, 23, 59);

        AuditLog log1 = new AuditLog();
        log1.setCreatedAt(LocalDateTime.of(2026, 10, 2, 10, 0));
        log1.setAction("CREATE");
        log1.setEntityName("Horse");

        when(repository.findByCreatedAtBetweenOrderByCreatedAtAsc(from, before))
                .thenReturn(List.of(log1));

        String csv = service.exportArchiveCsv(from, null, before);
        assertNotNull(csv);
        assertTrue(csv.contains("Horse"));
    }

    @Test
    void purgeAuditLogs_withCustomRange_usesBetweenDelete() {
        LocalDateTime from = LocalDateTime.of(2026, 8, 1, 0, 0);
        LocalDateTime before = LocalDateTime.of(2026, 8, 5, 23, 59);

        when(repository.deleteByCreatedAtBetween(from, before)).thenReturn(25);

        AuditPurgeRequest request = new AuditPurgeRequest(before, from, "Custom purge");
        AuditPurgeResponse response = service.purgeAuditLogs(10L, request);

        assertEquals(25, response.purgedCount());
    }

    @Test
    void purgeAuditLogs_withinLast30Days_throwsRetentionViolation() {
        LocalDateTime recentDate = LocalDateTime.now().minusDays(5);
        AuditPurgeRequest request = new AuditPurgeRequest(recentDate, null, "Illegal purge");

        ApiException ex = assertThrows(ApiException.class, () ->
                service.purgeAuditLogs(10L, request)
        );

        assertEquals("PURGE_RETENTION_VIOLATION", ex.getErrorCode());
    }

    @Test
    void purgeAuditLogs_fromDateOnly_purgesUpToThirtyDaysAgoNotNow() {
        LocalDateTime from = LocalDateTime.now().minusDays(45);
        when(repository.deleteByCreatedAtBetween(any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(10);

        AuditPurgeRequest request = new AuditPurgeRequest(null, from, "Purge older");
        AuditPurgeResponse response = service.purgeAuditLogs(10L, request);

        assertEquals(10, response.purgedCount());
        ArgumentCaptor<LocalDateTime> captor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(repository).deleteByCreatedAtBetween(captor.capture(), captor.capture());
        assertEquals(from, captor.getAllValues().get(0));
        assertTrue(captor.getAllValues().get(1).isBefore(LocalDateTime.now().minusDays(29)));
    }

    @Test
    void exportArchiveCsv_throwsWhenNoLogsFound() {
        when(repository.findByCreatedAtBeforeOrderByCreatedAtAsc(any(LocalDateTime.class)))
                .thenReturn(List.of());

        ApiException ex = assertThrows(ApiException.class, () ->
                service.exportArchiveCsv(null, null, LocalDateTime.now().minusDays(30))
        );

        assertEquals("NO_LOGS_TO_EXPORT", ex.getErrorCode());
    }

    @Test
    void purgeAuditLogs_throwsWhenNoLogsPurged() {
        when(repository.deleteByCreatedAtBefore(any(LocalDateTime.class))).thenReturn(0);

        AuditPurgeRequest request = new AuditPurgeRequest(LocalDateTime.now().minusDays(30), null, "Routine purge");
        ApiException ex = assertThrows(ApiException.class, () ->
                service.purgeAuditLogs(10L, request)
        );

        assertEquals("NO_LOGS_TO_PURGE", ex.getErrorCode());
    }
}
