package com.rtms.backend.training.service;

import com.rtms.backend.security.AuthenticatedUser;
import com.rtms.backend.training.dto.CourseDetailResponse;
import com.rtms.backend.training.dto.CourseSubjectItemRequest;
import com.rtms.backend.training.dto.CreateCourseRequest;
import com.rtms.backend.training.entity.Course;
import com.rtms.backend.training.entity.CourseSubject;
import com.rtms.backend.training.entity.Subject;
import com.rtms.backend.training.repository.CourseRepository;
import com.rtms.backend.training.repository.CourseSubjectRepository;
import com.rtms.backend.training.repository.SubjectRepository;
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
        assertCanSee(course, currentUser);
        return new CourseDetailResponse(course, courseSubjectRepository.findCourseSubjectResponses(id));
    }

    private Long resolveTrainerId(Long trainerId, AuthenticatedUser currentUser) {
        if (currentUser != null && "HEAD_TRAINER".equals(currentUser.getRole())) {
            return currentUser.getUserId();
        }
        return trainerId;
    }

    /** Một Trainer chỉ thấy khóa của mình; khóa của người khác báo "không tìm thấy". (A trainer only sees their own courses; another trainer course is "not found".) */
    private void assertCanSee(Course course, AuthenticatedUser currentUser) {
        if (currentUser != null && "HEAD_TRAINER".equals(currentUser.getRole())
                && course.getTrainerId() != null && !course.getTrainerId().equals(currentUser.getUserId())) {
            throw new RuntimeException("Course not found with id: " + course.getId());
        }
    }

    /**
     * Tạo khóa cùng vòng xoay các bài. Chủ của khóa là người đang đăng nhập, không bao giờ do client chọn.
     * (Creates a course with its subject rotation. The owner is the signed-in user, never chosen by the client.)
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

        List<CourseSubjectItemRequest> rotation = inRotationOrder(request.getSubjects());
        assertSubjectsFitSessions(rotation.size(), request.getTotalSessions());
        assertRotationIsNew(rotation, trainerId);

        Course course = new Course();
        course.setName(name);
        course.setTargetGoal(blankToNull(request.getTargetGoal()));
        course.setTotalSessions(request.getTotalSessions());
        course.setTrainerId(trainerId);
        course.setCreatedById(trainerId);

        Course savedCourse = courseRepository.save(course);
        addSubjects(savedCourse.getId(), request.getSubjects(), rotation, trainerId);

        return new CourseDetailResponse(savedCourse,
                courseSubjectRepository.findCourseSubjectResponses(savedCourse.getId()));
    }

    /**
     * Gắn các bài vào khóa và ĐÁNH SỐ LẠI vòng xoay 1..n theo thứ tự client gửi (orderIndex chỉ dùng để sắp xếp). Nhờ vậy số
     * thứ tự trùng, âm, thiếu hay có khoảng trống đều cho ra một vòng xoay hợp lệ thay vì một lỗi DB.
     * (Attaches the subjects and RENUMBERS the rotation 1..n in the order the client sent (orderIndex only sorts). Duplicate,
     * negative, missing or gapped numbers then give a valid rotation instead of a DB error.)
     */
    private void addSubjects(Long courseId, List<CourseSubjectItemRequest> items,
                             List<CourseSubjectItemRequest> inRotationOrder, Long courseTrainerId) {
        Set<Long> seen = new HashSet<>();
        for (CourseSubjectItemRequest item : items) {
            // Khóa xoay vòng qua danh sách bài, nên mỗi bài chỉ có một lần.
            // (The course rotates through the subject list, so each subject appears once.)
            if (!seen.add(item.getSubjectId())) {
                throw new IllegalArgumentException("Subject #" + item.getSubjectId() + " is already in this course!");
            }
        }

        Map<Long, Subject> subjectsById = subjectRepository.findAllById(seen).stream()
                .collect(Collectors.toMap(Subject::getId, Function.identity()));
        for (Long subjectId : seen) {
            Subject subject = subjectsById.get(subjectId);
            if (subject == null) {
                throw new IllegalArgumentException("Subject not found with id: " + subjectId);
            }
            assertSubjectBelongsTo(subject, courseTrainerId);
        }

        int order = 1;
        for (CourseSubjectItemRequest item : inRotationOrder) {
            CourseSubject courseSubject = new CourseSubject();
            courseSubject.setCourseId(courseId);
            courseSubject.setSubjectId(item.getSubjectId());
            courseSubject.setOrderIndex(order++);
            courseSubjectRepository.save(courseSubject);
        }
    }

    /** Các bài theo thứ tự xoay vòng: theo orderIndex, bài không có số xếp cuối, bằng nhau thì giữ thứ tự client gửi. (Items in rotation order.) */
    private static List<CourseSubjectItemRequest> inRotationOrder(List<CourseSubjectItemRequest> items) {
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
     * (The course rotates through its subjects, one per session, so it cannot have more subjects than sessions: the extra ones would never be trained.)
     */
    private static void assertSubjectsFitSessions(int subjectCount, Integer totalSessions) {
        if (totalSessions != null && subjectCount > totalSessions) {
            throw new IllegalArgumentException("A course cannot have more subjects (" + subjectCount
                    + ") than sessions (" + totalSessions + ")!");
        }
    }

    /**
     * Không tạo khóa có đúng cùng danh sách bài và cùng thứ tự với một khóa đã có của Trainer; khác thứ tự thì được.
     * (A course cannot repeat the exact subject list, in the same order, of one the trainer already has; a different order is fine.)
     */
    private void assertRotationIsNew(List<CourseSubjectItemRequest> rotation, Long trainerId) {
        List<Long> wanted = rotation.stream().map(CourseSubjectItemRequest::getSubjectId).toList();
        List<Course> existing = trainerId != null ? courseRepository.findByTrainerId(trainerId) : courseRepository.findAll();
        if (existing.isEmpty()) return;

        Map<Long, String> namesById = existing.stream().collect(Collectors.toMap(Course::getId, Course::getName));
        Map<Long, List<Long>> subjectsByCourse = new LinkedHashMap<>();
        for (CourseSubject cs : courseSubjectRepository.findByCourseIdInOrderByCourseIdAscOrderIndexAsc(namesById.keySet())) {
            subjectsByCourse.computeIfAbsent(cs.getCourseId(), id -> new ArrayList<>()).add(cs.getSubjectId());
        }
        subjectsByCourse.forEach((courseId, subjects) -> {
            if (subjects.equals(wanted)) {
                throw new IllegalArgumentException("Course '" + namesById.get(courseId)
                        + "' already has exactly these subjects in the same order!");
            }
        });
    }

    private void assertSubjectBelongsTo(Subject subject, Long courseTrainerId) {
        if (courseTrainerId != null && subject.getTrainerId() != null && !courseTrainerId.equals(subject.getTrainerId())) {
            throw new IllegalArgumentException("Subject '" + subject.getName() + "' (#" + subject.getId()
                    + ") does not belong to this trainer!");
        }
    }

    private static String blankToNull(String text) {
        return text == null || text.isBlank() ? null : text.trim();
    }
}
