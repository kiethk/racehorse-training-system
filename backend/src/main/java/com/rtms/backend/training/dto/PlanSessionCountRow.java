package com.rtms.backend.training.dto;

import com.rtms.backend.training.enums.SessionStatus;

/** Số buổi của một Group Plan theo trạng thái. (The number of Sessions of a Group Plan per status.) */
public record PlanSessionCountRow(Long planId, SessionStatus status, Long count) {
}
