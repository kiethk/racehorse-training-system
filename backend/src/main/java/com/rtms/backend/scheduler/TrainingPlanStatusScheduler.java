package com.rtms.backend.scheduler;

import com.rtms.backend.service.HorseTrainingPlanService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Giữ nhãn trạng thái kế hoạch huấn luyện khớp với thực tế.
 *
 * VẤN ĐỀ NÓ GIẢI QUYẾT:
 * createPlan đặt UPCOMING cho kế hoạch có startDate ở tương lai. Trước khi có
 * job này, không gì chuyển nó sang ACTIVE khi ngày bắt đầu tới — Trainer mở
 * màn hình ra vẫn thấy "sắp diễn ra" trong khi khoá đã chạy được hai tuần.
 *
 * PHẠM VI CÓ CHỦ ĐÍCH — job này CHỈ lo hiển thị:
 * Việc đóng khoá (ACTIVE -> COMPLETED) do completeWorkout tự xử lý ngay khi
 * buổi cuối được ghi nhận, KHÔNG phụ thuộc job này. Nếu server tắt đúng nửa
 * đêm thì chỉ nhãn trạng thái chậm một ngày, kế hoạch vẫn đóng được bình
 * thường. Dồn cả hai việc vào job sẽ tạo một điểm hỏng duy nhất.
 */
@Component
public class TrainingPlanStatusScheduler {

    private static final Logger log = LoggerFactory.getLogger(TrainingPlanStatusScheduler.class);

    private final HorseTrainingPlanService planService;

    public TrainingPlanStatusScheduler(HorseTrainingPlanService planService) {
        this.planService = planService;
    }

    /**
     * 00:05 mỗi đêm — chạy SAU GroomDailyTaskScheduler (00:00) năm phút.
     *
     * Vì sao lệch giờ: hai job đụng những bảng khác nhau nên không tranh chấp,
     * nhưng tách ra thì log dễ đọc và job này không bị chậm nếu việc sinh SOP
     * cho hàng chục chuồng kéo dài.
     */
    @Scheduled(cron = "0 5 0 * * *", zone = "Asia/Ho_Chi_Minh")
    public void activateStartedPlans() {
        LocalDate today = LocalDate.now();
        try {
            int activated = planService.activateStartedPlans(today);
            if (activated > 0) {
                log.info("[Plan Scheduler] Đã chuyển {} kế hoạch từ UPCOMING sang ACTIVE (ngày {}).",
                        activated, today);
            }
        } catch (Exception e) {
            log.error("[Plan Scheduler] Lỗi khi cập nhật trạng thái kế hoạch: {}", e.getMessage(), e);
        }
    }
}
