package com.rtms.backend.medical.enums;

/**
 * Trạng thái tiêm của một (ngựa, phác đồ), tính từ mũi tiêm gần nhất, không lưu DB.
 * NONE: phác đồ không bắt buộc và ngựa chưa từng tiêm.
 */
public enum PreventiveDueStatus {
    OK,
    DUE_SOON,
    OVERDUE,
    MISSING,
    NONE
}
