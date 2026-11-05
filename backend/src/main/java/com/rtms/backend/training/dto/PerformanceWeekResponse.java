package com.rtms.backend.training.dto;

import java.time.LocalDate;

/**
 * Một tuần trên biểu đồ phong độ chung: mỗi con được tính MỘT lần trong tuần (trung bình các buổi của nó), rồi lấy
 * trung bình giữa các con, nên con tập nhiều buổi hơn không kéo lệch cả tuần.
 * (One week on the performance chart: each horse counts ONCE in the week (the average of its sessions), then the
 * horses are averaged, so a horse with more sessions does not skew the week.)
 *
 * @param weekStart           thứ Hai của tuần (the Monday of the week)
 * @param horses              số con có buổi tập hoàn thành trong tuần (horses with a completed session in the week)
 * @param sessions            tổng số buổi đã hoàn thành (completed sessions in total)
 * @param averageRating       điểm phong độ trung bình, thang 1–10 (average performance rating, 1-10)
 * @param averageSpeedKmh     tốc độ trung bình (average speed)
 * @param averageMaxHeartRate nhịp tim tối đa trung bình (average maximum heart rate)
 */
public record PerformanceWeekResponse(LocalDate weekStart,
                                      int horses,
                                      int sessions,
                                      Double averageRating,
                                      Double averageSpeedKmh,
                                      Double averageMaxHeartRate) {
}
