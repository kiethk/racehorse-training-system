package com.rtms.backend.config;

import java.time.Duration;
import java.time.LocalTime;

/**
 * Chính sách thời gian vận hành trong ngày của trang trại.
 *
 * MỌI mốc giờ nghiệp vụ khai báo ở đây — không rải magic number trong service.
 * Xem thêm: enums/SopSlot.java (5 việc thường nhật của Groom) — PLAN_02.
 *
 * Vì sao là hằng số chứ không phải application.properties:
 *   - Đây là quy tắc SINH LÝ của loài ngựa, không phải cấu hình triển khai;
 *     nó không đổi giữa các môi trường dev/staging/prod.
 *   - Unit test dùng được mà không cần Spring context.
 *   - Gõ sai tên là lỗi biên dịch, không phải null lúc chạy.
 *
 * Ngược lại, max_capacity của lot ĐƯỢC lưu trong DB vì nó phụ thuộc bề rộng
 * đường chạy và có thể khác nhau giữa các lot.
 */
public final class FarmSchedulePolicy {

    private FarmSchedulePolicy() {
        // lớp tiện ích, không cho khởi tạo
    }

    // ---------------------------------------------------------------
    // BR-03 — KHUNG GIỜ VÀNG HUẤN LUYỆN
    //
    // 06:00 - 10:00 = 240 phút.
    // Vì sao 240 mà không phải 180 như thiết kế ban đầu:
    //   Với 3 nhóm ngựa lệch pha, tổ hợp bài tập nặng nhất là
    //   90 + 60 + 60 = 210 phút. Khung 180 phút chỉ nhét vừa 1/4 tổ hợp
    //   (75% số ngày sẽ báo "hết khe"). Khung 240 nhét vừa cả 4 tổ hợp,
    //   tổ hợp chật nhất vẫn dư 30 phút.
    //
    // Ba lập luận sinh lý vẫn giữ nguyên:
    //   - Tiêu hoá: ăn 05:00 -> tập 06:00 = đúng 60 phút giãn cách
    //   - Sốc nhiệt: lot cuối kết thúc 10:00, phần lớn khối lượng trước 09:00
    //   - Mặt sân: 06:00 độ ẩm còn tốt hơn 06:30
    // ---------------------------------------------------------------
    public static final LocalTime GOLDEN_HOURS_START = LocalTime.of(6, 0);
    public static final LocalTime GOLDEN_HOURS_END   = LocalTime.of(10, 0);

    // ---------------------------------------------------------------
    // KHUNG GIỜ THÚ Y ĐỊNH KỲ
    //
    // CHỈ dùng để sắp xếp hiển thị trong màn hình Today Tasks của Groom,
    // KHÔNG dùng để kiểm tra xung đột. Lý do: preventive_care_schedules chỉ
    // có scheduled_date (không có giờ), nên cần một mốc để chèn nó đúng vị
    // trí trên dòng thời gian. Việc tách hẳn khung sáng/chiều khiến xung đột
    // Vet <-> Lot không thể xảy ra, nên không cần code kiểm tra.
    // ---------------------------------------------------------------
    public static final LocalTime VET_WINDOW_START = LocalTime.of(13, 30);
    public static final LocalTime VET_WINDOW_END   = LocalTime.of(15, 30);

    /** BR-10 — sức chứa mặc định khi hệ thống tự mở lot mới. */
    public static final int DEFAULT_LOT_CAPACITY = 6;

    /** BR-06 — số chuồng tối đa một Groom được phụ trách. */
    public static final int MAX_STALLS_PER_GROOM = 3;

    /** Tổng số phút của khung giờ vàng (240). */
    public static int goldenWindowMinutes() {
        return (int) Duration.between(GOLDEN_HOURS_START, GOLDEN_HOURS_END).toMinutes();
    }

    /** BR-03 — khoảng [start, end] có nằm trọn trong khung giờ vàng không. */
    public static boolean isWithinGoldenHours(LocalTime start, LocalTime end) {
        return !start.isBefore(GOLDEN_HOURS_START) && !end.isAfter(GOLDEN_HOURS_END);
    }
}
