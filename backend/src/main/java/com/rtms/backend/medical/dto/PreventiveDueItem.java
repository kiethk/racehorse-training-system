package com.rtms.backend.medical.dto;

import com.rtms.backend.horse.enums.HorseStatus;

/** Một dòng trong tab "Việc cần làm": một (ngựa, phác đồ) cùng trạng thái tiêm. */
public record PreventiveDueItem(Long horseId, String horseName, HorseStatus horseStatus,
        PreventiveProtocolStatus protocol) {
}
