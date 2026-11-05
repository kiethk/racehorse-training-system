package com.rtms.backend.training.dto;

import java.util.List;

/** Chi tiết một buổi: thông tin buổi và các chiến mã người xem được phép thấy. (A Session detail: the Session and the horses the viewer may see.) */
public record SessionDetailResponse(ScheduleSessionResponse session, List<SessionHorseResponse> horses) {
}
