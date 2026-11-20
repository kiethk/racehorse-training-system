package com.rtms.backend.training.service;

import com.rtms.backend.training.util.ScheduleLocks;
import com.rtms.backend.training.util.WorkoutResults;
import com.rtms.backend.config.ApiException;
import com.rtms.backend.security.AuthenticatedUser;
import com.rtms.backend.training.dto.CompleteWorkoutRequest;
import com.rtms.backend.training.dto.SessionWorkoutResponse;
import com.rtms.backend.training.dto.WorkoutResultPreviewResponse;
import com.rtms.backend.training.entity.Exercise;
import com.rtms.backend.training.entity.TrainingSession;
import com.rtms.backend.training.entity.TrainingWorkout;
import com.rtms.backend.training.enums.WorkoutStatus;
import com.rtms.backend.training.repository.ExerciseRepository;
import com.rtms.backend.training.repository.TrainingSessionRepository;
import com.rtms.backend.training.repository.TrainingWorkoutRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Xác nhận Workout của buổi đã diễn ra: ghi kết quả chuyển HELD thành RAN (bằng chứng con ngựa đã chạy), "Did not run" chuyển HELD
 * thành CANCELLED. Session và kế hoạch vẫn theo đồng hồ.
 * (Confirming the Workouts of a Session that took place: recording results turns HELD into RAN (the proof the horse took part),
 * "Did not run" turns HELD into CANCELLED. Sessions and course runs still follow the clock.)
 *
 * Mọi thao tác bắt đầu bằng settle() để Workout của buổi đã hết giờ đã là HELD.
 * (Every operation starts with settle() so the Workouts of an ended Session are already HELD.)
 */
@Service
public class TrainingWorkoutService {

    private final TrainingWorkoutRepository workoutRepository;
    private final TrainingSessionRepository sessionRepository;
    private final ExerciseRepository exerciseRepository;
    private final WorkoutResultSimulationService simulationService;
    private final SessionSettlementService settlement;
    private final SessionRules rules;

    public TrainingWorkoutService(TrainingWorkoutRepository workoutRepository,
                                     TrainingSessionRepository sessionRepository,
                                     ExerciseRepository exerciseRepository,
                                     WorkoutResultSimulationService simulationService,
                                     SessionSettlementService settlement,
                                     SessionRules rules) {
        this.workoutRepository = workoutRepository;
        this.sessionRepository = sessionRepository;
        this.exerciseRepository = exerciseRepository;
        this.simulationService = simulationService;
        this.settlement = settlement;
        this.rules = rules;
    }

    /** Số đo mô phỏng mới, không lưu. Chỉ sau khi buổi đã hết giờ. (A fresh simulated measurement, not saved. Only after the Session ended.) */
    @Transactional
    public WorkoutResultPreviewResponse previewResult(Long workoutId, AuthenticatedUser currentUser) {
        settlement.settle();
        TrainingWorkout workout = findOwnWorkout(workoutId, currentUser);
        assertTookPlace(workout);
        TrainingSession session = findSession(workout);
        Exercise exercise = exerciseRepository.findById(session.getExerciseId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "EXERCISE_NOT_FOUND", "Exercise of this workout not found!"));
        return simulationService.generate(exercise);
    }

    /** Ghi kết quả lần đầu cho một Workout HELD, chuyển nó thành RAN; không có hạn chót. (First recording on a HELD Workout, which becomes RAN; no deadline.) */
    @Transactional
    public SessionWorkoutResponse recordResult(Long workoutId, CompleteWorkoutRequest request, AuthenticatedUser currentUser) {
        settlement.settle();
        TrainingWorkout workout = findOwnWorkoutForUpdate(workoutId, currentUser);
        assertTookPlace(workout);
        if (workout.getStatus() == WorkoutStatus.RAN) {
            throw new ApiException(HttpStatus.CONFLICT, "WORKOUT_ALREADY_RECORDED",
                    "Results were already recorded for this workout. Edit them instead!");
        }
        WorkoutResults.validate(request);
        WorkoutResults.apply(workout, request);
        workout.setStatus(WorkoutStatus.RAN);
        return SessionWorkoutResponse.of(workoutRepository.saveAndFlush(workout));
    }

    /** Sửa kết quả của Workout RAN. (Edits the results of a Workout that Ran.) */
    @Transactional
    public SessionWorkoutResponse updateResult(Long workoutId, CompleteWorkoutRequest request, AuthenticatedUser currentUser) {
        settlement.settle();
        TrainingWorkout workout = findOwnWorkoutForUpdate(workoutId, currentUser);
        assertTookPlace(workout);
        if (workout.getStatus() != WorkoutStatus.RAN) {
            throw new ApiException(HttpStatus.CONFLICT, "WORKOUT_NOT_RECORDED",
                    "No results were recorded for this workout yet!");
        }
        WorkoutResults.validate(request);
        WorkoutResults.apply(workout, request);
        return SessionWorkoutResponse.of(workoutRepository.saveAndFlush(workout));
    }

    /**
     * Đánh dấu "Không chạy": chiến mã thực ra không tham gia dù buổi đã diễn ra. Workout thành CANCELLED và không còn tính vào
     * Attendance. Chỉ khi còn HELD.
     * (Marks "Did not run": the horse did not actually take part although the Session took place. The Workout becomes CANCELLED and
     * no longer counts in Attendance. Only while still HELD.)
    */
    @Transactional
    public SessionWorkoutResponse markDidNotRun(Long workoutId, AuthenticatedUser currentUser) {
        settlement.settle();
        TrainingWorkout workout = findOwnWorkoutForUpdate(workoutId, currentUser);
        assertTookPlace(workout);
        if (workout.getStatus() == WorkoutStatus.RAN) {
            throw new ApiException(HttpStatus.CONFLICT, "WORKOUT_ALREADY_RECORDED",
                    "Results were already recorded, so this horse cannot be marked as not running!");
        }
        workout.setStatus(WorkoutStatus.CANCELLED);
        return SessionWorkoutResponse.of(workoutRepository.saveAndFlush(workout));
    }

    /** Hủy Workout của riêng một chiến mã trước khi buổi bắt đầu; con vẫn ở trong nhóm. Buổi không còn Workout nào thì tự hủy. (Cancels one horse's Workout before its Session starts; the horse stays in the group. A Session left with none cancels itself.) */
    @Transactional
    public SessionWorkoutResponse cancel(Long workoutId, AuthenticatedUser currentUser) {
        sessionRepository.lockSchedule(ScheduleLocks.of(currentUser.getUserId()));
        settlement.settle();
        TrainingWorkout workout = workoutRepository.findById(workoutId)
                .filter(found -> found.getSessionId() != null)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "WORKOUT_NOT_FOUND", "Workout not found!"));
        TrainingSession session = rules.findOwnSession(workout.getSessionId(), currentUser);
        rules.assertNotStarted(session);
        if (workout.getStatus() != WorkoutStatus.SCHEDULED) {
            throw new ApiException(HttpStatus.CONFLICT, "WORKOUT_NOT_SCHEDULED", "Only a scheduled workout can be cancelled!");
        }

        workout.setStatus(WorkoutStatus.CANCELLED);
        workoutRepository.saveAndFlush(workout);
        if (rules.cancelIfEmpty(session)) {
            settlement.settle();
        }
        return SessionWorkoutResponse.of(workoutRepository.findById(workoutId).orElseThrow());
    }

    // =================================================================
    // TIỆN ÍCH (Helpers)
    // =================================================================

    /** Workout của buổi thuộc Trainer này; của người khác báo "không tìm thấy" để không lộ nó tồn tại. */
    private TrainingWorkout findOwnWorkout(Long workoutId, AuthenticatedUser currentUser) {
        return ownOrNotFound(workoutRepository.findById(workoutId), currentUser);
    }

    /** Như findOwnWorkout nhưng khóa dòng, dùng cho thao tác ghi. (Like findOwnWorkout but locks the row, for writes.) */
    private TrainingWorkout findOwnWorkoutForUpdate(Long workoutId, AuthenticatedUser currentUser) {
        return ownOrNotFound(workoutRepository.findByIdForUpdate(workoutId), currentUser);
    }

    private TrainingWorkout ownOrNotFound(Optional<TrainingWorkout> found, AuthenticatedUser currentUser) {
        TrainingWorkout workout = found
                .filter(w -> w.getSessionId() != null)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "WORKOUT_NOT_FOUND", "Workout not found!"));
        TrainingSession session = findSession(workout);
        if (!session.getTrainerId().equals(currentUser.getUserId())) {
            throw new ApiException(HttpStatus.NOT_FOUND, "WORKOUT_NOT_FOUND", "Workout not found!");
        }
        return workout;
    }

    private TrainingSession findSession(TrainingWorkout workout) {
        return sessionRepository.findById(workout.getSessionId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "WORKOUT_NOT_FOUND", "Workout not found!"));
    }

    /** Workout của buổi đã diễn ra và chưa bị hủy: HELD hoặc RAN. (A Workout of a Session that took place and was not cancelled: HELD or RAN.) */
    private void assertTookPlace(TrainingWorkout workout) {
        if (workout.getStatus() == WorkoutStatus.CANCELLED) {
            throw new ApiException(HttpStatus.CONFLICT, "WORKOUT_CANCELLED",
                    "This workout was cancelled, so results cannot be recorded!");
        }
        if (workout.getStatus() == WorkoutStatus.SCHEDULED) {
            throw new ApiException(HttpStatus.CONFLICT, "WORKOUT_NOT_ENDED",
                    "Results can be recorded only after the session has ended!");
        }
    }
}
