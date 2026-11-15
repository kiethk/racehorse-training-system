package com.rtms.backend.training.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;

/** Giờ bắt đầu mới của một buổi; giờ kết thúc giữ nguyên độ dài bài. (The new start of a Session; the end keeps the Exercise length.) */
public record RescheduleSessionRequest(@NotNull LocalTime newStartTime) {
}
