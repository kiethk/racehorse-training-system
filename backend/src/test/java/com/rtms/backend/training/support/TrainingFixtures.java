package com.rtms.backend.training.support;

import com.rtms.backend.horse.entity.Horse;
import com.rtms.backend.horse.repository.HorseRepository;
import com.rtms.backend.identity.entity.User;
import com.rtms.backend.identity.repository.RoleRepository;
import com.rtms.backend.identity.repository.UserRepository;
import com.rtms.backend.security.AuthenticatedUser;
import com.rtms.backend.security.Roles;
import com.rtms.backend.stable.entity.Area;
import com.rtms.backend.stable.entity.StableStall;
import com.rtms.backend.stable.repository.AreaRepository;
import com.rtms.backend.stable.repository.StableStallRepository;
import com.rtms.backend.training.entity.Course;
import com.rtms.backend.training.entity.CourseExercise;
import com.rtms.backend.training.entity.Exercise;
import com.rtms.backend.training.entity.TrainingWorkout;
import com.rtms.backend.training.enums.IntensityLevel;
import com.rtms.backend.training.enums.ExerciseCategory;
import com.rtms.backend.training.enums.SurfaceType;
import com.rtms.backend.training.enums.WorkoutStatus;
import com.rtms.backend.training.repository.CourseRepository;
import com.rtms.backend.training.repository.CourseExerciseRepository;
import com.rtms.backend.training.repository.ExerciseRepository;
import com.rtms.backend.training.repository.TrainingWorkoutRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestComponent;

import java.math.BigDecimal;

/**
 * Dữ liệu dựng sẵn cho test huấn luyện: Trainer có một khu, ngựa có chuồng và Groom, bài tập và khóa. Mọi thứ được tạo trong
 * transaction của test nên được rollback.
 * (Ready-made data for the training tests: a Trainer with an area, horses with a stall and a Groom, Exercises and Courses.
 * Everything is created inside the test transaction, so it rolls back.)
 */
@TestComponent
public class TrainingFixtures {

    @Autowired private UserRepository users;
    @Autowired private RoleRepository roles;
    @Autowired private AreaRepository areas;
    @Autowired private StableStallRepository stalls;
    @Autowired private HorseRepository horses;
    @Autowired private ExerciseRepository exercises;
    @Autowired private CourseRepository courses;
    @Autowired private CourseExerciseRepository courseExercises;
    @Autowired private TrainingWorkoutRepository workouts;

    private Area area;
    private int stallNumber = 500;

    /** Trainer mới phụ trách khu A. (A new Trainer in charge of area A.) */
    public User trainer() {
        User trainer = user(Roles.HEAD_TRAINER, "trainer");
        area = areas.findAll().stream().filter(a -> "A".equals(a.getCode())).findFirst().orElseThrow();
        area.setTrainerId(trainer.getId());
        areas.saveAndFlush(area);
        return trainer;
    }

    public AuthenticatedUser principal(User user) {
        return new AuthenticatedUser(user.getId(), user.getEmail(), user.getRole().getName());
    }

    public User user(String role, String label) {
        User user = new User();
        user.setFullName(label);
        user.setEmail("fx-" + label + "-" + System.nanoTime() + "@test.invalid");
        user.setPasswordHash("not-used");
        user.setRole(roles.findByName(role).orElseThrow());
        user.setActive(true);
        return users.saveAndFlush(user);
    }

    /** Ngựa trong khu A với chuồng riêng và một Groom mới. (A horse in area A with its own stall and a new Groom.) */
    public Horse horse(String name) {
        User groom = user(Roles.GROOM, "groom");
        User owner = user(Roles.HORSE_OWNER, "owner");
        StableStall stall = new StableStall();
        stall.setAreaId(area.getId());
        stall.setGroomId(groom.getId());
        stall.setStallNumber(stallNumber);
        stall.setStallCode("FX-" + stallNumber + "-" + System.nanoTime());
        stallNumber++;
        stall = stalls.saveAndFlush(stall);
        Horse horse = new Horse();
        horse.setName(name);
        horse.setOwnerId(owner.getId());
        horse.setCurrentStallId(stall.getId());
        return horses.saveAndFlush(horse);
    }

    public Exercise exercise(User trainer, String name, int minutes) {
        Exercise exercise = new Exercise();
        exercise.setTrainerId(trainer.getId());
        exercise.setCategory(ExerciseCategory.SPEED);
        exercise.setName(name);
        exercise.setSurfaceType(SurfaceType.TURF);
        exercise.setTargetDistanceMeters(BigDecimal.valueOf(800));
        exercise.setIntensityLevel(IntensityLevel.HIGH);
        exercise.setDurationMinutes(minutes);
        return exercises.saveAndFlush(exercise);
    }

    /** Lưu lại một bài đã sửa trong test. (Saves an Exercise changed inside a test.) */
    public Exercise save(Exercise exercise) {
        return exercises.saveAndFlush(exercise);
    }

    public Course course(User trainer, String name, int totalSessions, Exercise... orderedExercises) {
        Course course = new Course();
        course.setName(name);
        course.setTotalSessions(totalSessions);
        course.setCreatedById(trainer.getId());
        course.setTrainerId(trainer.getId());
        course = courses.saveAndFlush(course);
        int order = 1;
        for (Exercise exercise : orderedExercises) {
            CourseExercise link = new CourseExercise();
            link.setCourseId(course.getId());
            link.setExerciseId(exercise.getId());
            link.setOrderIndex(order++);
            courseExercises.saveAndFlush(link);
        }
        return course;
    }

    /**
     * Ghi kết quả cho mọi Workout HELD của các ngựa này, để chúng thành RAN (tính vào Attendance).
     * (Records results on every HELD Workout of these horses, so they become RAN and count in Attendance.)
     */
    public void recordHeldResults(Horse... ofHorses) {
        for (Horse horse : ofHorses) {
            for (TrainingWorkout workout : workouts.findByHorseIdAndSessionIdIsNotNull(horse.getId())) {
                if (workout.getStatus() != WorkoutStatus.HELD) {
                    continue;
                }
                workout.setActualDistanceMeters(BigDecimal.valueOf(1200));
                workout.setActualDurationMinutes(BigDecimal.valueOf(30));
                workout.setTopSpeedKmh(BigDecimal.valueOf(55));
                workout.setAverageSpeedKmh(BigDecimal.valueOf(40));
                workout.setAverageHeartRate(150);
                workout.setMaxHeartRate(200);
                workout.setRecoveryHeartRate(90);
                workout.setPerformanceRating(7);
                workout.setStatus(WorkoutStatus.RAN);
                workouts.saveAndFlush(workout);
            }
        }
    }
}
