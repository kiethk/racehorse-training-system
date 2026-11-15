package com.rtms.backend.training.service;

import com.rtms.backend.config.ApiException;
import com.rtms.backend.horse.entity.Horse;
import com.rtms.backend.horse.enums.HorseStatus;
import com.rtms.backend.medical.enums.CareScheduleStatus;
import com.rtms.backend.medical.repository.CareScheduleRepository;
import com.rtms.backend.training.enums.TrainingDecision;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Trả lời "ngựa này tập được chưa, và nếu chưa thì vì sao và đến khi nào". Chỉ ĐỌC quyết định; việc đổi quyết định nằm ở
 * TrainingDecisionService. Tách riêng để TrainingGroupService hỏi được mà không vòng phụ thuộc với TrainingDecisionService.
 * (Answers "may this horse train yet, and if not, why and until when". It only READS the decision; changing it lives in
 * TrainingDecisionService. Separate so TrainingGroupService can ask without a dependency cycle with TrainingDecisionService.)
 */
@Component
class TrainingEligibility {

    private static final List<CareScheduleStatus> PENDING_EXAM_STATUSES = CareScheduleStatus.OPEN;
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final CareScheduleRepository careScheduleRepository;

    TrainingEligibility(CareScheduleRepository careScheduleRepository) {
        this.careScheduleRepository = careScheduleRepository;
    }

    /**
     * Ném lỗi kèm lý do cụ thể nếu ngựa chưa tập được. Trainer cần biết VÌ SAO và ĐẾN KHI NÀO, không chỉ "đang bị khóa".
     * (Throws an error with a concrete reason if the horse cannot train yet. The trainer needs to know WHY and
     * UNTIL WHEN, not just "locked".)
     */
    void assertCanTrain(Horse horse) {
        if (horse.getCurrentStatus() != HorseStatus.ELIGIBLE) {
            throw new ApiException(HttpStatus.CONFLICT, "HORSE_NOT_ELIGIBLE", String.format(
                    "Horse '%s' has not been admitted to the club (status %s), so a course run cannot be created yet!",
                    horse.getName(), horse.getCurrentStatus()));
        }
        if (horse.getTrainingDecision() == TrainingDecision.BLOCKED) {
            throw new ApiException(HttpStatus.CONFLICT, "TRAINING_BLOCKED", String.format(
                    "Horse '%s' is resting: %s. A new course run cannot be created!",
                    horse.getName(), describeRest(horse)));
        }
    }

    /**
     * Lý do nghỉ tập và hạn khám lại của một ngựa đang bị chặn, ví dụ "Tendon strain (re-examination at
     * 20/10/2026 14:00)". Dùng chung cho thông báo lỗi khi tạo kế hoạch và cho màn hình xem trước kế hoạch.
     * (The rest reason and re-examination time of a blocked horse, for example "Tendon strain (re-examination at
     * 20/10/2026 14:00)". Shared by the course run creation error and the course run preview screen.)
     */
    String describeRest(Horse horse) {
        LocalDateTime nextExam = careScheduleRepository.findNextExamTime(horse.getId(), PENDING_EXAM_STATUSES);
        String reason = horse.getTrainingDecisionReason() == null || horse.getTrainingDecisionReason().isBlank()
                ? "as directed by the vet"
                : horse.getTrainingDecisionReason();
        String until = nextExam == null
                ? "no follow-up exam scheduled"
                : "re-examination at " + nextExam.format(DATE_TIME);
        return reason + " (" + until + ")";
    }
}
