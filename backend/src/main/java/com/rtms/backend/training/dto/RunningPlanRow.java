package com.rtms.backend.training.dto;

import com.rtms.backend.training.enums.TrainingPlanStatus;

import java.time.LocalDate;

/**
 * Một kế hoạch đang chạy hoặc sắp chạy (ACTIVE / UPCOMING) của một chiến mã, kèm tên khóa. Dùng để kiểm trùng lịch
 * (BR-01), để tìm ngày rảnh sớm nhất, và để hiển thị "ngựa đang bận vì khóa nào".
 * (A running or upcoming (ACTIVE / UPCOMING) plan of a horse, with the course name. Used for the BR-01 overlap check,
 * to find the earliest free date, and to show "which course keeps the horse busy".)
 */
public record RunningPlanRow(Long planId,
                             Long horseId,
                             String courseName,
                             LocalDate startDate,
                             LocalDate endDate,
                             TrainingPlanStatus status) {

    public boolean overlaps(LocalDate otherStart, LocalDate otherEnd) {
        return !startDate.isAfter(otherEnd) && !endDate.isBefore(otherStart);
    }
}
