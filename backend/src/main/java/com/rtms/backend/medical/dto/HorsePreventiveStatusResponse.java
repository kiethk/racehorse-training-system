package com.rtms.backend.medical.dto;

import java.util.List;

/** Trạng thái tiêm của một ngựa và mốc tiêm phòng đang chờ (null nếu chưa có). */
public record HorsePreventiveStatusResponse(Long horseId, String horseName, List<PreventiveProtocolStatus> protocols,
        CareScheduleResponse nextMilestone) {
}
