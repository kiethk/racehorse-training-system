package com.rtms.backend.training.dto;

import java.time.LocalDate;

/**
 * Một dòng lịch sử của một chiến mã: một Group Plan nó đã tham gia, đã học mấy trên mấy buổi (Attendance) và vào từ ngày nào.
 * (One line of a horse's history: a Group Plan it took part in, how many Sessions it attended out of how many (Attendance), and
 * since when.)
 */
public record HorseAttendanceResponse(Long planId,
                                      String groupName,
                                      boolean groupDisbanded,
                                      String courseName,
                                      String planStatus,
                                      LocalDate startDate,
                                      LocalDate endDate,
                                      int attended,
                                      int total,
                                      LocalDate joinedOn) {
}
