package com.rtms.backend.training.dto;

import com.rtms.backend.training.enums.SessionStatus;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Một buổi kèm tên bài tập, thời lượng và số ngựa còn hiệu lực (tính bằng SQL), dùng cho các màn đọc lịch như trợ lý AI.
 * (A Session with its Exercise name, duration and active-horse count, computed in SQL, for the screens that read the schedule
 * such as the AI assistant.)
 */
public record SessionRow(Long sessionId,
                         Long trainerId,
                         LocalDate sessionDate,
                         LocalTime startTime,
                         LocalTime endTime,
                         Long exerciseId,
                         String exerciseName,
                         Integer durationMinutes,
                         SessionStatus status,
                         Long occupied) {
}
