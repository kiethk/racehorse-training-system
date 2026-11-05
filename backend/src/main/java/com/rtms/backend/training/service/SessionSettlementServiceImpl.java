package com.rtms.backend.training.service;

import com.rtms.backend.training.entity.GroupPlan;
import com.rtms.backend.training.enums.SessionStatus;
import com.rtms.backend.training.enums.TrainingPlanStatus;
import com.rtms.backend.training.repository.GroupPlanRepository;
import com.rtms.backend.training.repository.TrainingSessionRepository;
import com.rtms.backend.training.repository.TrainingWorkoutRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Cài đặt SessionSettlementService bằng vài câu UPDATE hàng loạt, nên chi phí không phụ thuộc số buổi. Được gọi trước mỗi lần đọc hay
 * ghi liên quan đến trạng thái và bởi job chạy định kỳ, nên dữ liệu người dùng thấy luôn đúng với đồng hồ.
 * (Implements SessionSettlementService with a few bulk UPDATEs, so the cost does not depend on the number of Sessions. Called before
 * every status-related read or write and by a periodic job, so what users see always matches the clock.)
 */
@Service
public class SessionSettlementServiceImpl implements SessionSettlementService {

    private final TrainingSessionRepository sessionRepository;
    private final TrainingWorkoutRepository workoutRepository;
    private final GroupPlanRepository planRepository;
    private final Clock clock;

    public SessionSettlementServiceImpl(TrainingSessionRepository sessionRepository,
                                        TrainingWorkoutRepository workoutRepository,
                                        GroupPlanRepository planRepository,
                                        Clock clock) {
        this.sessionRepository = sessionRepository;
        this.workoutRepository = workoutRepository;
        this.planRepository = planRepository;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void settle() {
        if (TransactionSynchronizationManager.isCurrentTransactionReadOnly()) {
            return;     // a read-only caller (the AI tools) cannot write; the scheduler keeps statuses within minutes of the clock
        }
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDate today = now.toLocalDate();

        // 1. Buổi hết giờ và các Workout còn SCHEDULED của nó thành HELD. (Ended Sessions and their SCHEDULED Workouts become HELD.)
        if (sessionRepository.markEndedAsHeld(today, now.toLocalTime()) > 0) {
            workoutRepository.markWorkoutsOfHeldSessionsAsHeld();
        }

        // 2. Kế hoạch tới ngày bắt đầu thành ACTIVE. (Plans that reached their start date become ACTIVE.)
        List<GroupPlan> started = planRepository.findByStatusAndStartDateLessThanEqual(TrainingPlanStatus.UPCOMING, today);
        started.forEach(plan -> plan.setStatus(TrainingPlanStatus.ACTIVE));
        planRepository.saveAll(started);

        // 3. Kế hoạch không còn buổi nào chờ chạy thì đóng. (Plans with no Session left to run close.)
        List<GroupPlan> finished = planRepository.findWithoutScheduledSessions(
                List.of(TrainingPlanStatus.UPCOMING, TrainingPlanStatus.ACTIVE), SessionStatus.SCHEDULED);
        for (GroupPlan plan : finished) {
            boolean anyHeld = sessionRepository.countByGroupPlanIdAndStatus(plan.getId(), SessionStatus.HELD) > 0;
            plan.setStatus(anyHeld ? TrainingPlanStatus.COMPLETED : TrainingPlanStatus.CANCELLED);
        }
        planRepository.saveAll(finished);
    }
}
