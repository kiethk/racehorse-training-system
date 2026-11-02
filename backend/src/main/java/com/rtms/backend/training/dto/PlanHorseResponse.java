package com.rtms.backend.training.dto;

/**
 * Một chiến mã của kế hoạch kèm Attendance (đã học X trên Y buổi). (A horse of a plan with its Attendance, X of Y Sessions.)
 */
public record PlanHorseResponse(Long horseId, String horseName, int attended, int total) {
}
