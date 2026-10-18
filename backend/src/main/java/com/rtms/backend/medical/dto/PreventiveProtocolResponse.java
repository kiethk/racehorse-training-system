package com.rtms.backend.medical.dto;

import com.rtms.backend.medical.entity.PreventiveProtocol;
import com.rtms.backend.medical.enums.ProtocolCategory;

public record PreventiveProtocolResponse(Long id, String code, String name, ProtocolCategory category,
        int intervalDays, int warningDays, boolean mandatory, String defaultDosage, String defaultRoute) {
    public static PreventiveProtocolResponse from(PreventiveProtocol protocol) {
        return new PreventiveProtocolResponse(protocol.getId(), protocol.getCode(), protocol.getName(),
                protocol.getCategory(), protocol.getIntervalDays(), protocol.getWarningDays(), protocol.isMandatory(),
                protocol.getDefaultDosage(), protocol.getDefaultRoute());
    }
}
