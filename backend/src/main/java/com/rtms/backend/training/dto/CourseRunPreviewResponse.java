package com.rtms.backend.training.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * Hậu quả của việc giao khóa TRƯỚC khi bấm tạo: khóa chạy từ ngày nào đến ngày nào, giờ từng buổi, ngày nào hết khe và kế hoạch
 * nào đang chồng. Không ghi gì vào DB.
 * (The outcome of assigning a course BEFORE it is created: from which date to which, the time of each Session, which days have no
 * slot and which course runs overlap. Nothing is written.)
 */
public record CourseRunPreviewResponse(LocalDate startDate,
                                       LocalDate endDate,
                                       int memberCount,
                                       List<PlannedSessionResponse> sessions,
                                       List<LocalDate> fullDates,
                                       List<CourseRunOverlapResponse> overlaps,
                                       LocalDate earliestFreeStart,
                                       boolean canCreate) {
}
