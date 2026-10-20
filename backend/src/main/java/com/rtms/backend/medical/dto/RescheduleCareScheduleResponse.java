package com.rtms.backend.medical.dto;

import java.util.List;

/** warnings: cảnh báo trung tính (ví dụ ngày mới vượt hạn tiêm). Chỉ để hiển thị, không chặn. */
public record RescheduleCareScheduleResponse(CareScheduleResponse schedule, List<String> warnings) {
}
