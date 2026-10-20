package com.rtms.backend.training.service;

import com.rtms.backend.security.AuthenticatedUser;
import com.rtms.backend.training.dto.CourseDetailResponse;
import com.rtms.backend.training.dto.CourseSubjectItemRequest;
import com.rtms.backend.training.dto.CourseSubjectResponse;
import com.rtms.backend.training.dto.CreateCourseRequest;
import com.rtms.backend.training.entity.Course;
import com.rtms.backend.training.entity.CourseSubject;
import com.rtms.backend.training.entity.Subject;
import com.rtms.backend.training.enums.IntensityLevel;
import com.rtms.backend.training.enums.SurfaceType;
import com.rtms.backend.training.repository.CourseRepository;
import com.rtms.backend.training.repository.CourseSubjectRepository;
import com.rtms.backend.training.repository.SubjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CourseServiceTest {

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private CourseSubjectRepository courseSubjectRepository;

    @Mock
    private SubjectRepository subjectRepository;

    private CourseService courseService;

    private final AuthenticatedUser trainerUser = new AuthenticatedUser(9L, "trainer@example.com", "HEAD_TRAINER");

    @BeforeEach
    void setUp() {
        courseService = new CourseService(courseRepository, courseSubjectRepository, subjectRepository);
    }

    /** Một dòng bài tập của khóa đã JOIN sẵn. (A course subject row with the join already done.) */
    private CourseSubjectResponse row(long id, long subjectId, String name, int orderIndex, Integer durationMinutes) {
        return new CourseSubjectResponse(id, subjectId, name, null, SurfaceType.TURF,
                BigDecimal.valueOf(1200), IntensityLevel.MEDIUM, orderIndex, durationMinutes);
    }

    @Test
    @DisplayName("1. Lists every course")
    void testGetAllCourses() {
        Course c1 = new Course();
        c1.setId(1L);
        c1.setName("Beginner course");

        Course c2 = new Course();
        c2.setId(2L);
        c2.setName("Advanced course");

        when(courseRepository.findAll()).thenReturn(List.of(c1, c2));

        List<Course> result = courseService.getAllCourses();

        assertEquals(2, result.size());
        assertEquals("Beginner course", result.get(0).getName());
    }

    @Test
    @DisplayName("2. Course detail returns its subjects in order, loaded with one joined query")
    void testGetCourseById_Success() {
        Long courseId = 10L;
        Course course = new Course();
        course.setId(courseId);
        course.setName("Endurance course");

        when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
        when(courseSubjectRepository.findCourseSubjectResponses(courseId))
                .thenReturn(List.of(row(101L, 50L, "Slow run 20 min", 1, 30)));

        CourseDetailResponse response = courseService.getCourseById(courseId);

        assertNotNull(response);
        assertEquals("Endurance course", response.getCourse().getName());
        assertEquals(1, response.getSubjects().size());
        assertEquals("Slow run 20 min", response.getSubjects().get(0).getSubjectName());
        assertEquals(1, response.getSubjects().get(0).getOrderIndex());
        // Không còn gọi subjectRepository từng bài (no more per-subject lookups)
        verifyNoInteractions(subjectRepository);
    }

    @Test
    @DisplayName("3. Fails when the course id is unknown")
    void testGetCourseById_NotFound_ThrowsException() {
        when(courseRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> courseService.getCourseById(99L));
    }

    @Test
    @DisplayName("4. Creates a course together with its subjects")
    void testCreateCourse_Success() {
        CreateCourseRequest req = new CreateCourseRequest();
        req.setName("Sprint 1200m course");
        req.setDescription("Sprint training");
        req.setTargetGoal("Prepare for the autumn race");
        req.setTotalSessions(12);

        CourseSubjectItemRequest item1 = new CourseSubjectItemRequest();
        item1.setSubjectId(1L);
        item1.setOrderIndex(1);

        CourseSubjectItemRequest item2 = new CourseSubjectItemRequest();
        item2.setSubjectId(2L);
        item2.setOrderIndex(2);

        req.setSubjects(List.of(item1, item2));

        when(courseRepository.existsByNameAndTrainerId("Sprint 1200m course", 9L)).thenReturn(false);
        when(subjectRepository.existsById(1L)).thenReturn(true);
        when(subjectRepository.existsById(2L)).thenReturn(true);

        when(courseRepository.save(any(Course.class))).thenAnswer(inv -> {
            Course c = inv.getArgument(0);
            c.setId(100L);
            return c;
        });
        when(courseSubjectRepository.save(any(CourseSubject.class))).thenAnswer(inv -> {
            CourseSubject cs = inv.getArgument(0);
            cs.setId(cs.getSubjectId() + 500L);
            return cs;
        });
        when(courseSubjectRepository.findCourseSubjectResponses(100L)).thenReturn(List.of(
                row(501L, 1L, "Sprint 400m", 1, 45),
                row(502L, 2L, "Recovery 15 min", 2, 30)));

        CourseDetailResponse res = courseService.createCourse(req, trainerUser);

        assertNotNull(res);
        assertEquals(100L, res.getCourse().getId());
        assertEquals("Sprint 1200m course", res.getCourse().getName());
        assertEquals(2, res.getSubjects().size());
        verify(courseRepository).save(any(Course.class));
        verify(courseSubjectRepository, times(2)).save(any(CourseSubject.class));
    }

    @Test
    @DisplayName("5. Rejects a course whose name already exists")
    void testCreateCourse_DuplicateName_ThrowsIllegalArgument() {
        CreateCourseRequest req = new CreateCourseRequest();
        req.setName("Existing course");

        when(courseRepository.existsByNameAndTrainerId("Existing course", 9L)).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> courseService.createCourse(req, trainerUser));
        assertTrue(ex.getMessage().contains("already exists"));
    }

    @Test
    @DisplayName("6. Rejects a course that references a subject which does not exist")
    void testCreateCourse_SubjectNotFound_ThrowsIllegalArgument() {
        CreateCourseRequest req = new CreateCourseRequest();
        req.setName("Trial course");
        CourseSubjectItemRequest item = new CourseSubjectItemRequest();
        item.setSubjectId(999L);
        req.setSubjects(List.of(item));

        when(courseRepository.existsByNameAndTrainerId("Trial course", 9L)).thenReturn(false);
        when(courseRepository.save(any(Course.class))).thenAnswer(inv -> {
            Course c = inv.getArgument(0);
            c.setId(101L);
            return c;
        });
        when(subjectRepository.existsById(999L)).thenReturn(false);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> courseService.createCourse(req, trainerUser));
        assertTrue(ex.getMessage().contains("Subject not found"));
    }

    @Test
    @DisplayName("7. Adds a subject to a course")
    void testAddSubjectToCourse_Success() {
        Long courseId = 5L;
        CourseSubjectItemRequest req = new CourseSubjectItemRequest();
        req.setSubjectId(20L);
        req.setOrderIndex(3);

        when(courseRepository.existsById(courseId)).thenReturn(true);

        Subject subject = new Subject();
        subject.setId(20L);
        subject.setName("Gallop");
        when(subjectRepository.findById(20L)).thenReturn(Optional.of(subject));

        when(courseSubjectRepository.save(any(CourseSubject.class))).thenAnswer(inv -> {
            CourseSubject cs = inv.getArgument(0);
            cs.setId(777L);
            return cs;
        });

        CourseSubjectResponse res = courseService.addSubjectToCourse(courseId, req);

        assertNotNull(res);
        assertEquals(777L, res.getId());
        assertEquals("Gallop", res.getSubjectName());
        assertEquals(3, res.getOrderIndex());
    }

    @Test
    @DisplayName("8. Adding a subject fails when the course does not exist")
    void testAddSubjectToCourse_CourseNotFound_ThrowsRuntimeException() {
        CourseSubjectItemRequest req = new CourseSubjectItemRequest();
        req.setSubjectId(1L);

        when(courseRepository.existsById(999L)).thenReturn(false);

        assertThrows(RuntimeException.class,
                () -> courseService.addSubjectToCourse(999L, req));
    }

    // =================================================================
    // durationMinutes — con số QUYẾT ĐỊNH ĐỘ DÀI LOT (the number that DECIDES the lot length)
    // =================================================================

    @Test
    @DisplayName("Course detail must return durationMinutes of every subject")
    void testGetCourseById_ReturnsDurationMinutes() {
        Long courseId = 10L;
        Course course = new Course();
        course.setId(courseId);
        course.setName("Sprint 2YO");

        when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
        when(courseSubjectRepository.findCourseSubjectResponses(courseId))
                .thenReturn(List.of(row(101L, 50L, "Warm-up and steady run", 1, 90)));

        CourseDetailResponse response = courseService.getCourseById(courseId);

        // Thiếu trường này thì màn chọn khóa chỉ hiện chữ "phút" trơ trọi, Trainer không biết khóa ngốn bao
        // nhiêu khung giờ vàng.
        // (Without this field the course picker shows a bare "minutes" label and the trainer cannot tell how
        // much golden window the course consumes.)
        assertEquals(90, response.getSubjects().get(0).getDurationMinutes());
    }

    @Test
    @DisplayName("Adding a subject to a course also returns durationMinutes")
    void testAddSubjectToCourse_ReturnsDurationMinutes() {
        Long courseId = 10L;

        Subject sub = new Subject();
        sub.setId(50L);
        sub.setName("Sprint acceleration");
        sub.setDurationMinutes(60);

        CourseSubjectItemRequest req = new CourseSubjectItemRequest();
        req.setSubjectId(50L);
        req.setOrderIndex(2);

        when(courseRepository.existsById(courseId)).thenReturn(true);
        when(subjectRepository.findById(50L)).thenReturn(Optional.of(sub));
        when(courseSubjectRepository.save(any(CourseSubject.class)))
                .thenAnswer(i -> {
                    CourseSubject saved = i.getArgument(0);
                    saved.setId(999L);
                    return saved;
                });

        CourseSubjectResponse res = courseService.addSubjectToCourse(courseId, req);

        assertEquals(60, res.getDurationMinutes());
    }

    @Test
    @DisplayName("Trainer only views courses belonging to them")
    void testGetAllCourses_TrainerOnlyViewsOwnCourses() {
        Course c1 = new Course();
        c1.setId(1L);
        c1.setName("Trainer 9 Course");
        c1.setTrainerId(9L);

        when(courseRepository.findByTrainerId(9L)).thenReturn(List.of(c1));

        List<Course> result = courseService.getAllCourses(null, trainerUser);

        assertEquals(1, result.size());
        assertEquals("Trainer 9 Course", result.get(0).getName());
        verify(courseRepository).findByTrainerId(9L);
    }

    @Test
    @DisplayName("Trainer cannot view another trainer's course detail")
    void testGetCourseById_OtherTrainer_ThrowsException() {
        Course otherCourse = new Course();
        otherCourse.setId(20L);
        otherCourse.setName("Trainer 10 Course");
        otherCourse.setTrainerId(10L); // different trainer

        when(courseRepository.findById(20L)).thenReturn(Optional.of(otherCourse));

        assertThrows(RuntimeException.class, () -> courseService.getCourseById(20L, trainerUser));
    }

    @Test
    @DisplayName("Cannot add another trainer's subject to course")
    void testAddSubjectToCourse_DifferentTrainerSubject_ThrowsException() {
        Course course = new Course();
        course.setId(5L);
        course.setTrainerId(9L);

        Subject otherSubject = new Subject();
        otherSubject.setId(20L);
        otherSubject.setName("Other Trainer Subject");
        otherSubject.setTrainerId(10L); // different trainer

        when(courseRepository.existsById(5L)).thenReturn(true);
        when(courseRepository.findById(5L)).thenReturn(Optional.of(course));
        when(subjectRepository.findById(20L)).thenReturn(Optional.of(otherSubject));

        CourseSubjectItemRequest req = new CourseSubjectItemRequest();
        req.setSubjectId(20L);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> courseService.addSubjectToCourse(5L, req));
        assertTrue(ex.getMessage().contains("does not belong to this course's trainer"));
    }
}
