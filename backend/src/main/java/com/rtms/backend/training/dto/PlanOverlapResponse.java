package com.rtms.backend.training.dto;

import java.time.LocalDate;

/** Một kế hoạch chưa đóng của nhóm đang chồng ngày với khóa mới. (An unfinished plan of the group that overlaps the new course.) */
public record PlanOverlapResponse(Long planId,
                                  String courseName,
                                  LocalDate startDate,
                                  LocalDate endDate,
                                  String status) {
}
