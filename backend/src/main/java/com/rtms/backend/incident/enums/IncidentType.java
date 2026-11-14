package com.rtms.backend.incident.enums;

/**
 * Phân loại sự cố ngựa: vấn đề thường (ROUTINE) hoặc chấn thương (URGENT).
 * Thay thế enum IncidentType (LOW/MEDIUM/HIGH/CRITICAL) — không dùng mức độ nữa.
 */
public enum IncidentType {
    /** Vấn đề thường — Vet kiểm tra ở slot Routine kế tiếp, ngựa vẫn tập. */
    PROBLEM,
    /** Chấn thương — Vet gọi ngay, chặn tập, tạo ca URGENT. */
    TRAUMA
}