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
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Giữ Workout khớp với thành viên của nhóm khi nhóm đang chạy một khóa: ngựa vào giữa khóa chỉ có Workout cho các buổi CHƯA bắt đầu,
 * ngựa ra (rút, chuyển nhóm, bị Thú y chặn tập) bị hủy các Workout của buổi chưa bắt đầu, và nhóm cùng các ngựa còn lại không đổi.
 * Mọi hàm bắt đầu bằng settle() để "chưa bắt đầu" được xét theo đồng hồ hiện tại. Chỉ TrainingGroupService và TrainingDecisionService
 * dùng lớp này; module khác đi qua TrainingGroupService.
 * (Keeps Workouts matching the members of a group while it runs a course: a horse that joins mid-course only gets Workouts for the
 * Sessions that have NOT started, and a horse that leaves (withdrawn, moved, blocked by the Vet) has its not-yet-started Workouts
 * cancelled while the group and the other horses stay as they are. Every method starts with settle() so "not started" is judged by
 * the current clock. Only TrainingGroupService and TrainingDecisionService use this class; other modules go through TrainingGroupService.)
 */
@Component
class GroupMemberSync {

    private final CourseRunRepository courseRunRepository;
    private final TrainingSessionRepository sessionRepository;
    private final TrainingWorkoutRepository workoutRepository;
    private final TrainingGroupMemberRepository memberRepository;
    private final SessionSettlementService settlement;
    private final SessionRules rules;
    private final Clock clock;

    GroupMemberSync(CourseRunRepository courseRunRepository,
                                       TrainingSessionRepository sessionRepository,
                                       TrainingWorkoutRepository workoutRepository,
                                       TrainingGroupMemberRepository memberRepository,
                                       SessionSettlementService settlement,
                                       SessionRules rules,
                                       Clock clock) {
        this.courseRunRepository = courseRunRepository;
        this.sessionRepository = sessionRepository;
        this.workoutRepository = workoutRepository;
        this.memberRepository = memberRepository;
        this.settlement = settlement;
        this.rules = rules;
        this.clock = clock;
    }

    @Transactional
    void assertCanJoin(Long groupId) {
        settlement.settle();
        List<Long> courseRunIds = unfinishedCourseRunIds(groupId);
        if (!courseRunIds.isEmpty() && notStartedSessions(courseRunIds).isEmpty()) {
            throw new ApiException(HttpStatus.CONFLICT, "COURSE_RUN_NO_SESSION_LEFT",
                    "This group's course has no session left to attend. Wait for it to finish, then add the horse!");
        }
    }

    @Transactional
    int createFutureWorkouts(TrainingGroupMember member) {
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

    @Transactional
    void exitGroup(TrainingGroupMember member) {
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
                rules.cancelIfEmpty(session);
            }
        }
        member.setLeftAt(LocalDateTime.now(clock));
        memberRepository.saveAndFlush(member);
        settlement.settle();
    }

    @Transactional
    int reassignFutureWorkoutsToGroom(Long horseId, Long newGroomId) {
        Map<Long, Long> single = new HashMap<>();
        single.put(horseId, newGroomId);
        return reassignFutureWorkoutsToGrooms(single);
    }

    @Transactional
    int reassignFutureWorkoutsToGrooms(Map<Long, Long> groomByHorse) {
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

    @Transactional
    void exitAnyGroup(Long horseId) {
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
