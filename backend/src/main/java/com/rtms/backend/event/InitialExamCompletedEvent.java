package com.rtms.backend.event;

/**
 * Thú y vừa hoàn tất lần khám nhập học, đơn đã sang TRAINER_REVIEW.
 *
 * Phát ra BÊN TRONG transaction của Vet; việc gán Trainer lắng nghe ở pha
 * AFTER_COMMIT nên lỗi lúc gán không thể làm rollback kết quả khám.
 */
public record InitialExamCompletedEvent(Long admissionId) {}
