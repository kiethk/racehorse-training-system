package com.rtms.backend.training.dto;

import com.rtms.backend.training.enums.PlanAvailability;

import java.time.LocalDate;
import java.util.List;

/**
 * Tình trạng của một chiến mã đối với kế hoạch đang xem trước.
 * (The status of one horse for the plan being previewed.)
 *
 * @param reason        câu giải thích hiển thị thẳng cho Trainer; null khi AVAILABLE (the explanation shown to the trainer; null when AVAILABLE)
 * @param conflicts     các kế hoạch chồng lên khoảng ngày đang xem (the plans overlapping the previewed range)
 * @param earliestStart ngày sớm nhất con này rảnh đủ trọn khóa; chỉ có khi BUSY (the earliest date this horse is free for the whole course; only when BUSY)
 */
public record HorsePlanAvailability(Long horseId,
                                    String horseName,
                                    String breed,
                                    String stallCode,
                                    PlanAvailability availability,
                                    String reason,
                                    List<RunningPlanRow> conflicts,
                                    LocalDate earliestStart) {
}
