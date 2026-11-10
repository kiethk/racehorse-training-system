package com.rtms.backend.training.service;

import com.rtms.backend.config.ApiException;
import com.rtms.backend.training.entity.CourseRun;
import com.rtms.backend.training.entity.TrainingGroupMember;
import com.rtms.backend.training.entity.TrainingSession;
import com.rtms.backend.training.entity.TrainingWorkout;
import com.rtms.backend.training.enums.SessionStatus;
import com.rtms.backend.training.enums.CourseRunStatus;
import com.rtms.backend.training.enums.WorkoutStatus;
import com.rtms.backend.training.repository.CourseRunRepository;
import com.rtms.backend.training.repository.TrainingGroupMemberRepository;
import com.rtms.backend.training.repository.TrainingSessionRepository;
import com.rtms.backend.training.repository.TrainingWorkoutRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Cài đặt GroupWorkoutSyncService. Mọi hàm bắt đầu bằng settle() để "chưa bắt đầu" được xét theo đồng hồ hiện tại.
 * (Implements GroupWorkoutSyncService. Every method starts with settle() so "not started" is judged by the current clock.)
 */
@Service
public class GroupWorkoutSyncServiceImpl implements GroupWorkoutSyncService {

    private final CourseRunRepository courseRunRepository;
    private final TrainingSessionRepository sessionRepository;
    private final TrainingWorkoutRepository workoutRepository;
    private final TrainingGroupMemberRepository memberRepository;
    private final SessionSettlementService settlement;
    private final Clock clock;

    public GroupWorkoutSyncServiceImpl(CourseRunRepository courseRunRepository,
                                       TrainingSessionRepository sessionRepository,
                                       TrainingWorkoutRepository workoutRepository,
                                       TrainingGroupMemberRepository memberRepository,
                                       SessionSettlementService settlement,
                                       Clock clock) {
        this.courseRunRepository = courseRunRepository;
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
        List<Long> courseRunIds = unfinishedCourseRunIds(groupId);
        if (!courseRunIds.isEmpty() && notStartedSessions(courseRunIds).isEmpty()) {
            throw new ApiException(HttpStatus.CONFLICT, "COURSE_RUN_NO_SESSION_LEFT",
                    "This group's course has no session left to attend. Wait for it to finish, then add the horse!");
        }
    }

    @Override
    @Transactional
    public int createFutureWorkouts(TrainingGroupMember member) {
        settlement.settle();
        List<Long> courseRunIds = unfinishedCourseRunIds(member.getGroupId());
        if (courseRunIds.isEmpty()) {
            return 0;
        }
        List<TrainingWorkout> created = new ArrayList<>();
        for (TrainingSession session : notStartedSessions(courseRunIds)) {
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
        List<Long> courseRunIds = unfinishedCourseRunIds(member.getGroupId());
        if (!courseRunIds.isEmpty()) {
            List<TrainingSession> future = notStartedSessions(courseRunIds);
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
        member.setLeftAt(LocalDateTime.now(clock));
        memberRepository.saveAndFlush(member);
        settlement.settle();
    }

    @Override
    @Transactional
    public int reassignFutureWorkoutsToGroom(Long horseId, Long newGroomId) {
        Map<Long, Long> single = new HashMap<>();
        single.put(horseId, newGroomId);
        return reassignFutureWorkoutsToGrooms(single);
    }

    @Override
    @Transactional
    public int reassignFutureWorkoutsToGrooms(Map<Long, Long> groomByHorse) {
        if (groomByHorse.isEmpty()) {
            return 0;
        }
        settlement.settle();
        List<TrainingGroupMember> changing = memberRepository.findByHorseIdInAndLeftAtIsNull(groomByHorse.keySet()).stream()
                .filter(member -> !Objects.equals(member.getGroomId(), groomByHorse.get(member.getHorseId())))
                .toList();
        if (changing.isEmpty()) {
            return 0;
        }
        changing.forEach(member -> member.setGroomId(groomByHorse.get(member.getHorseId())));
        memberRepository.saveAllAndFlush(changing);

        LocalDateTime now = LocalDateTime.now(clock);
        List<TrainingWorkout> future = workoutRepository.findNotStartedOfHorses(
                changing.stream().map(TrainingGroupMember::getHorseId).toList(), now.toLocalDate(), now.toLocalTime());
        future.forEach(workout -> workout.setAssignedToId(groomByHorse.get(workout.getHorseId())));
        workoutRepository.saveAllAndFlush(future);
        return future.size();
    }

    @Override
    @Transactional
    public void exitAnyGroup(Long horseId) {
        memberRepository.findByHorseIdAndLeftAtIsNull(horseId).ifPresent(this::exitGroup);
    }

    // =================================================================
    // TIỆN ÍCH (Helpers)
    // =================================================================

    private List<Long> unfinishedCourseRunIds(Long groupId) {
        return courseRunRepository.findByGroupIdAndStatusIn(groupId, CourseRunStatus.UNFINISHED).stream().map(CourseRun::getId).toList();
    }

    private List<TrainingSession> notStartedSessions(List<Long> courseRunIds) {
        LocalDateTime now = LocalDateTime.now(clock);
        return sessionRepository.findNotStartedOfCourseRuns(courseRunIds, now.toLocalDate(), now.toLocalTime());
    }
}
