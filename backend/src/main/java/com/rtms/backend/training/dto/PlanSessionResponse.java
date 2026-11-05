package com.rtms.backend.training.dto;

import java.time.LocalDate;
import java.time.LocalTime;

/** Một buổi của Group Plan trong màn chi tiết. (One Session of a Group Plan in the detail view.) */
public record PlanSessionResponse(Long id,
                                  LocalDate date,
                                  LocalTime startTime,
                                  LocalTime endTime,
                                  String subjectName,
                                  String status,
                                  int horseCount) {
}
