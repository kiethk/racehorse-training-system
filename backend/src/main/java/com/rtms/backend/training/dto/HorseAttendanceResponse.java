package com.rtms.backend.training.dto;

import java.time.LocalDate;

/**
 * Một dòng lịch sử của một chiến mã: một Course Run nó đã tham gia, đã học mấy trên mấy buổi (Attendance) và vào từ ngày nào.
 * (One line of a horse's history: a Course Run it took part in, how many Sessions it attended out of how many (Attendance), and
 * since when.)
 */
public record HorseAttendanceResponse(Long courseRunId,
                                      String groupName,
                                      boolean groupDisbanded,
                                      String courseName,
                                      String courseRunStatus,
                                      LocalDate startDate,
                                      LocalDate endDate,
                                      int attended,
                                      int total,
                                      LocalDate joinedOn) {
}
