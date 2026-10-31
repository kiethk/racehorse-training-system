package com.rtms.backend.training.service;

/**
 * Đưa dữ liệu theo kịp ĐỒNG HỒ: buổi hết giờ thành HELD (cùng các Workout của nó), kế hoạch tới ngày bắt đầu thành ACTIVE, và kế hoạch
 * không còn buổi nào chờ chạy thì đóng. Không ai phải bấm gì, và việc ghi kết quả không liên quan.
 * (Brings the data up to the CLOCK: a Session whose end time passed becomes HELD (with its Workouts), a plan that reached its start
 * date becomes ACTIVE, and a plan with no Session left to run closes. Nobody has to press anything, and recording results has
 * nothing to do with it.)
 */
public interface SessionSettlementService {

    /** Áp theo giờ hiện tại của hệ thống; gọi lại là vô hại. (Applies at the current system time; calling again is harmless.) */
    void settle();
}
