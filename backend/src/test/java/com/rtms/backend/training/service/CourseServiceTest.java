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
import org.mockito.ArgumentCaptor;
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
        req.setTargetGoal("Prepare for the autumn race");
        req.setTotalSessions(12);

        CourseSubjectItemRequest item1 = new CourseSubjectItemRequest();
        item1.setSubjectId(1L);
        item1.setOrderIndex(1);

        CourseSubjectItemRequest item2 = new CourseSubjectItemRequest();
        item2.setSubjectId(2L);
        item2.setOrderIndex(2);

        req.setSubjects(List.of(item1, item2));

        when(courseRepository.existsByNameIgnoreCaseAndTrainerId("Sprint 1200m course", 9L)).thenReturn(false);
        when(subjectRepository.findAllById(any())).thenReturn(List.of(subject(1L, 9L), subject(2L, 9L)));

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
        req.setName("  Existing course ");

        when(courseRepository.existsByNameIgnoreCaseAndTrainerId("Existing course", 9L)).thenReturn(true);

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

        when(courseRepository.existsByNameIgnoreCaseAndTrainerId("Trial course", 9L)).thenReturn(false);
        when(courseRepository.save(any(Course.class))).thenAnswer(inv -> {
            Course c = inv.getArgument(0);
            c.setId(101L);
            return c;
        });
        when(subjectRepository.findAllById(any())).thenReturn(List.of());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> courseService.createCourse(req, trainerUser));
        assertTrue(ex.getMessage().contains("Subject not found"));
    }

    @Test
    @DisplayName("6b. Rejects a course that lists the same subject twice")
    void testCreateCourse_DuplicateSubject_ThrowsIllegalArgument() {
        CreateCourseRequest req = new CreateCourseRequest();
        req.setName("Trial course");
        CourseSubjectItemRequest first = new CourseSubjectItemRequest();
        first.setSubjectId(1L);
        CourseSubjectItemRequest again = new CourseSubjectItemRequest();
        again.setSubjectId(1L);
        req.setSubjects(List.of(first, again));

        when(courseRepository.save(any(Course.class))).thenAnswer(inv -> inv.getArgument(0));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> courseService.createCourse(req, trainerUser));
        assertTrue(ex.getMessage().contains("already in this course"));
        verify(courseSubjectRepository, never()).save(any(CourseSubject.class));
    }

    @Test
    @DisplayName("6c. The rotation is renumbered 1..n: duplicate, negative or missing order numbers cannot reach the DB")
    void testCreateCourse_RenumbersTheRotation() {
        CreateCourseRequest req = new CreateCourseRequest();
        req.setName("Renumbered");
        req.setTotalSessions(6);
        req.setSubjects(List.of(item(11L, 7), item(12L, 7), item(13L, null), item(14L, 2)));

        when(subjectRepository.findAllById(any())).thenReturn(List.of(subject(11L, 9L), subject(12L, 9L), subject(13L, 9L), subject(14L, 9L)));
        when(courseRepository.save(any(Course.class))).thenAnswer(inv -> {
            Course c = inv.getArgument(0);
            c.setId(102L);
            return c;
        });
        when(courseSubjectRepository.save(any(CourseSubject.class))).thenAnswer(inv -> inv.getArgument(0));

        courseService.createCourse(req, trainerUser);

        ArgumentCaptor<CourseSubject> saved = ArgumentCaptor.forClass(CourseSubject.class);
        verify(courseSubjectRepository, times(4)).save(saved.capture());
        // sorted by the number sent (2, 7, 7), the one without a number last, ties kept in the order sent
        assertEquals(List.of(14L, 11L, 12L, 13L), saved.getAllValues().stream().map(CourseSubject::getSubjectId).toList());
        assertEquals(List.of(1, 2, 3, 4), saved.getAllValues().stream().map(CourseSubject::getOrderIndex).toList());
    }

    @Test
    @DisplayName("6d. The owner of the course is the signed-in trainer, whatever the request says")
    void testCreateCourse_OwnerIsTheSignedInTrainer() {
        CreateCourseRequest req = new CreateCourseRequest();
        req.setName("Mine");
        req.setTotalSessions(6);
        req.setSubjects(List.of(item(1L, 1)));
        when(subjectRepository.findAllById(any())).thenReturn(List.of(subject(1L, 9L)));
        when(courseRepository.save(any(Course.class))).thenAnswer(inv -> inv.getArgument(0));
        when(courseSubjectRepository.save(any(CourseSubject.class))).thenAnswer(inv -> inv.getArgument(0));

        courseService.createCourse(req, trainerUser);

        ArgumentCaptor<Course> saved = ArgumentCaptor.forClass(Course.class);
        verify(courseRepository).save(saved.capture());
        assertEquals(9L, saved.getValue().getTrainerId());
        assertEquals(9L, saved.getValue().getCreatedById());
    }

    @Test
    @DisplayName("6e. A subject of another trainer cannot be put in the course")
    void testCreateCourse_SubjectOfAnotherTrainer() {
        CreateCourseRequest req = new CreateCourseRequest();
        req.setName("Mixed");
        req.setSubjects(List.of(item(1L, 1)));
        when(courseRepository.save(any(Course.class))).thenAnswer(inv -> inv.getArgument(0));
        when(subjectRepository.findAllById(any())).thenReturn(List.of(subject(1L, 10L)));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> courseService.createCourse(req, trainerUser));
        assertTrue(ex.getMessage().contains("does not belong to this trainer"));
    }

    private static CourseSubjectItemRequest item(Long subjectId, Integer orderIndex) {
        CourseSubjectItemRequest item = new CourseSubjectItemRequest();
        item.setSubjectId(subjectId);
        item.setOrderIndex(orderIndex);
        return item;
    }

    private static Subject subject(Long id, Long trainerId) {
        Subject subject = new Subject();
        subject.setId(id);
        subject.setName("Subject " + id);
        subject.setTrainerId(trainerId);
        return subject;
    }

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
}
