package com.rtms.backend.training.service;

import com.rtms.backend.training.dto.CourseResponse;
import org.springframework.http.HttpStatus;
import com.rtms.backend.config.ApiException;
import com.rtms.backend.security.AuthenticatedUser;
import com.rtms.backend.security.Roles;
import com.rtms.backend.training.dto.CourseDetailResponse;
import com.rtms.backend.training.dto.CourseExerciseItemRequest;
import com.rtms.backend.training.dto.CreateCourseRequest;
import com.rtms.backend.training.entity.Course;
import com.rtms.backend.training.entity.CourseExercise;
import com.rtms.backend.training.entity.Exercise;
import com.rtms.backend.training.repository.CourseRepository;
import com.rtms.backend.training.repository.CourseExerciseRepository;
import com.rtms.backend.training.repository.ExerciseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
public class CourseService {

    private final CourseRepository courseRepository;
    private final CourseExerciseRepository courseExerciseRepository;
    private final ExerciseRepository exerciseRepository;

    public CourseService(CourseRepository courseRepository,
                         CourseExerciseRepository courseExerciseRepository,
                         ExerciseRepository exerciseRepository) {
        this.courseRepository = courseRepository;
        this.courseExerciseRepository = courseExerciseRepository;
        this.exerciseRepository = exerciseRepository;
    }

    public List<Course> getAllCourses(Long trainerId, AuthenticatedUser currentUser) {
        Long effectiveTrainerId = resolveTrainerId(trainerId, currentUser);
        if (effectiveTrainerId != null) {
            return courseRepository.findByTrainerId(effectiveTrainerId);
        }
        return courseRepository.findAll();
    }

    public CourseDetailResponse getCourseById(Long id, AuthenticatedUser currentUser) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "COURSE_NOT_FOUND", "Course not found!"));
        assertCanSee(course, currentUser);
        return new CourseDetailResponse(CourseResponse.of(course), courseExerciseRepository.findCourseExerciseResponses(id));
    }

    private Long resolveTrainerId(Long trainerId, AuthenticatedUser currentUser) {
        if (currentUser != null && Roles.HEAD_TRAINER.equals(currentUser.getRole())) {
            return currentUser.getUserId();
        }
        return trainerId;
    }

    /** Một Trainer chỉ thấy khóa của mình; khóa của người khác báo "không tìm thấy". (A trainer only sees their own courses; another trainer course is "not found".) */
    private void assertCanSee(Course course, AuthenticatedUser currentUser) {
        if (currentUser != null && Roles.HEAD_TRAINER.equals(currentUser.getRole())
                && course.getTrainerId() != null && !course.getTrainerId().equals(currentUser.getUserId())) {
            throw new ApiException(HttpStatus.NOT_FOUND, "COURSE_NOT_FOUND", "Course not found!");
        }
    }

    /**
     * Tạo khóa cùng vòng xoay các bài. Chủ của khóa là người đang đăng nhập, không bao giờ do client chọn.
     * (Creates a course with its exercise rotation. The owner is the signed-in user, never chosen by the client.)
     */
    @Transactional
    public CourseDetailResponse createCourse(CreateCourseRequest request, AuthenticatedUser currentUser) {
        Long trainerId = currentUser != null ? currentUser.getUserId() : null;
        String name = request.getName().trim();

        boolean taken = trainerId != null
                ? courseRepository.existsByNameIgnoreCaseAndTrainerId(name, trainerId)
                : courseRepository.existsByNameIgnoreCase(name);
        if (taken) {
            throw new IllegalArgumentException("A course named '" + name + "' already exists!");
        }

        List<CourseExerciseItemRequest> rotation = inRotationOrder(request.getExercises());
        assertExercisesFitSessions(rotation.size(), request.getTotalSessions());
        assertRotationIsNew(rotation, trainerId);

        Course course = new Course();
        course.setName(name);
        course.setTargetGoal(blankToNull(request.getTargetGoal()));
        course.setTotalSessions(request.getTotalSessions());
        course.setTrainerId(trainerId);
        course.setCreatedById(trainerId);

        Course savedCourse = courseRepository.save(course);
        addExercises(savedCourse.getId(), request.getExercises(), rotation, trainerId);

        return new CourseDetailResponse(CourseResponse.of(savedCourse),
                courseExerciseRepository.findCourseExerciseResponses(savedCourse.getId()));
    }

    /**
     * Gắn các bài vào khóa và ĐÁNH SỐ LẠI vòng xoay 1..n theo thứ tự client gửi (orderIndex chỉ dùng để sắp xếp). Nhờ vậy số
     * thứ tự trùng, âm, thiếu hay có khoảng trống đều cho ra một vòng xoay hợp lệ thay vì một lỗi DB.
     * (Attaches the exercises and RENUMBERS the rotation 1..n in the order the client sent (orderIndex only sorts). Duplicate,
     * negative, missing or gapped numbers then give a valid rotation instead of a DB error.)
     */
    private void addExercises(Long courseId, List<CourseExerciseItemRequest> items,
                             List<CourseExerciseItemRequest> inRotationOrder, Long courseTrainerId) {
        Set<Long> seen = new HashSet<>();
        for (CourseExerciseItemRequest item : items) {
            // Khóa xoay vòng qua danh sách bài, nên mỗi bài chỉ có một lần.
            // (The course rotates through the exercise list, so each exercise appears once.)
            if (!seen.add(item.getExerciseId())) {
                throw new IllegalArgumentException("Exercise #" + item.getExerciseId() + " is already in this course!");
            }
        }

        Map<Long, Exercise> exercisesById = exerciseRepository.findAllById(seen).stream()
                .collect(Collectors.toMap(Exercise::getId, Function.identity()));
        for (Long exerciseId : seen) {
            Exercise exercise = exercisesById.get(exerciseId);
            if (exercise == null) {
                throw new IllegalArgumentException("Exercise not found with id: " + exerciseId);
            }
            assertExerciseBelongsTo(exercise, courseTrainerId);
        }

        int order = 1;
        for (CourseExerciseItemRequest item : inRotationOrder) {
            CourseExercise courseExercise = new CourseExercise();
            courseExercise.setCourseId(courseId);
            courseExercise.setExerciseId(item.getExerciseId());
            courseExercise.setOrderIndex(order++);
            courseExerciseRepository.save(courseExercise);
        }
    }

    /** Các bài theo thứ tự xoay vòng: theo orderIndex, bài không có số xếp cuối, bằng nhau thì giữ thứ tự client gửi. (Items in rotation order.) */
    private static List<CourseExerciseItemRequest> inRotationOrder(List<CourseExerciseItemRequest> items) {
        return IntStream.range(0, items.size())
                .boxed()
                .sorted(Comparator
                        .comparing((Integer position) -> items.get(position).getOrderIndex() == null)
                        .thenComparing(position -> items.get(position).getOrderIndex() == null
                                ? 0 : items.get(position).getOrderIndex())
                        .thenComparing(Comparator.naturalOrder()))
                .map(items::get)
                .toList();
    }

    /**
     * Khóa xoay vòng qua danh sách bài, mỗi buổi tập một bài, nên số bài không thể nhiều hơn số buổi: bài thừa sẽ không bao giờ được tập.
     * (The course rotates through its exercises, one per session, so it cannot have more exercises than sessions: the extra ones would never be trained.)
     */
    private static void assertExercisesFitSessions(int exerciseCount, Integer totalSessions) {
        if (totalSessions != null && exerciseCount > totalSessions) {
            throw new IllegalArgumentException("A course cannot have more exercises (" + exerciseCount
                    + ") than sessions (" + totalSessions + ")!");
        }
    }

    /**
     * Không tạo khóa có đúng cùng danh sách bài và cùng thứ tự với một khóa đã có của Trainer; khác thứ tự thì được.
     * (A course cannot repeat the exact exercise list, in the same order, of one the trainer already has; a different order is fine.)
     */
    private void assertRotationIsNew(List<CourseExerciseItemRequest> rotation, Long trainerId) {
        List<Long> wanted = rotation.stream().map(CourseExerciseItemRequest::getExerciseId).toList();
        List<Course> existing = trainerId != null ? courseRepository.findByTrainerId(trainerId) : courseRepository.findAll();
        if (existing.isEmpty()) return;

        Map<Long, String> namesById = existing.stream().collect(Collectors.toMap(Course::getId, Course::getName));
        Map<Long, List<Long>> exercisesByCourse = new LinkedHashMap<>();
        for (CourseExercise cs : courseExerciseRepository.findByCourseIdInOrderByCourseIdAscOrderIndexAsc(namesById.keySet())) {
            exercisesByCourse.computeIfAbsent(cs.getCourseId(), id -> new ArrayList<>()).add(cs.getExerciseId());
        }
        exercisesByCourse.forEach((courseId, exercises) -> {
            if (exercises.equals(wanted)) {
                throw new IllegalArgumentException("Course '" + namesById.get(courseId)
                        + "' already has exactly these exercises in the same order!");
            }
        });
    }

    private void assertExerciseBelongsTo(Exercise exercise, Long courseTrainerId) {
        if (courseTrainerId != null && exercise.getTrainerId() != null && !courseTrainerId.equals(exercise.getTrainerId())) {
            throw new IllegalArgumentException("Exercise '" + exercise.getName() + "' (#" + exercise.getId()
                    + ") does not belong to this trainer!");
        }
    }

    private static String blankToNull(String text) {
        return text == null || text.isBlank() ? null : text.trim();
    }
}
