package com.rtms.backend.training.service;

import com.rtms.backend.security.AuthenticatedUser;
import com.rtms.backend.training.dto.HorseAttendanceResponse;
import com.rtms.backend.training.dto.PlanSessionCountRow;
import com.rtms.backend.training.entity.Course;
import com.rtms.backend.training.entity.GroupPlan;
import com.rtms.backend.training.entity.TrainingGroup;
import com.rtms.backend.training.entity.TrainingSession;
import com.rtms.backend.training.entity.TrainingWorkout;
import com.rtms.backend.training.enums.SessionStatus;
import com.rtms.backend.training.enums.TrainingPlanStatus;
import com.rtms.backend.training.enums.WorkoutStatus;
import com.rtms.backend.training.repository.CourseRepository;
import com.rtms.backend.training.repository.GroupPlanRepository;
import com.rtms.backend.training.repository.TrainingGroupRepository;
import com.rtms.backend.training.repository.TrainingSessionRepository;
import com.rtms.backend.training.repository.TrainingWorkoutRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Cài đặt HorseAttendanceService. Đã học (X) là số Workout RAN của ngựa (đã ghi kết quả). Tổng (Y) là các buổi chưa bị
 * hủy của chính kế hoạch (kể cả buổi trước khi ngựa vào và buổi mà Workout riêng của ngựa bị hủy), không lấy từ Course nên sửa Course
 * về sau không làm đổi lịch sử cũ.
 * (Implements HorseAttendanceService. Attended (X) is the horse's Workouts that Ran (results recorded). Total (Y) is
 * the plan's own Sessions that were not cancelled (including Sessions before the horse joined and Sessions where only its own
 * Workout was cancelled), never taken from the Course, so editing a Course later does not rewrite old history.)
 */
@Service
public class HorseAttendanceServiceImpl implements HorseAttendanceService {

    private final TrainingWorkoutRepository workoutRepository;
    private final TrainingSessionRepository sessionRepository;
    private final GroupPlanRepository planRepository;
    private final TrainingGroupRepository groupRepository;
    private final CourseRepository courseRepository;
    private final HorseViewAccessService accessService;
    private final SessionSettlementService settlement;

    public HorseAttendanceServiceImpl(TrainingWorkoutRepository workoutRepository,
                                      TrainingSessionRepository sessionRepository,
                                      GroupPlanRepository planRepository,
                                      TrainingGroupRepository groupRepository,
                                      CourseRepository courseRepository,
                                      HorseViewAccessService accessService,
                                      SessionSettlementService settlement) {
        this.workoutRepository = workoutRepository;
        this.sessionRepository = sessionRepository;
        this.planRepository = planRepository;
        this.groupRepository = groupRepository;
        this.courseRepository = courseRepository;
        this.accessService = accessService;
        this.settlement = settlement;
    }

    @Override
    @Transactional
    public List<HorseAttendanceResponse> getHistory(Long horseId, AuthenticatedUser currentUser) {
        accessService.assertCanViewHorse(horseId, currentUser);
        settlement.settle();

        List<TrainingWorkout> workouts = workoutRepository.findByHorseIdAndSessionIdIsNotNull(horseId);
        if (workouts.isEmpty()) {
            return List.of();
        }
        Map<Long, TrainingSession> sessionsById = sessionRepository
                .findAllById(workouts.stream().map(TrainingWorkout::getSessionId).collect(Collectors.toSet())).stream()
                .collect(Collectors.toMap(TrainingSession::getId, Function.identity()));
        Map<Long, List<TrainingWorkout>> workoutsByPlan = workouts.stream()
                .collect(Collectors.groupingBy(workout -> sessionsById.get(workout.getSessionId()).getGroupPlanId()));

        Map<Long, GroupPlan> plansById = planRepository.findAllById(workoutsByPlan.keySet()).stream()
                .collect(Collectors.toMap(GroupPlan::getId, Function.identity()));
        Map<Long, TrainingGroup> groupsById = groupRepository
                .findAllById(plansById.values().stream().map(GroupPlan::getGroupId).filter(Objects::nonNull).toList()).stream()
                .collect(Collectors.toMap(TrainingGroup::getId, Function.identity()));
        Map<Long, String> courseNames = courseRepository
                .findAllById(plansById.values().stream().map(GroupPlan::getCourseId).toList()).stream()
                .collect(Collectors.toMap(Course::getId, Course::getName));
        Map<Long, Map<SessionStatus, Long>> counts = sessionRepository.countByPlanAndStatus(workoutsByPlan.keySet()).stream()
                .collect(Collectors.groupingBy(PlanSessionCountRow::planId,
                        Collectors.toMap(PlanSessionCountRow::status, PlanSessionCountRow::count)));

        List<HorseAttendanceResponse> history = new ArrayList<>();
        for (Map.Entry<Long, List<TrainingWorkout>> entry : workoutsByPlan.entrySet()) {
            GroupPlan plan = plansById.get(entry.getKey());
            List<TrainingWorkout> planWorkouts = entry.getValue();
            int attended = (int) planWorkouts.stream().filter(workout -> workout.getStatus() == WorkoutStatus.RAN).count();
            long tookPlace = attended + planWorkouts.stream().filter(workout -> workout.getStatus() == WorkoutStatus.HELD).count();
            long scheduled = planWorkouts.stream().filter(workout -> workout.getStatus() == WorkoutStatus.SCHEDULED).count();

            String effectiveStatus;
            if (plan.getStatus() == TrainingPlanStatus.CANCELLED) {
                effectiveStatus = TrainingPlanStatus.CANCELLED.name();
            } else if (plan.getStatus() == TrainingPlanStatus.COMPLETED) {
                effectiveStatus = TrainingPlanStatus.COMPLETED.name();
            } else if (scheduled > 0) {
                effectiveStatus = plan.getStatus().name();
            } else if (tookPlace > 0) {
                effectiveStatus = TrainingPlanStatus.COMPLETED.name();
            } else {
                effectiveStatus = TrainingPlanStatus.CANCELLED.name();
            }

            Map<SessionStatus, Long> byStatus = counts.getOrDefault(plan.getId(), Map.of());
            int total = byStatus.getOrDefault(SessionStatus.HELD, 0L).intValue()
                    + byStatus.getOrDefault(SessionStatus.SCHEDULED, 0L).intValue();
            LocalDate joinedOn = planWorkouts.stream()
                    .map(workout -> sessionsById.get(workout.getSessionId()).getSessionDate())
                    .min(Comparator.naturalOrder()).orElse(plan.getStartDate());
            history.add(new HorseAttendanceResponse(plan.getId(),
                    plan.getGroupId() == null ? null : groupsById.get(plan.getGroupId()).getName(),
                    plan.getGroupId() != null && groupsById.get(plan.getGroupId()).isDisbanded(),
                    courseNames.get(plan.getCourseId()), effectiveStatus, plan.getStartDate(), plan.getEndDate(),
                    attended, total, joinedOn));
        }
        history.sort(Comparator.comparing(HorseAttendanceResponse::startDate).reversed());
        return history;
    }
}
