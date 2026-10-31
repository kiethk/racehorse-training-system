package com.rtms.backend.training.service;

import com.rtms.backend.config.ApiException;
import com.rtms.backend.training.entity.GroupPlan;
import com.rtms.backend.training.entity.TrainingGroupMember;
import com.rtms.backend.training.entity.TrainingSession;
import com.rtms.backend.training.entity.TrainingWorkout;
import com.rtms.backend.training.enums.SessionStatus;
import com.rtms.backend.training.enums.TrainingPlanStatus;
import com.rtms.backend.training.enums.WorkoutStatus;
import com.rtms.backend.training.repository.GroupPlanRepository;
import com.rtms.backend.training.repository.TrainingGroupMemberRepository;
import com.rtms.backend.training.repository.TrainingSessionRepository;
import com.rtms.backend.training.repository.TrainingWorkoutRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Cài đặt GroupWorkoutSyncService. Mọi hàm bắt đầu bằng settle() để "chưa bắt đầu" được xét theo đồng hồ hiện tại.
 * (Implements GroupWorkoutSyncService. Every method starts with settle() so "not started" is judged by the current clock.)
 */
@Service
public class GroupWorkoutSyncServiceImpl implements GroupWorkoutSyncService {

    private static final List<TrainingPlanStatus> UNFINISHED = List.of(TrainingPlanStatus.UPCOMING, TrainingPlanStatus.ACTIVE);

    private final GroupPlanRepository planRepository;
    private final TrainingSessionRepository sessionRepository;
    private final TrainingWorkoutRepository workoutRepository;
    private final TrainingGroupMemberRepository memberRepository;
    private final SessionSettlementService settlement;
    private final Clock clock;

    public GroupWorkoutSyncServiceImpl(GroupPlanRepository planRepository,
                                       TrainingSessionRepository sessionRepository,
                                       TrainingWorkoutRepository workoutRepository,
                                       TrainingGroupMemberRepository memberRepository,
                                       SessionSettlementService settlement,
                                       Clock clock) {
        this.planRepository = planRepository;
        this.sessionRepository = sessionRepository;
        this.workoutRepository = workoutRepository;
        this.memberRepository = memberRepository;
        this.settlement = settlement;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void assertCanJoin(Long groupId) {
        settlement.settle();
        List<Long> planIds = unfinishedPlanIds(groupId);
        if (!planIds.isEmpty() && notStartedSessions(planIds).isEmpty()) {
            throw new ApiException(HttpStatus.CONFLICT, "GROUP_PLAN_NO_SESSION_LEFT",
                    "This group's course has no session left to attend. Wait for it to finish, then add the horse!");
        }
    }

    @Override
    @Transactional
    public int createFutureWorkouts(TrainingGroupMember member) {
        settlement.settle();
        List<Long> planIds = unfinishedPlanIds(member.getGroupId());
        if (planIds.isEmpty()) {
            return 0;
        }
        List<TrainingWorkout> created = new ArrayList<>();
        for (TrainingSession session : notStartedSessions(planIds)) {
            TrainingWorkout workout = new TrainingWorkout();
            workout.setSessionId(session.getId());
            workout.setHorseId(member.getHorseId());
            workout.setAssignedToId(member.getGroomId());
            workout.setStatus(WorkoutStatus.SCHEDULED);
            created.add(workout);
        }
        workoutRepository.saveAllAndFlush(created);
        return created.size();
    }

    @Override
    @Transactional
    public void exitGroup(TrainingGroupMember member) {
        settlement.settle();
        List<Long> planIds = unfinishedPlanIds(member.getGroupId());
        if (!planIds.isEmpty()) {
            List<TrainingSession> future = notStartedSessions(planIds);
            List<TrainingWorkout> mine = future.isEmpty() ? List.of()
                    : workoutRepository.findScheduledOfHorseInSessions(
                            member.getHorseId(), future.stream().map(TrainingSession::getId).toList());
            mine.forEach(workout -> workout.setStatus(WorkoutStatus.CANCELLED));
            workoutRepository.saveAllAndFlush(mine);

            // Buổi không còn Workout nào hiệu lực thì tự hủy. (A Session with no effective Workout left cancels itself.)
            for (TrainingSession session : future) {
                if (workoutRepository.countBySessionIdAndStatusNot(session.getId(), WorkoutStatus.CANCELLED) == 0) {
                    session.setStatus(SessionStatus.CANCELLED);
                    sessionRepository.saveAndFlush(session);
                }
            }
        }
        memberRepository.deleteById(member.getId());
        memberRepository.flush();
        settlement.settle();
    }

    @Override
    @Transactional
    public void exitAnyGroup(Long horseId) {
        memberRepository.findByHorseId(horseId).ifPresent(this::exitGroup);
    }

    // =================================================================
    // TIỆN ÍCH (Helpers)
    // =================================================================

    private List<Long> unfinishedPlanIds(Long groupId) {
        return planRepository.findByGroupIdAndStatusIn(groupId, UNFINISHED).stream().map(GroupPlan::getId).toList();
    }

    private List<TrainingSession> notStartedSessions(List<Long> planIds) {
        LocalDateTime now = LocalDateTime.now(clock);
        return sessionRepository.findNotStartedOfPlans(planIds, now.toLocalDate(), now.toLocalTime());
    }
}
