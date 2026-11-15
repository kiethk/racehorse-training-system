package com.rtms.backend.training.service;

import com.rtms.backend.security.Roles;
import com.rtms.backend.training.dto.CreateExerciseRequest;
import com.rtms.backend.training.entity.Exercise;
import com.rtms.backend.training.enums.IntensityLevel;
import com.rtms.backend.training.enums.ExerciseCategory;
import com.rtms.backend.training.enums.SurfaceType;
import com.rtms.backend.training.repository.ExerciseRepository;
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
class ExerciseServiceTest {

    @Mock
    private ExerciseRepository exerciseRepository;

    private ExerciseService exerciseService;

    @BeforeEach
    void setUp() {
        exerciseService = new ExerciseService(exerciseRepository);
    }

    private CreateExerciseRequest validRequest() {
        CreateExerciseRequest req = new CreateExerciseRequest();
        req.setCategory(ExerciseCategory.ENDURANCE);
        req.setName("45-minute heart-rate run");
        req.setDescription("Keep a moderate pace");
        req.setSurfaceType(SurfaceType.TURF);
        req.setTargetDistanceMeters(BigDecimal.valueOf(1600));
        req.setIntensityLevel(IntensityLevel.MEDIUM);
        req.setDurationMinutes(45);
        return req;
    }

    @Test
    @DisplayName("1. Lists exercises without a category filter")
    void testGetAllExercises_WithoutCategory_ReturnsAll() {
        Exercise s1 = new Exercise();
        s1.setId(1L);
        s1.setName("Warm-up");

        Exercise s2 = new Exercise();
        s2.setId(2L);
        s2.setName("Steady run");

        when(exerciseRepository.findAll()).thenReturn(List.of(s1, s2));

        List<Exercise> result = exerciseService.getAllExercises(null, null, null);

        assertEquals(2, result.size());
        verify(exerciseRepository).findAll();
        verify(exerciseRepository, never()).findByCategory(any());
    }

    @Test
    @DisplayName("2. Lists exercises filtered by category")
    void testGetAllExercises_WithCategory_ReturnsFiltered() {
        Exercise s = new Exercise();
        s.setId(1L);
        s.setCategory(ExerciseCategory.SPEED);
        s.setName("Fitness drill");

        when(exerciseRepository.findByCategory(ExerciseCategory.SPEED)).thenReturn(List.of(s));

        List<Exercise> result = exerciseService.getAllExercises(null, ExerciseCategory.SPEED, null);

        assertEquals(1, result.size());
        assertEquals("Fitness drill", result.get(0).getName());
        verify(exerciseRepository).findByCategory(ExerciseCategory.SPEED);
    }

    @Test
    @DisplayName("3. Gets an exercise by id")
    void testGetExerciseById_Success() {
        Exercise s = new Exercise();
        s.setId(5L);
        s.setName("Short-distance burst");

        when(exerciseRepository.findById(5L)).thenReturn(Optional.of(s));

        Exercise result = exerciseService.getExerciseById(5L, null);

        assertNotNull(result);
        assertEquals(5L, result.getId());
        assertEquals("Short-distance burst", result.getName());
    }

    @Test
    @DisplayName("4. Fails when the exercise id is unknown")
    void testGetExerciseById_NotFound_ThrowsException() {
        when(exerciseRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> exerciseService.getExerciseById(999L, null));
    }

    @Test
    @DisplayName("5. Creates an exercise with a valid duration (15 - 240 minutes)")
    void testCreateExercise_Success() {
        when(exerciseRepository.save(any(Exercise.class))).thenAnswer(inv -> {
            Exercise s = inv.getArgument(0);
            s.setId(88L);
            return s;
        });

        Exercise created = exerciseService.createExercise(validRequest(), null);

        assertNotNull(created);
        assertEquals(88L, created.getId());
        assertEquals(ExerciseCategory.ENDURANCE, created.getCategory());
        assertEquals("45-minute heart-rate run", created.getName());
        assertEquals(45, created.getDurationMinutes());
        verify(exerciseRepository).save(any(Exercise.class));
    }

    @Test
    @DisplayName("5b. A missing duration defaults to 60 minutes")
    void testCreateExercise_DefaultDuration() {
        CreateExerciseRequest req = validRequest();
        req.setDurationMinutes(null);
        when(exerciseRepository.save(any(Exercise.class))).thenAnswer(inv -> inv.getArgument(0));

        assertEquals(60, exerciseService.createExercise(req, null).getDurationMinutes());
    }

    @Test
    @DisplayName("6. An exercise without a category is rejected")
    void testCreateExercise_MissingCategory_ThrowsIllegalArgument() {
        CreateExerciseRequest req = validRequest();
        req.setCategory(null);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> exerciseService.createExercise(req, null));
        assertTrue(ex.getMessage().contains("Category is required"));
        verify(exerciseRepository, never()).save(any());
    }

    @Test
    @DisplayName("7. A duration that is too short (< 15 minutes) is rejected")
    void testCreateExercise_DurationTooShort_ThrowsIllegalArgument() {
        CreateExerciseRequest req = validRequest();
        req.setDurationMinutes(10); // < 15

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> exerciseService.createExercise(req, null));
        assertTrue(ex.getMessage().contains("between 15 and 240 minutes"));
    }

    @Test
    @DisplayName("8. A duration that is too long (> 240 minutes) is rejected")
    void testCreateExercise_DurationTooLong_ThrowsIllegalArgument() {
        CreateExerciseRequest req = validRequest();
        req.setDurationMinutes(300); // > 240

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> exerciseService.createExercise(req, null));
        assertTrue(ex.getMessage().contains("between 15 and 240 minutes"));
    }

    @Test
    @DisplayName("Trainer only views exercises belonging to them")
    void testGetAllExercises_TrainerOnlyViewsOwnExercises() {
        Exercise s1 = new Exercise();
        s1.setId(10L);
        s1.setName("Trainer 9 Exercise");
        s1.setTrainerId(9L);

        com.rtms.backend.security.AuthenticatedUser trainer =
                new com.rtms.backend.security.AuthenticatedUser(9L, "trainer@example.com", Roles.HEAD_TRAINER);

        when(exerciseRepository.findByTrainerId(9L)).thenReturn(List.of(s1));

        List<Exercise> result = exerciseService.getAllExercises(null, null, trainer);

        assertEquals(1, result.size());
        assertEquals("Trainer 9 Exercise", result.get(0).getName());
        verify(exerciseRepository).findByTrainerId(9L);
    }

    @Test
    @DisplayName("Trainer cannot view another trainer's exercise")
    void testGetExerciseById_OtherTrainer_ThrowsException() {
        Exercise other = new Exercise();
        other.setId(50L);
        other.setName("Other Exercise");
        other.setTrainerId(10L); // different trainer

        com.rtms.backend.security.AuthenticatedUser trainer =
                new com.rtms.backend.security.AuthenticatedUser(9L, "trainer@example.com", Roles.HEAD_TRAINER);

        when(exerciseRepository.findById(50L)).thenReturn(Optional.of(other));

        assertThrows(RuntimeException.class, () -> exerciseService.getExerciseById(50L, trainer));
    }

    @Test
    @DisplayName("The owner of a new exercise is the signed-in trainer")
    void testCreateExercise_OwnerIsTheSignedInTrainer() {
        com.rtms.backend.security.AuthenticatedUser trainer =
                new com.rtms.backend.security.AuthenticatedUser(9L, "trainer@example.com", Roles.HEAD_TRAINER);
        when(exerciseRepository.save(any(Exercise.class))).thenAnswer(inv -> {
            Exercise s = inv.getArgument(0);
            s.setId(100L);
            return s;
        });

        Exercise created = exerciseService.createExercise(validRequest(), trainer);

        assertNotNull(created);
        assertEquals(9L, created.getTrainerId());
        verify(exerciseRepository).save(any(Exercise.class));
    }

    @Test
    @DisplayName("A name is trimmed and a duplicate is found whatever its case")
    void testCreateExercise_DuplicateNameIgnoresCaseAndSpaces() {
        com.rtms.backend.security.AuthenticatedUser trainer =
                new com.rtms.backend.security.AuthenticatedUser(9L, "trainer@example.com", Roles.HEAD_TRAINER);
        CreateExerciseRequest req = validRequest();
        req.setName("  45-MINUTE heart-rate run ");
        when(exerciseRepository.existsByNameIgnoreCaseAndTrainerId("45-MINUTE heart-rate run", 9L)).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> exerciseService.createExercise(req, trainer));
        assertTrue(ex.getMessage().contains("already exists"));
        verify(exerciseRepository, never()).save(any());
    }
}
