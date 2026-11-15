package com.rtms.backend.training.dto;

import java.time.LocalDate;

/**
 * Một Course Run trong danh sách, kèm số buổi để tính tiến độ. (A Course Run in a list, with the Session counts that give its progress.)
 */
public record CourseRunSummaryResponse(Long id,
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
