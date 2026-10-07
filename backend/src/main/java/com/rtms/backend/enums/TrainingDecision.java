package com.rtms.backend.enums;

/**
 * Ngựa có được tập hay không — MỘT kiểu duy nhất dùng cho cả trạng thái hiện
 * hành của ngựa (Horse.trainingDecision) lẫn kết luận của từng lần khám
 * (HealthRecord.trainingDecision).
 *
 * Không có mức "tập hạn chế": thương nhẹ là BLOCKED kèm một lịch khám lại gần
 * (nextSchedule). Mức độ nặng nhẹ thể hiện ở thời hạn nghỉ, không ở số trạng thái.
 */
public enum TrainingDecision {
    ALLOWED,
    BLOCKED
}
