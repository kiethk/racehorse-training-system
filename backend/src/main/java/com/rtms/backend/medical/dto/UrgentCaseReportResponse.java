package com.rtms.backend.medical.dto;

import com.rtms.backend.incident.enums.IncidentType;

import java.time.LocalDateTime;

/** Một báo cáo sự cố đã gộp vào ca khẩn cấp (kể cả báo cáo nguồn của ca). */
public record UrgentCaseReportResponse(
        Long id,
        Long reportedById,
        String reportedByName,
        String reportedByRole,
        LocalDateTime reportedAt,
        IncidentType incidentType,
        String title,
        String description,
        String imageUrl
) {
}
