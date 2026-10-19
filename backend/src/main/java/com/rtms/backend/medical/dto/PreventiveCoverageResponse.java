package com.rtms.backend.medical.dto;

/** Tab "Theo vaccine": độ phủ của một phác đồ trên toàn bộ ngựa CANDIDATE + ELIGIBLE. */
public record PreventiveCoverageResponse(Long protocolId, String code, String name, boolean mandatory,
        int totalHorses, int ok, int dueSoon, int overdue, int missing, int none) {
}
