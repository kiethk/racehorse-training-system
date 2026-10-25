package com.rtms.backend.training.service;

import com.rtms.backend.config.FarmSchedulePolicy;
import com.rtms.backend.config.TrainingLimits;
import com.rtms.backend.security.AuthenticatedUser;
import com.rtms.backend.training.dto.CreateSubjectRequest;
import com.rtms.backend.training.entity.Subject;
import com.rtms.backend.training.enums.SubjectCategory;
import com.rtms.backend.training.repository.SubjectRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SubjectService {

    public static final int MIN_DURATION_MINUTES = TrainingLimits.MIN_SUBJECT_DURATION_MINUTES;
    public static final int MAX_DURATION_MINUTES = TrainingLimits.MAX_SUBJECT_DURATION_MINUTES;
    private static final int DEFAULT_DURATION_MINUTES = 60;

    private final SubjectRepository subjectRepository;

    public SubjectService(SubjectRepository subjectRepository) {
        this.subjectRepository = subjectRepository;
    }

    public List<Subject> getAllSubjects(SubjectCategory category) {
        return getAllSubjects(null, category, null);
    }

    public List<Subject> getAllSubjects(Long trainerId, SubjectCategory category, AuthenticatedUser currentUser) {
        Long effectiveTrainerId = resolveTrainerId(trainerId, currentUser);
        if (effectiveTrainerId != null) {
            if (category != null) {
                return subjectRepository.findByTrainerIdAndCategory(effectiveTrainerId, category);
            }
            return subjectRepository.findByTrainerId(effectiveTrainerId);
        }
        if (category != null) {
            return subjectRepository.findByCategory(category);
        }
        return subjectRepository.findAll();
    }

    public Subject getSubjectById(Long id) {
        return getSubjectById(id, null);
    }

    public Subject getSubjectById(Long id, AuthenticatedUser currentUser) {
        Subject subject = subjectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Subject not found with id: " + id));

        if (currentUser != null && "HEAD_TRAINER".equals(currentUser.getRole())
                && subject.getTrainerId() != null && !subject.getTrainerId().equals(currentUser.getUserId())) {
            throw new RuntimeException("Subject not found with id: " + id);
        }

        return subject;
    }

    private Long resolveTrainerId(Long trainerId, AuthenticatedUser currentUser) {
        if (currentUser != null && "HEAD_TRAINER".equals(currentUser.getRole())) {
            return currentUser.getUserId();
        }
        return trainerId;
    }

    public Subject createSubject(CreateSubjectRequest request) {
        return createSubject(request, null);
    }

    /**
     * Tạo một bài tập. Chủ của bài là người đang đăng nhập, không bao giờ do client chọn. Hình dạng của request (tên không
     * rỗng, cự ly, enum, độ dài) đã được kiểm ở Bean Validation; ở đây chỉ còn luật cần tra DB.
     * (Creates a subject. The owner is the signed-in user, never chosen by the client. The shape of the request (non-blank
     * name, distance, enums, length) is already checked by Bean Validation; only the rules that need the DB remain here.)
     */
    @Transactional
    public Subject createSubject(CreateSubjectRequest request, AuthenticatedUser currentUser) {
        if (request.getCategory() == null) {
            throw new IllegalArgumentException("Category is required!");
        }

        Long trainerId = currentUser != null ? currentUser.getUserId() : null;
        String name = request.getName().trim();

        if (trainerId != null && subjectRepository.existsByNameIgnoreCaseAndTrainerId(name, trainerId)) {
            throw new IllegalArgumentException("A subject named '" + name + "' already exists for this trainer!");
        }

        Subject subject = new Subject();
        subject.setTrainerId(trainerId);
        subject.setCategory(request.getCategory());
        subject.setName(name);
        subject.setDescription(request.getDescription() == null || request.getDescription().isBlank()
                ? null : request.getDescription().trim());
        subject.setSurfaceType(request.getSurfaceType());
        subject.setTargetDistanceMeters(request.getTargetDistanceMeters());
        subject.setIntensityLevel(request.getIntensityLevel());
        subject.setDurationMinutes(
                request.getDurationMinutes() != null ? request.getDurationMinutes() : DEFAULT_DURATION_MINUTES);

        validateDuration(subject.getDurationMinutes());
        return subjectRepository.save(subject);
    }

    private void validateDuration(int durationMinutes) {
        if (durationMinutes < MIN_DURATION_MINUTES || durationMinutes > MAX_DURATION_MINUTES) {
            throw new IllegalArgumentException(String.format(
                    "Subject duration must be between %d and %d minutes!", MIN_DURATION_MINUTES, MAX_DURATION_MINUTES));
        }
        // Một bài dài hơn cả khung giờ vàng thì không bao giờ xếp được lot.
        // (A subject longer than the whole golden window can never be placed in a lot.)
        if (durationMinutes > FarmSchedulePolicy.goldenWindowMinutes()) {
            throw new IllegalArgumentException(String.format(
                    "Duration of %d minutes exceeds the golden window (%d minutes), so no lot can be scheduled!",
                    durationMinutes, FarmSchedulePolicy.goldenWindowMinutes()));
        }
    }
}
