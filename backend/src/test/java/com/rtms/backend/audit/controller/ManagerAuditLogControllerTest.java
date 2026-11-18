package com.rtms.backend.audit.controller;

import com.rtms.backend.audit.service.AuditLogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ManagerAuditLogControllerTest {

    private AuditLogService auditLogService;
    private ManagerAuditLogController controller;

    @BeforeEach
    void setUp() {
        auditLogService = mock(AuditLogService.class);
        controller = new ManagerAuditLogController(auditLogService);
    }

    @Test
    void getAuditLogs_singleDayFilter_expandsToFullDayCoverage() {
        when(auditLogService.getAuditLogs(any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(Page.empty());

        Pageable pageable = PageRequest.of(0, 20);
        controller.getAuditLogs(null, null, "200", "2026-11-15", "2026-11-15", null, null, null, pageable);

        ArgumentCaptor<LocalDateTime> fromCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> toCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<Integer> statusCaptor = ArgumentCaptor.forClass(Integer.class);

        verify(auditLogService).getAuditLogs(
                isNull(), isNull(), statusCaptor.capture(),
                fromCaptor.capture(), toCaptor.capture(),
                isNull(), isNull(), isNull(), eq(pageable)
        );

        assertEquals(200, statusCaptor.getValue());
        assertNotNull(fromCaptor.getValue());
        assertNotNull(toCaptor.getValue());

        assertEquals(LocalDateTime.of(2026, 11, 15, 0, 0, 0), fromCaptor.getValue());
        assertEquals(2026, toCaptor.getValue().getYear());
        assertEquals(11, toCaptor.getValue().getMonthValue());
        assertEquals(15, toCaptor.getValue().getDayOfMonth());
        assertEquals(23, toCaptor.getValue().getHour());
        assertEquals(59, toCaptor.getValue().getMinute());
        assertEquals(59, toCaptor.getValue().getSecond());
    }

    @Test
    void getAuditLogs_statusStrings_parseCorrectly() {
        when(auditLogService.getAuditLogs(any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(Page.empty());

        Pageable pageable = PageRequest.of(0, 20);
        controller.getAuditLogs(null, null, "FAILED", null, null, null, null, null, pageable);

        ArgumentCaptor<Integer> statusCaptor = ArgumentCaptor.forClass(Integer.class);
        verify(auditLogService).getAuditLogs(
                isNull(), isNull(), statusCaptor.capture(),
                isNull(), isNull(), isNull(), isNull(), isNull(), eq(pageable)
        );
        assertEquals(400, statusCaptor.getValue());
    }
}
