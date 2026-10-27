package com.rtms.backend.training.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * Kết quả xem trước kế hoạch: khóa sẽ chạy từ ngày nào tới ngày nào, và từng chiến mã trong khu có ghi danh được không.
 * (The plan preview: from which date to which date the course runs, and whether each horse of the area can enroll.)
 *
 * @param firstSessionDate  ngày tập đầu tiên, tức ngày bắt đầu đã dời tới thứ tập gần nhất (the first training day,
 *                          i.e. the chosen start moved to the nearest training weekday)
 * @param groupEarliestStart ngày sớm nhất mọi ngựa đang chọn cùng rảnh trọn khóa; null khi chưa chọn con nào ghi danh
 *                          được (the earliest date every selected horse is free for the whole course; null when no
 *                          enrollable horse is selected)
 * @param scheduleFullDates  những buổi của khóa rơi vào ngày mà khung giờ vàng của Trainer chắc chắn không còn chỗ cho nhóm
 *                          đang chọn; rỗng nghĩa là chưa thấy ngày nào hết chỗ (the course sessions on days where the trainer
 *                          golden window certainly has no room for the selected group; empty means no full day was found)
 */
public record PlanPreviewResponse(LocalDate firstSessionDate,
                                  LocalDate endDate,
                                  int totalSessions,
                                  LocalDate groupEarliestStart,
                                  List<LocalDate> scheduleFullDates,
                                  List<HorsePlanAvailability> horses) {
}
