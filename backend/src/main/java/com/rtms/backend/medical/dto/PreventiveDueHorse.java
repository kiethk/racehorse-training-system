package com.rtms.backend.medical.dto;

import com.rtms.backend.horse.enums.HorseStatus;
import com.rtms.backend.medical.enums.PreventiveDueStatus;

import java.time.LocalDate;
import java.util.List;

/**
 * Một dòng của tab "To do": một ngựa cùng các phác đồ khớp bộ lọc.
 * earliestDueOn null khi phác đồ khẩn nhất chưa từng có dữ liệu (MISSING).
 */
public record PreventiveDueHorse(Long horseId, String horseName, HorseStatus horseStatus,
        List<PreventiveProtocolStatus> protocols, LocalDate earliestDueOn, PreventiveDueStatus worstStatus) {
}
