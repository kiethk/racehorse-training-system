package com.rtms.backend.training.dto;

import java.time.LocalDate;

/**
 * Một Group Plan trong danh sách, kèm số buổi để tính tiến độ. (A Group Plan in a list, with the Session counts that give its progress.)
 */
public record GroupPlanSummaryResponse(Long id,
                                       Long groupId,
                                       String groupName,
                                       boolean groupDisbanded,
                                       Long courseId,
                                       String courseName,
                                       LocalDate startDate,
                                       LocalDate endDate,
                                       String status,
                                       int sessionsHeld,
                                       int sessionsCancelled,
                                       int sessionsTotal) {
}
