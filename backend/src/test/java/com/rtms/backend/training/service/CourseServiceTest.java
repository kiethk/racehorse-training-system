package com.rtms.backend.training.service;

import com.rtms.backend.security.AuthenticatedUser;
import com.rtms.backend.training.dto.CourseDetailResponse;
import com.rtms.backend.training.dto.CourseExerciseItemRequest;
import com.rtms.backend.training.dto.CourseExerciseResponse;
import com.rtms.backend.training.dto.CreateCourseRequest;
import com.rtms.backend.training.entity.Course;
import com.rtms.backend.training.entity.CourseExercise;
import com.rtms.backend.training.entity.Exercise;
import com.rtms.backend.training.enums.IntensityLevel;
import com.rtms.backend.training.enums.SurfaceType;
import com.rtms.backend.training.repository.CourseRepository;
import com.rtms.backend.training.repository.CourseExerciseRepository;
import com.rtms.backend.training.repository.ExerciseRepository;
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
    private CourseExerciseRepository courseExerciseRepository;

    @Mock
    private ExerciseRepository exerciseRepository;

    private CourseService courseService;

    private final AuthenticatedUser trainerUser = new AuthenticatedUser(9L, "trainer@example.com", "HEAD_TRAINER");

    @BeforeEach
    void setUp() {
        courseService = new CourseService(courseRepository, courseExerciseRepository, exerciseRepository);
    }

    /** Một dòng bài tập của khóa đã JOIN sẵn. (A course exercise row with the join already done.) */
    private CourseExerciseResponse row(long id, long exerciseId, String name, int orderIndex, Integer durationMinutes) {
        return new CourseExerciseResponse(id, exerciseId, name, null, SurfaceType.TURF,
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
    @DisplayName("2. Course detail returns its exercises in order, loaded with one joined query")
    void testGetCourseById_Success() {
        Long courseId = 10L;
        Course course = new Course();
        course.setId(courseId);
        course.setName("Endurance course");

        when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
        when(courseExerciseRepository.findCourseExerciseResponses(courseId))
                .thenReturn(List.of(row(101L, 50L, "Slow run 20 min", 1, 30)));

        CourseDetailResponse response = courseService.getCourseById(courseId);

        assertNotNull(response);
        assertEquals("Endurance course", response.getCourse().getName());
        assertEquals(1, response.getExercises().size());
        assertEquals("Slow run 20 min", response.getExercises().get(0).getExerciseName());
        assertEquals(1, response.getExercises().get(0).getOrderIndex());
        // Không còn gọi exerciseRepository từng bài (no more per-exercise lookups)
        verifyNoInteractions(exerciseRepository);
    }

    @Test
    @DisplayName("3. Fails when the course id is unknown")
    void testGetCourseById_NotFound_ThrowsException() {
        when(courseRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> courseService.getCourseById(99L));
    }

    @Test
    @DisplayName("4. Creates a course together with its exercises")
    void testCreateCourse_Success() {
        CreateCourseRequest req = new CreateCourseRequest();
        req.setName("Sprint 1200m course");
        req.setTargetGoal("Prepare for the autumn race");
        req.setTotalSessions(12);

        CourseExerciseItemRequest item1 = new CourseExerciseItemRequest();
        item1.setExerciseId(1L);
        item1.setOrderIndex(1);

        CourseExerciseItemRequest item2 = new CourseExerciseItemRequest();
        item2.setExerciseId(2L);
        item2.setOrderIndex(2);

        req.setExercises(List.of(item1, item2));

        when(courseRepository.existsByNameIgnoreCaseAndTrainerId("Sprint 1200m course", 9L)).thenReturn(false);
        when(exerciseRepository.findAllById(any())).thenReturn(List.of(exercise(1L, 9L), exercise(2L, 9L)));

        when(courseRepository.save(any(Course.class))).thenAnswer(inv -> {
            Course c = inv.getArgument(0);
            c.setId(100L);
            return c;
        });
        when(courseExerciseRepository.save(any(CourseExercise.class))).thenAnswer(inv -> {
            CourseExercise cs = inv.getArgument(0);
            cs.setId(cs.getExerciseId() + 500L);
            return cs;
        });
        when(courseExerciseRepository.findCourseExerciseResponses(100L)).thenReturn(List.of(
                row(501L, 1L, "Sprint 400m", 1, 45),
                row(502L, 2L, "Recovery 15 min", 2, 30)));

        CourseDetailResponse res = courseService.createCourse(req, trainerUser);

        assertNotNull(res);
        assertEquals(100L, res.getCourse().getId());
        assertEquals("Sprint 1200m course", res.getCourse().getName());
        assertEquals(2, res.getExercises().size());
        verify(courseRepository).save(any(Course.class));
        verify(courseExerciseRepository, times(2)).save(any(CourseExercise.class));
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
    @DisplayName("6. Rejects a course that references an exercise which does not exist")
    void testCreateCourse_ExerciseNotFound_ThrowsIllegalArgument() {
        CreateCourseRequest req = new CreateCourseRequest();
        req.setName("Trial course");
        CourseExerciseItemRequest item = new CourseExerciseItemRequest();
        item.setExerciseId(999L);
        req.setExercises(List.of(item));

        when(courseRepository.existsByNameIgnoreCaseAndTrainerId("Trial course", 9L)).thenReturn(false);
        when(courseRepository.save(any(Course.class))).thenAnswer(inv -> {
            Course c = inv.getArgument(0);
            c.setId(101L);
            return c;
        });
        when(exerciseRepository.findAllById(any())).thenReturn(List.of());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> courseService.createCourse(req, trainerUser));
        assertTrue(ex.getMessage().contains("Exercise not found"));
    }

    @Test
    @DisplayName("6b. Rejects a course that lists the same exercise twice")
    void testCreateCourse_DuplicateExercise_ThrowsIllegalArgument() {
        CreateCourseRequest req = new CreateCourseRequest();
        req.setName("Trial course");
        CourseExerciseItemRequest first = new CourseExerciseItemRequest();
        first.setExerciseId(1L);
        CourseExerciseItemRequest again = new CourseExerciseItemRequest();
        again.setExerciseId(1L);
        req.setExercises(List.of(first, again));

        when(courseRepository.save(any(Course.class))).thenAnswer(inv -> inv.getArgument(0));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> courseService.createCourse(req, trainerUser));
        assertTrue(ex.getMessage().contains("already in this course"));
        verify(courseExerciseRepository, never()).save(any(CourseExercise.class));
    }

    @Test
    @DisplayName("6c. The rotation is renumbered 1..n: duplicate, negative or missing order numbers cannot reach the DB")
    void testCreateCourse_RenumbersTheRotation() {
        CreateCourseRequest req = new CreateCourseRequest();
        req.setName("Renumbered");
        req.setTotalSessions(6);
        req.setExercises(List.of(item(11L, 7), item(12L, 7), item(13L, null), item(14L, 2)));

        when(exerciseRepository.findAllById(any())).thenReturn(List.of(exercise(11L, 9L), exercise(12L, 9L), exercise(13L, 9L), exercise(14L, 9L)));
        when(courseRepository.save(any(Course.class))).thenAnswer(inv -> {
            Course c = inv.getArgument(0);
            c.setId(102L);
            return c;
        });
        when(courseExerciseRepository.save(any(CourseExercise.class))).thenAnswer(inv -> inv.getArgument(0));

        courseService.createCourse(req, trainerUser);

        ArgumentCaptor<CourseExercise> saved = ArgumentCaptor.forClass(CourseExercise.class);
        verify(courseExerciseRepository, times(4)).save(saved.capture());
        // sorted by the number sent (2, 7, 7), the one without a number last, ties kept in the order sent
        assertEquals(List.of(14L, 11L, 12L, 13L), saved.getAllValues().stream().map(CourseExercise::getExerciseId).toList());
        assertEquals(List.of(1, 2, 3, 4), saved.getAllValues().stream().map(CourseExercise::getOrderIndex).toList());
    }

    @Test
    @DisplayName("6d. The owner of the course is the signed-in trainer, whatever the request says")
    void testCreateCourse_OwnerIsTheSignedInTrainer() {
        CreateCourseRequest req = new CreateCourseRequest();
        req.setName("Mine");
        req.setTotalSessions(6);
        req.setExercises(List.of(item(1L, 1)));
        when(exerciseRepository.findAllById(any())).thenReturn(List.of(exercise(1L, 9L)));
        when(courseRepository.save(any(Course.class))).thenAnswer(inv -> inv.getArgument(0));
        when(courseExerciseRepository.save(any(CourseExercise.class))).thenAnswer(inv -> inv.getArgument(0));

        courseService.createCourse(req, trainerUser);

        ArgumentCaptor<Course> saved = ArgumentCaptor.forClass(Course.class);
        verify(courseRepository).save(saved.capture());
        assertEquals(9L, saved.getValue().getTrainerId());
        assertEquals(9L, saved.getValue().getCreatedById());
    }

    @Test
    @DisplayName("6e. An exercise of another trainer cannot be put in the course")
    void testCreateCourse_ExerciseOfAnotherTrainer() {
        CreateCourseRequest req = new CreateCourseRequest();
        req.setName("Mixed");
        req.setExercises(List.of(item(1L, 1)));
        when(courseRepository.save(any(Course.class))).thenAnswer(inv -> inv.getArgument(0));
        when(exerciseRepository.findAllById(any())).thenReturn(List.of(exercise(1L, 10L)));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> courseService.createCourse(req, trainerUser));
        assertTrue(ex.getMessage().contains("does not belong to this trainer"));
    }

    @Test
    @DisplayName("6f. A course cannot have more exercises than sessions")
    void testCreateCourse_MoreExercisesThanSessions_Rejected() {
        CreateCourseRequest req = new CreateCourseRequest();
        req.setName("Too many");
        req.setTotalSessions(2);
        req.setExercises(List.of(item(1L, 1), item(2L, 2), item(3L, 3)));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> courseService.createCourse(req, trainerUser));
        assertTrue(ex.getMessage().contains("more exercises (3) than sessions (2)"));
        verify(courseRepository, never()).save(any(Course.class));
    }

    @Test
    @DisplayName("6g. A course may have as many exercises as sessions")
    void testCreateCourse_ExercisesEqualSessions_Allowed() {
        CreateCourseRequest req = new CreateCourseRequest();
        req.setName("Exact");
        req.setTotalSessions(2);
        req.setExercises(List.of(item(1L, 1), item(2L, 2)));
        when(exerciseRepository.findAllById(any())).thenReturn(List.of(exercise(1L, 9L), exercise(2L, 9L)));
        when(courseRepository.save(any(Course.class))).thenAnswer(inv -> inv.getArgument(0));
        when(courseExerciseRepository.save(any(CourseExercise.class))).thenAnswer(inv -> inv.getArgument(0));

        assertDoesNotThrow(() -> courseService.createCourse(req, trainerUser));
    }

    @Test
    @DisplayName("6h. Rejects a course with exactly the same exercises in the same order as one the trainer already has")
    void testCreateCourse_SameRotationAsExistingCourse_Rejected() {
        CreateCourseRequest req = new CreateCourseRequest();
        req.setName("Copy");
        req.setTotalSessions(6);
        // sent out of order on purpose: the order numbers, not the array position, make the rotation 5, 6, 7
        req.setExercises(List.of(item(7L, 3), item(5L, 1), item(6L, 2)));
        existingCourseWithRotation(30L, "Original", 5L, 6L, 7L);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> courseService.createCourse(req, trainerUser));
        assertTrue(ex.getMessage().contains("'Original' already has exactly these exercises in the same order"));
        verify(courseRepository, never()).save(any(Course.class));
    }

    @Test
    @DisplayName("6i. The same exercises in a different order, or with a different list, are allowed")
    void testCreateCourse_DifferentOrderOrList_Allowed() {
        existingCourseWithRotation(30L, "Original", 5L, 6L, 7L);
        when(exerciseRepository.findAllById(any())).thenReturn(
                List.of(exercise(5L, 9L), exercise(6L, 9L), exercise(7L, 9L), exercise(8L, 9L)));
        when(courseRepository.save(any(Course.class))).thenAnswer(inv -> inv.getArgument(0));
        when(courseExerciseRepository.save(any(CourseExercise.class))).thenAnswer(inv -> inv.getArgument(0));

        CreateCourseRequest reordered = new CreateCourseRequest();
        reordered.setName("Reordered");
        reordered.setTotalSessions(6);
        reordered.setExercises(List.of(item(7L, 1), item(6L, 2), item(5L, 3)));
        assertDoesNotThrow(() -> courseService.createCourse(reordered, trainerUser));

        CreateCourseRequest longer = new CreateCourseRequest();
        longer.setName("Longer");
        longer.setTotalSessions(6);
        longer.setExercises(List.of(item(5L, 1), item(6L, 2), item(7L, 3), item(8L, 4)));
        assertDoesNotThrow(() -> courseService.createCourse(longer, trainerUser));
    }

    private void existingCourseWithRotation(Long courseId, String name, Long... exerciseIds) {
        Course existing = new Course();
        existing.setId(courseId);
        existing.setName(name);
        existing.setTrainerId(9L);
        when(courseRepository.findByTrainerId(9L)).thenReturn(List.of(existing));
        List<CourseExercise> rows = new java.util.ArrayList<>();
        for (int i = 0; i < exerciseIds.length; i++) {
            CourseExercise row = new CourseExercise();
            row.setCourseId(courseId);
            row.setExerciseId(exerciseIds[i]);
            row.setOrderIndex(i + 1);
            rows.add(row);
        }
        when(courseExerciseRepository.findByCourseIdInOrderByCourseIdAscOrderIndexAsc(any())).thenReturn(rows);
    }

    private static CourseExerciseItemRequest item(Long exerciseId, Integer orderIndex) {
        CourseExerciseItemRequest item = new CourseExerciseItemRequest();
        item.setExerciseId(exerciseId);
        item.setOrderIndex(orderIndex);
        return item;
    }

    private static Exercise exercise(Long id, Long trainerId) {
        Exercise exercise = new Exercise();
        exercise.setId(id);
        exercise.setName("Exercise " + id);
        exercise.setTrainerId(trainerId);
        return exercise;
    }

    @Test
    @DisplayName("Course detail must return durationMinutes of every exercise")
    void testGetCourseById_ReturnsDurationMinutes() {
        Long courseId = 10L;
        Course course = new Course();
        course.setId(courseId);
        course.setName("Sprint 2YO");

        when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
        when(courseExerciseRepository.findCourseExerciseResponses(courseId))
                .thenReturn(List.of(row(101L, 50L, "Warm-up and steady run", 1, 90)));

        CourseDetailResponse response = courseService.getCourseById(courseId);

        // Thiếu trường này thì màn chọn khóa chỉ hiện chữ "phút" trơ trọi, Trainer không biết khóa ngốn bao
        // nhiêu khung giờ vàng.
        // (Without this field the course picker shows a bare "minutes" label and the trainer cannot tell how
        // much golden window the course consumes.)
        assertEquals(90, response.getExercises().get(0).getDurationMinutes());
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
