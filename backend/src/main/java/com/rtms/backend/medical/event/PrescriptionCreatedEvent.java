package com.rtms.backend.medical.event;

import java.util.List;

/**
 * Phát sau khi ca khám complete commit thành công (B7/C6): danh sách prescription id vừa tạo.
 * Listener AFTER_COMMIT tiêu thụ — notification Groom/Trainer nối ở Track 2a.
 */
public record PrescriptionCreatedEvent(Long scheduleId, Long healthRecordId, Long horseId, List<Long> prescriptionIds) {
}
