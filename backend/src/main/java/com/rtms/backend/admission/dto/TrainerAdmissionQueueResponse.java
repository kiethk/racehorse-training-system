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
 * @param pending          đơn đang ở bước TRAINER_REVIEW và thuộc Trainer này
 * @param reviewed         đơn Trainer này đã đánh giá, mọi trạng thái về sau
 * @param activeHorseCount số ngựa Trainer đang lo (trong khu + đơn đang xử lý)
 * @param horseLimit       giới hạn tải; đạt mức này thì không được giao đơn mới
 * @param atCapacity       activeHorseCount >= horseLimit
 */
public record TrainerAdmissionQueueResponse(
        List<AdmissionSummaryResponse> pending,
        List<AdmissionSummaryResponse> reviewed,
        long activeHorseCount,
        int horseLimit,
        boolean atCapacity) {

    public TrainerAdmissionQueueResponse(List<AdmissionSummaryResponse> pending,
                                         List<AdmissionSummaryResponse> reviewed) {
        this(pending, reviewed, 0, 0, false);
    }

    public TrainerAdmissionQueueResponse withCapacity(long activeHorseCount, int horseLimit) {
        return new TrainerAdmissionQueueResponse(pending, reviewed, activeHorseCount, horseLimit,
                activeHorseCount >= horseLimit);
    }
}
