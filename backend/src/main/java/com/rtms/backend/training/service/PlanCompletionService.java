package com.rtms.backend.training.service;

import com.rtms.backend.training.entity.HorseTrainingPlan;
import com.rtms.backend.training.enums.TrainingPlanStatus;
import com.rtms.backend.training.enums.WorkoutStatus;
import com.rtms.backend.training.repository.HorseTrainingPlanRepository;
import com.rtms.backend.training.repository.TrainingWorkoutRepository;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

/**
 * Đóng một kế hoạch khi nó không còn buổi nào phải tập. Một luật duy nhất cho mọi cách đóng buổi tập (ghi kết quả,
 * hủy lot), để kế hoạch không bị kẹt ở UPCOMING/ACTIVE và không chiếm khoảng ngày của BR-01 khi đã hết buổi.
 * (Closes a plan once it has no session left to hold. One rule for every way a session is closed (recording a result,
 * cancelling a lot), so a plan never sticks at UPCOMING/ACTIVE and stops holding its BR-01 date range once empty.)
 *
 * Luật (The rule):
 *  - Còn buổi SCHEDULED: giữ nguyên. (A SCHEDULED session is left: unchanged.)
 *  - Hết buổi SCHEDULED, đã tập ít nhất một buổi: COMPLETED, tức "khóa đã đóng"; khóa đủ hay thiếu xem ở tiến độ
 *    (số buổi đã tập và số buổi bị hủy). (None left and at least one held: COMPLETED, meaning "the course is
 *    closed"; whether it was full shows in the progress (sessions held and sessions cancelled).)
 *  - Hết buổi SCHEDULED, chưa tập buổi nào: CANCELLED. (None left and none held: CANCELLED.)
 *
 * Ngựa bị khóa tập KHÔNG đi qua đây: TrainingWorkoutService.cancelFutureTrainingForHorse luôn hủy kế hoạch, kể cả khi
 * đã tập vài buổi, vì kế hoạch dở dang sau thời gian nghỉ hiếm khi còn đúng.
 * (A blocked horse does NOT go through here: TrainingWorkoutService.cancelFutureTrainingForHorse always cancels the
 * plan, even after some sessions were held, since an unfinished plan rarely still fits after the rest.)
 */
@Service
public class PlanCompletionService {

    private static final List<WorkoutStatus> CLOSED = List.of(WorkoutStatus.COMPLETED, WorkoutStatus.CANCELLED);

    private final TrainingWorkoutRepository workoutRepository;
    private final HorseTrainingPlanRepository planRepository;

    public PlanCompletionService(TrainingWorkoutRepository workoutRepository,
                                 HorseTrainingPlanRepository planRepository) {
        this.workoutRepository = workoutRepository;
        this.planRepository = planRepository;
    }

    /**
     * Áp luật lên một kế hoạch đã tải; người gọi tự lưu. Các buổi vừa đổi phải được ghi xuống DB trước.
     * (Applies the rule to a loaded plan; the caller saves it. Changed sessions must be written to the DB first.)
     */
    public void closeIfFinished(HorseTrainingPlan plan) {
        if (plan.getStatus() == TrainingPlanStatus.COMPLETED || plan.getStatus() == TrainingPlanStatus.CANCELLED) {
            return;
        }
        if (workoutRepository.countByPlanIdAndStatusNotIn(plan.getId(), CLOSED) > 0) {
            return;
        }
        boolean anyHeld = workoutRepository.countByPlanIdAndStatus(plan.getId(), WorkoutStatus.COMPLETED) > 0;
        plan.setStatus(anyHeld ? TrainingPlanStatus.COMPLETED : TrainingPlanStatus.CANCELLED);
    }

    /** Áp luật lên nhiều kế hoạch theo id rồi lưu. (Applies the rule to several plans by id and saves them.) */
    public void closeFinishedPlans(Collection<Long> planIds) {
        if (planIds.isEmpty()) {
            return;
        }
        List<HorseTrainingPlan> plans = planRepository.findAllById(planIds);
        plans.forEach(this::closeIfFinished);
        planRepository.saveAll(plans);
    }
}
