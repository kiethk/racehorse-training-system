package com.rtms.backend.admission.dto;

import java.util.List;

/**
 * Hàng chờ của Huấn luyện viên — hai nhóm trong MỘT lời gọi.
 *
 * Vì sao gộp thay vì hai endpoint: màn hình danh sách luôn hiện số đếm của cả
 * hai tab ("Chờ đánh giá (3)" / "Đã đánh giá (12)") ngay khi mở, nên tách ra
 * cũng vẫn phải gọi cả hai. Gộp lại thì số đếm luôn đồng bộ với nhau, không
 * có cảnh một tab đã cập nhật mà tab kia còn số cũ.
 *
 * @param pending  đơn đang ở bước TRAINER_REVIEW và thuộc Trainer này
 * @param reviewed đơn Trainer này đã đánh giá, mọi trạng thái về sau
 */
public record TrainerAdmissionQueueResponse(
        List<AdmissionSummaryResponse> pending,
        List<AdmissionSummaryResponse> reviewed) {
}
