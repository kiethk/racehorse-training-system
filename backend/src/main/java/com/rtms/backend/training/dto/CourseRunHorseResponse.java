package com.rtms.backend.training.dto;

/**
 * Một chiến mã của kế hoạch kèm Attendance (đã học X trên Y buổi). (A horse of a course run with its Attendance, X of Y Sessions.)
 */
public record CourseRunHorseResponse(Long horseId, String horseName, int attended, int total) {
}
