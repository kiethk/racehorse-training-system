package com.rtms.backend.admission.dto;

/**
 * Trainer hoàn thành đánh giá trong luồng nhập học: chỉ một nhận xét bằng chữ.
 * Không có approve/reject — nộp xong là đơn tự sang MANAGER_REVIEW.
 * (The Trainer's review of an admission is a single written feedback. No approve/reject: handing it in moves the admission to MANAGER_REVIEW.)
 */
public record TrainerAdmissionReviewRequest(String trainerFeedback) {
}
