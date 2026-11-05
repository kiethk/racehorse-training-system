package com.rtms.backend.medical.dto;

import com.rtms.backend.incident.enums.IncidentSeverity;

import java.time.LocalDateTime;

/** Một báo cáo sự cố đã gộp vào ca khẩn cấp (kể cả báo cáo nguồn của ca). */
public record UrgentCaseReportResponse(
        Long id,
        Long reportedById,
        String reportedByName,
        String reportedByRole,
        LocalDateTime reportedAt,
        IncidentSeverity severity,
        String title,
        String description,
        String imageUrl
) {
}
