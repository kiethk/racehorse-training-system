package com.rtms.backend.training.dto;

import com.rtms.backend.training.enums.SessionStatus;

/** Số buổi của một Course Run theo trạng thái. (The number of Sessions of a Course Run per status.) */
public record CourseRunSessionCountRow(Long courseRunId, SessionStatus status, Long count) {
}
