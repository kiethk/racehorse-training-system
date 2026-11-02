package com.rtms.backend.scheduler;

import com.rtms.backend.training.service.SessionSettlementService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Giữ trạng thái buổi tập và kế hoạch của nhóm khớp đồng hồ ngay cả khi không ai mở màn hình. Dữ liệu cũng được đưa về đúng trước mỗi
 * lần đọc hay ghi liên quan, nên job này chỉ là lưới an toàn: nếu nó không chạy, người dùng vẫn không thấy trạng thái sai.
 * (Keeps the status of group Sessions and plans matching the clock even when nobody opens a screen. The data is also brought up to
 * date before every related read or write, so this job is only a safety net: if it does not run, users still never see a wrong
 * status.)
 */
@Component
public class SessionSettlementScheduler {

    private static final Logger log = LoggerFactory.getLogger(SessionSettlementScheduler.class);

    private final SessionSettlementService settlement;

    public SessionSettlementScheduler(SessionSettlementService settlement) {
        this.settlement = settlement;
    }

    @Scheduled(fixedDelay = 5 * 60 * 1000L, initialDelay = 60 * 1000L)
    public void settle() {
        try {
            settlement.settle();
        } catch (Exception e) {
            log.error("[Session Scheduler] Lỗi khi cập nhật trạng thái buổi tập: {}", e.getMessage(), e);
        }
    }
}
