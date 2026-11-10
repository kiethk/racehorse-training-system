package com.rtms.backend.training.dto;

import java.time.LocalDate;
import java.time.LocalTime;

/** Một buổi của Course Run trong màn chi tiết. (One Session of a Course Run in the detail view.) */
public record CourseRunSessionResponse(Long id,
                                  LocalDate date,
                                  LocalTime startTime,
                                  LocalTime endTime,
                                  String exerciseName,
                                  String status,
                                  int horseCount) {
}
