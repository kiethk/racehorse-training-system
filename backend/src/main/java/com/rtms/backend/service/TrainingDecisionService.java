package com.rtms.backend.service;

import com.rtms.backend.config.ApiException;
import com.rtms.backend.entity.Horse;
import com.rtms.backend.enums.CareScheduleStatus;
import com.rtms.backend.enums.HorseStatus;
import com.rtms.backend.enums.TrainingDecision;
import com.rtms.backend.repository.CareScheduleRepository;
import com.rtms.backend.repository.HorseRepository;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Nơi DUY NHẤT đổi quyết định "ngựa có được tập không".
 *
 * Trước đây 8 chỗ (Vet, Groom, Manager, sự cố...) tự đặt 3-4 trường khóa trên
 * ngựa và không chỗ nào hủy kế hoạch đang chạy: ngựa bị chặn mà buổi tập vẫn
 * nằm trong lot, Groom vẫn được giao dắt đi tập. Gom về đây để chặn tập luôn
 * kéo theo hủy buổi tập tương lai.
 */
@Service
public class TrainingDecisionService {

    private static final List<CareScheduleStatus> PENDING_EXAM_STATUSES = List.of(
            CareScheduleStatus.REQUESTED, CareScheduleStatus.SCHEDULED, CareScheduleStatus.IN_PROGRESS);
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final HorseRepository horseRepository;
    private final CareScheduleRepository careScheduleRepository;
    private final HorseTrainingPlanService trainingPlanService;

    /**
     * @param trainingPlanService @Lazy vì HorseTrainingPlanService cũng dùng lớp
     *                            này (assertCanTrain) — tránh vòng phụ thuộc lúc khởi động.
     */
    public TrainingDecisionService(HorseRepository horseRepository,
                                   CareScheduleRepository careScheduleRepository,
                                   @Lazy HorseTrainingPlanService trainingPlanService) {
        this.horseRepository = horseRepository;
        this.careScheduleRepository = careScheduleRepository;
        this.trainingPlanService = trainingPlanService;
    }

    /**
     * Chặn tập và hủy mọi buổi tập chưa diễn ra (lot rỗng bị hủy theo, plan
     * đang chạy chuyển CANCELLED). Gọi lại khi ngựa đã bị chặn là vô hại:
     * lần hủy thứ hai không còn gì để hủy.
     */
    @Transactional
    public void block(Horse horse, String reason) {
        horse.setTrainingDecision(TrainingDecision.BLOCKED);
        horse.setTrainingDecisionReason(reason);
        horseRepository.save(horse);
        trainingPlanService.cancelFutureTrainingForHorse(horse.getId(),
                reason == null || reason.isBlank() ? "Ngựa bị chặn tập" : "Ngựa bị chặn tập: " + reason);
    }

    @Transactional
    public void allow(Horse horse) {
        horse.setTrainingDecision(TrainingDecision.ALLOWED);
        horse.setTrainingDecisionReason(null);
        horseRepository.save(horse);
    }

    /**
     * Ném lỗi kèm lý do cụ thể nếu ngựa chưa tập được. Trainer cần biết VÌ SAO
     * và ĐẾN KHI NÀO, không chỉ "đang bị khóa".
     */
    public void assertCanTrain(Horse horse) {
        if (horse.getCurrentStatus() != HorseStatus.ELIGIBLE) {
            throw new ApiException(HttpStatus.CONFLICT, "HORSE_NOT_ELIGIBLE", String.format(
                    "Chiến mã '%s' chưa được nhận vào câu lạc bộ (trạng thái %s) — chưa thể lập kế hoạch!",
                    horse.getName(), horse.getCurrentStatus()));
        }
        if (horse.getTrainingDecision() == TrainingDecision.BLOCKED) {
            LocalDateTime nextExam = careScheduleRepository.findNextExamTime(horse.getId(), PENDING_EXAM_STATUSES);
            String reason = horse.getTrainingDecisionReason() == null || horse.getTrainingDecisionReason().isBlank()
                    ? "theo chỉ định của Thú y"
                    : horse.getTrainingDecisionReason();
            String until = nextExam == null
                    ? "chưa có lịch khám lại"
                    : "khám lại lúc " + nextExam.format(DATE_TIME);
            throw new ApiException(HttpStatus.CONFLICT, "TRAINING_BLOCKED", String.format(
                    "Chiến mã '%s' đang tạm nghỉ: %s (%s). Không thể lập kế hoạch mới!",
                    horse.getName(), reason, until));
        }
    }
}
