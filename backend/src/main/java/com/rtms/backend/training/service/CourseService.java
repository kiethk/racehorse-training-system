package com.rtms.backend.training.service;

import com.rtms.backend.security.AuthenticatedUser;
import com.rtms.backend.training.dto.CourseDetailResponse;
import com.rtms.backend.training.dto.CourseSubjectItemRequest;
import com.rtms.backend.training.dto.CourseSubjectResponse;
import com.rtms.backend.training.dto.CreateCourseRequest;
import com.rtms.backend.training.entity.Course;
import com.rtms.backend.training.entity.CourseSubject;
import com.rtms.backend.training.entity.Subject;
import com.rtms.backend.training.repository.CourseRepository;
import com.rtms.backend.training.repository.CourseSubjectRepository;
import com.rtms.backend.training.repository.SubjectRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CourseService {

    private final CourseRepository courseRepository;
    private final CourseSubjectRepository courseSubjectRepository;
    private final SubjectRepository subjectRepository;

    public CourseService(CourseRepository courseRepository,
                         CourseSubjectRepository courseSubjectRepository,
                         SubjectRepository subjectRepository) {
        this.courseRepository = courseRepository;
        this.courseSubjectRepository = courseSubjectRepository;
        this.subjectRepository = subjectRepository;
    }

    public List<Course> getAllCourses() {
        return courseRepository.findAll();
    }

    public List<Course> getAllCourses(Long trainerId, AuthenticatedUser currentUser) {
        Long effectiveTrainerId = resolveTrainerId(trainerId, currentUser);
        if (effectiveTrainerId != null) {
            return courseRepository.findByTrainerId(effectiveTrainerId);
        }
        return courseRepository.findAll();
    }

    public CourseDetailResponse getCourseById(Long id) {
        return getCourseById(id, null);
    }

    public CourseDetailResponse getCourseById(Long id, AuthenticatedUser currentUser) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Course not found with id: " + id));

        if (currentUser != null && "HEAD_TRAINER".equals(currentUser.getRole())
                && course.getTrainerId() != null && !course.getTrainerId().equals(currentUser.getUserId())) {
            throw new RuntimeException("Course not found with id: " + id);
        }

        return new CourseDetailResponse(course, courseSubjectRepository.findCourseSubjectResponses(id));
    }

    private Long resolveTrainerId(Long trainerId, AuthenticatedUser currentUser) {
        if (currentUser != null && "HEAD_TRAINER".equals(currentUser.getRole())) {
            return currentUser.getUserId();
        }
        return trainerId;
    }

    @Transactional
    public CourseDetailResponse createCourse(CreateCourseRequest request, AuthenticatedUser currentUser) {
        Long trainerId = request.getTrainerId() != null
                ? request.getTrainerId()
                : (currentUser != null ? currentUser.getUserId() : null);

        if (trainerId != null) {
            if (courseRepository.existsByNameAndTrainerId(request.getName(), trainerId)) {
                throw new IllegalArgumentException("A course named '" + request.getName() + "' already exists for this trainer!");
            }
        } else {
            if (courseRepository.existsByName(request.getName())) {
                throw new IllegalArgumentException("A course named '" + request.getName() + "' already exists!");
            }
        }

        Course course = new Course();
        course.setName(request.getName());
        course.setDescription(request.getDescription());
        course.setTargetGoal(request.getTargetGoal());
        course.setTotalSessions(request.getTotalSessions());
        course.setTrainerId(trainerId);
        course.setCreatedById(currentUser != null ? currentUser.getUserId() : trainerId);

        Course savedCourse = courseRepository.save(course);
        addSubjects(savedCourse.getId(), request.getSubjects(), trainerId);

        return new CourseDetailResponse(savedCourse,
                courseSubjectRepository.findCourseSubjectResponses(savedCourse.getId()));
    }

    /**
     * Gắn các bài tập vào khóa; thứ tự mặc định là 1, 2, 3... nếu request không nêu orderIndex.
     * (Attaches subjects to the course; the default order is 1, 2, 3... when the request gives no orderIndex.)
     */
    private void addSubjects(Long courseId, List<CourseSubjectItemRequest> items, Long courseTrainerId) {
        if (items == null || items.isEmpty()) {
            return;
        }

        int defaultIndex = 1;
        for (CourseSubjectItemRequest item : items) {
            // Kiểm tra bài tập có tồn tại trong ngân hàng không (check the subject exists in the bank)
            if (!subjectRepository.existsById(item.getSubjectId())) {
                throw new IllegalArgumentException("Subject not found with id: " + item.getSubjectId());
            }

            if (courseTrainerId != null) {
                subjectRepository.findById(item.getSubjectId()).ifPresent(sub -> {
                    if (sub.getTrainerId() != null && !courseTrainerId.equals(sub.getTrainerId())) {
                        throw new IllegalArgumentException("Subject '" + sub.getName() + "' (#" + item.getSubjectId()
                                + ") does not belong to this trainer!");
                    }
                });
            }

            CourseSubject courseSubject = new CourseSubject();
            courseSubject.setCourseId(courseId);
            courseSubject.setSubjectId(item.getSubjectId());
            courseSubject.setOrderIndex(item.getOrderIndex() != null ? item.getOrderIndex() : defaultIndex++);
            courseSubjectRepository.save(courseSubject);
        }
    }

    @Transactional
    public CourseSubjectResponse addSubjectToCourse(Long courseId, CourseSubjectItemRequest request) {
        if (!courseRepository.existsById(courseId)) {
            throw new RuntimeException("Course not found with id: " + courseId);
        }

        Subject subject = subjectRepository.findById(request.getSubjectId())
                .orElseThrow(() -> new IllegalArgumentException("Subject not found with id: " + request.getSubjectId()));

        courseRepository.findById(courseId).ifPresent(course -> {
            if (course.getTrainerId() != null && subject.getTrainerId() != null
                    && !course.getTrainerId().equals(subject.getTrainerId())) {
                throw new IllegalArgumentException("Subject '" + subject.getName() + "' does not belong to this course's trainer!");
            }
        });

        CourseSubject courseSubject = new CourseSubject();
        courseSubject.setCourseId(courseId);
        courseSubject.setSubjectId(request.getSubjectId());
        courseSubject.setOrderIndex(request.getOrderIndex());

        CourseSubject saved = courseSubjectRepository.save(courseSubject);
        return new CourseSubjectResponse(saved.getId(), subject.getId(), subject.getName(),
                subject.getDescription(), subject.getSurfaceType(), subject.getTargetDistanceMeters(),
                subject.getIntensityLevel(), saved.getOrderIndex(), subject.getDurationMinutes());
    }
}
