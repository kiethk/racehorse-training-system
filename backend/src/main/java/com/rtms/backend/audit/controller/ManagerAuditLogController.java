package com.rtms.backend.audit.controller;

import com.rtms.backend.audit.dto.AuditArchiveStatusResponse;
import com.rtms.backend.audit.dto.AuditLogResponse;
import com.rtms.backend.audit.dto.AuditPurgeRequest;
import com.rtms.backend.audit.dto.AuditPurgeResponse;
import com.rtms.backend.audit.service.AuditLogService;
import com.rtms.backend.common.dto.ApiResponse;
import com.rtms.backend.security.AuthenticatedUser;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/api/manager/audit-logs")
public class ManagerAuditLogController {

    private final AuditLogService auditLogService;

    public ManagerAuditLogController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('AUDIT_LOG_VIEW')")
    public ApiResponse<Page<AuditLogResponse>> getAuditLogs(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String httpMethod,
            @RequestParam(required = false) String statusCode,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String role,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        Integer parsedStatusCode = parseStatusCode(statusCode);
        LocalDateTime fromDateTime = parseFromDate(from);
        LocalDateTime toDateTime = parseToDate(to);
        return ApiResponse.success(
                auditLogService.getAuditLogs(userId, httpMethod, parsedStatusCode, fromDateTime, toDateTime, search, module, role, pageable)
        );
    }

    private Integer parseStatusCode(String statusCode) {
        if (statusCode == null || statusCode.isBlank() || "ALL".equalsIgnoreCase(statusCode.trim())) {
            return null;
        }
        String trimmed = statusCode.trim().toUpperCase();
        if ("SUCCESS".equals(trimmed)) {
            return 200;
        }
        if ("FAILED".equals(trimmed) || "FAILURE".equals(trimmed)) {
            return 400;
        }
        try {
            return Integer.parseInt(trimmed);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private LocalDateTime parseFromDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) {
            return null;
        }
        String trimmed = dateStr.trim();
        if (trimmed.endsWith("Z") || trimmed.endsWith("z")) {
            try {
                return Instant.parse(trimmed).atZone(ZoneId.systemDefault()).toLocalDateTime();
            } catch (Exception ignored) {
                trimmed = trimmed.substring(0, trimmed.length() - 1);
            }
        }
        if (trimmed.contains("T")) {
            try {
                return LocalDateTime.parse(trimmed);
            } catch (Exception ignored) {}
        }
        try {
            LocalDate localDate = LocalDate.parse(trimmed.length() >= 10 ? trimmed.substring(0, 10) : trimmed);
            return localDate.atStartOfDay();
        } catch (Exception e) {
            return null;
        }
    }

    private LocalDateTime parseToDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) {
            return null;
        }
        String trimmed = dateStr.trim();
        if (trimmed.endsWith("Z") || trimmed.endsWith("z")) {
            try {
                return Instant.parse(trimmed).atZone(ZoneId.systemDefault()).toLocalDateTime();
            } catch (Exception ignored) {
                trimmed = trimmed.substring(0, trimmed.length() - 1);
            }
        }
        if (trimmed.contains("T")) {
            try {
                return LocalDateTime.parse(trimmed);
            } catch (Exception ignored) {}
        }
        try {
            LocalDate localDate = LocalDate.parse(trimmed.length() >= 10 ? trimmed.substring(0, 10) : trimmed);
            return localDate.atTime(LocalTime.MAX);
        } catch (Exception e) {
            return null;
        }
    }

    @GetMapping("/archive-status")
    @PreAuthorize("hasAuthority('AUDIT_LOG_VIEW')")
    public ApiResponse<AuditArchiveStatusResponse> getArchiveStatus() {
        return ApiResponse.success(auditLogService.getArchiveStatus());
    }

    @GetMapping("/export-archive")
    @PreAuthorize("hasAuthority('AUDIT_LOG_VIEW') or hasAuthority('AUDIT_LOG_MANAGE')")
    public ResponseEntity<byte[]> exportArchive(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(required = false) String before
    ) {
        LocalDateTime fromDateTime = parseFromDate(from);
        LocalDateTime toDateTime = parseToDate(to);
        LocalDateTime beforeDateTime = parseToDate(before);
        String csvContent = auditLogService.exportArchiveCsv(fromDateTime, toDateTime, beforeDateTime);
        byte[] csvBytes = csvContent.getBytes(StandardCharsets.UTF_8);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
        LocalDateTime effectiveEnd = toDateTime != null ? toDateTime : beforeDateTime;
        String filename;
        if (fromDateTime != null && effectiveEnd != null) {
            filename = "audit_logs_from_" + fromDateTime.format(formatter) + "_to_" + effectiveEnd.format(formatter) + ".csv";
        } else if (effectiveEnd != null) {
            filename = "audit_archive_before_" + effectiveEnd.format(formatter) + ".csv";
        } else if (fromDateTime != null) {
            filename = "audit_logs_from_" + fromDateTime.format(formatter) + ".csv";
        } else {
            filename = "audit_logs_all_" + LocalDate.now().format(formatter) + ".csv";
        }

        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(filename, StandardCharsets.UTF_8)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csvBytes);
    }

    @PostMapping("/purge")
    @PreAuthorize("hasAuthority('AUDIT_LOG_MANAGE')")
    public ApiResponse<AuditPurgeResponse> purgeAuditLogs(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody(required = false) AuditPurgeRequest request
    ) {
        Long actorUserId = user != null ? user.getUserId() : null;
        return ApiResponse.success(auditLogService.purgeAuditLogs(actorUserId, request));
    }
}
