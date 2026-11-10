package com.rtms.backend.training.service;

import com.rtms.backend.config.FarmSchedulePolicy;
import com.rtms.backend.config.TrainingLimits;
import com.rtms.backend.security.AuthenticatedUser;
import com.rtms.backend.training.dto.CreateExerciseRequest;
import com.rtms.backend.training.entity.Exercise;
import com.rtms.backend.training.enums.ExerciseCategory;
import com.rtms.backend.training.repository.ExerciseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ExerciseService {

    public static final int MIN_DURATION_MINUTES = TrainingLimits.MIN_EXERCISE_DURATION_MINUTES;
    public static final int MAX_DURATION_MINUTES = TrainingLimits.MAX_EXERCISE_DURATION_MINUTES;
    private static final int DEFAULT_DURATION_MINUTES = 60;

    private final ExerciseRepository exerciseRepository;

    public ExerciseService(ExerciseRepository exerciseRepository) {
        this.exerciseRepository = exerciseRepository;
    }

    public List<Exercise> getAllExercises(ExerciseCategory category) {
        return getAllExercises(null, category, null);
    }

    public List<Exercise> getAllExercises(Long trainerId, ExerciseCategory category, AuthenticatedUser currentUser) {
        Long effectiveTrainerId = resolveTrainerId(trainerId, currentUser);
        if (effectiveTrainerId != null) {
            if (category != null) {
                return exerciseRepository.findByTrainerIdAndCategory(effectiveTrainerId, category);
            }
            return exerciseRepository.findByTrainerId(effectiveTrainerId);
        }
        if (category != null) {
            return exerciseRepository.findByCategory(category);
        }
        return exerciseRepository.findAll();
    }

    public Exercise getExerciseById(Long id) {
        return getExerciseById(id, null);
    }

    public Exercise getExerciseById(Long id, AuthenticatedUser currentUser) {
        Exercise exercise = exerciseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Exercise not found with id: " + id));

        if (currentUser != null && "HEAD_TRAINER".equals(currentUser.getRole())
                && exercise.getTrainerId() != null && !exercise.getTrainerId().equals(currentUser.getUserId())) {
            throw new RuntimeException("Exercise not found with id: " + id);
        }

        return exercise;
    }

    private Long resolveTrainerId(Long trainerId, AuthenticatedUser currentUser) {
        if (currentUser != null && "HEAD_TRAINER".equals(currentUser.getRole())) {
            return currentUser.getUserId();
        }
        return trainerId;
    }

    public Exercise createExercise(CreateExerciseRequest request) {
        return createExercise(request, null);
    }

    /**
     * Tạo một bài tập. Chủ của bài là người đang đăng nhập, không bao giờ do client chọn. Hình dạng của request (tên không
     * rỗng, cự ly, enum, độ dài) đã được kiểm ở Bean Validation; ở đây chỉ còn luật cần tra DB.
     * (Creates an exercise. The owner is the signed-in user, never chosen by the client. The shape of the request (non-blank
     * name, distance, enums, length) is already checked by Bean Validation; only the rules that need the DB remain here.)
     */
    @Transactional
    public Exercise createExercise(CreateExerciseRequest request, AuthenticatedUser currentUser) {
        if (request.getCategory() == null) {
            throw new IllegalArgumentException("Category is required!");
        }

        Long trainerId = currentUser != null ? currentUser.getUserId() : null;
        String name = request.getName().trim();

        if (trainerId != null && exerciseRepository.existsByNameIgnoreCaseAndTrainerId(name, trainerId)) {
            throw new IllegalArgumentException("An exercise named '" + name + "' already exists for this trainer!");
        }

        Exercise exercise = new Exercise();
        exercise.setTrainerId(trainerId);
        exercise.setCategory(request.getCategory());
        exercise.setName(name);
        exercise.setDescription(request.getDescription() == null || request.getDescription().isBlank()
                ? null : request.getDescription().trim());
        exercise.setSurfaceType(request.getSurfaceType());
        exercise.setTargetDistanceMeters(request.getTargetDistanceMeters());
        exercise.setIntensityLevel(request.getIntensityLevel());
        exercise.setDurationMinutes(
                request.getDurationMinutes() != null ? request.getDurationMinutes() : DEFAULT_DURATION_MINUTES);

        validateDuration(exercise.getDurationMinutes());
        return exerciseRepository.save(exercise);
    }

    private void validateDuration(int durationMinutes) {
        if (durationMinutes < MIN_DURATION_MINUTES || durationMinutes > MAX_DURATION_MINUTES) {
            throw new IllegalArgumentException(String.format(
                    "Exercise duration must be between %d and %d minutes!", MIN_DURATION_MINUTES, MAX_DURATION_MINUTES));
        }
        // Một bài dài hơn cả khung giờ vàng thì không bao giờ xếp được session.
        // (An exercise longer than the whole golden window can never be placed in a session.)
        if (durationMinutes > FarmSchedulePolicy.goldenWindowMinutes()) {
            throw new IllegalArgumentException(String.format(
                    "Duration of %d minutes exceeds the golden window (%d minutes), so no session can be scheduled!",
                    durationMinutes, FarmSchedulePolicy.goldenWindowMinutes()));
        }
    }
}
