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
import org.springframework.format.annotation.DateTimeFormat;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/api/manager/audit-logs")
public class ManagerAuditLogController {

    private final AuditLogService auditLogService;

    public ManagerAuditLogController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @GetMapping
    @PreAuthorize("principal.role == 'CLUB_MANAGER'")
    public ApiResponse<Page<AuditLogResponse>> getAuditLogs(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String httpMethod,
            @RequestParam(required = false) Integer statusCode,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String role,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ApiResponse.success(
                auditLogService.getAuditLogs(userId, httpMethod, statusCode, from, to, search, module, role, pageable)
        );
    }

    @GetMapping("/archive-status")
    @PreAuthorize("principal.role == 'CLUB_MANAGER'")
    public ApiResponse<AuditArchiveStatusResponse> getArchiveStatus() {
        return ApiResponse.success(auditLogService.getArchiveStatus());
    }

    @GetMapping("/export-archive")
    @PreAuthorize("principal.role == 'CLUB_MANAGER'")
    public ResponseEntity<byte[]> exportArchive(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime before
    ) {
        String csvContent = auditLogService.exportArchiveCsv(from, to, before);
        byte[] csvBytes = csvContent.getBytes(StandardCharsets.UTF_8);

        String filename = "audit_archive_" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + ".csv";
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(filename, StandardCharsets.UTF_8)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csvBytes);
    }

    @PostMapping("/purge")
    @PreAuthorize("principal.role == 'CLUB_MANAGER'")
    public ApiResponse<AuditPurgeResponse> purgeAuditLogs(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody(required = false) AuditPurgeRequest request
    ) {
        Long actorUserId = user != null ? user.getUserId() : null;
        return ApiResponse.success(auditLogService.purgeAuditLogs(actorUserId, request));
    }
}
