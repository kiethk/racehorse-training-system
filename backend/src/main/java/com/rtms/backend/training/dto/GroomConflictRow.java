package com.rtms.backend.training.dto;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Một buổi vướng BR-09 khi đổi Groom: Groom mới đã dắt một con ngựa khác trong buổi đó.
 * (A Session violating BR-09 on a Groom change: the new Groom already leads another horse there.)
 */
public record GroomConflictRow(Long sessionId, LocalDate sessionDate, LocalTime startTime, String otherHorseName) {
}
