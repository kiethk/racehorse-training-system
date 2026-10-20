package com.rtms.backend.medical.dto;

import com.rtms.backend.medical.enums.PreventiveDueStatus;
import com.rtms.backend.medical.enums.ProtocolCategory;
import java.time.LocalDate;

/** lastDoseOn / nextDueOn là null khi ngựa chưa từng tiêm phác đồ này (MISSING hoặc NONE). */
public record PreventiveProtocolStatus(Long horseId, Long protocolId, String code, String name,
        ProtocolCategory category, boolean mandatory, LocalDate lastDoseOn, LocalDate nextDueOn,
        PreventiveDueStatus status) {
}
