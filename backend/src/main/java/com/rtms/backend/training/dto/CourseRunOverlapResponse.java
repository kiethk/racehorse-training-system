package com.rtms.backend.training.dto;

import java.time.LocalDate;

/** Một kế hoạch chưa đóng của nhóm đang chồng ngày với khóa mới. (An unfinished course run of the group that overlaps the new course.) */
public record CourseRunOverlapResponse(Long courseRunId,
                                  String courseName,
                                  LocalDate startDate,
                                  LocalDate endDate,
                                  String status) {
}
