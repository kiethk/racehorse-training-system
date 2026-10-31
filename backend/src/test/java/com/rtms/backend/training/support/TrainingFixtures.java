package com.rtms.backend.training.support;

import com.rtms.backend.horse.entity.Horse;
import com.rtms.backend.horse.repository.HorseRepository;
import com.rtms.backend.identity.entity.User;
import com.rtms.backend.identity.repository.RoleRepository;
import com.rtms.backend.identity.repository.UserRepository;
import com.rtms.backend.security.AuthenticatedUser;
import com.rtms.backend.stable.entity.Area;
import com.rtms.backend.stable.entity.StableStall;
import com.rtms.backend.stable.repository.AreaRepository;
import com.rtms.backend.stable.repository.StableStallRepository;
import com.rtms.backend.training.entity.Course;
import com.rtms.backend.training.entity.CourseSubject;
import com.rtms.backend.training.entity.Subject;
import com.rtms.backend.training.enums.IntensityLevel;
import com.rtms.backend.training.enums.SubjectCategory;
import com.rtms.backend.training.enums.SurfaceType;
import com.rtms.backend.training.repository.CourseRepository;
import com.rtms.backend.training.repository.CourseSubjectRepository;
import com.rtms.backend.training.repository.SubjectRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestComponent;

import java.math.BigDecimal;

/**
 * Dữ liệu dựng sẵn cho test huấn luyện: Trainer có một khu, ngựa có chuồng và Groom, bài tập và khóa. Mọi thứ được tạo trong
 * transaction của test nên được rollback.
 * (Ready-made data for the training tests: a Trainer with an area, horses with a stall and a Groom, Subjects and Courses.
 * Everything is created inside the test transaction, so it rolls back.)
 */
@TestComponent
public class TrainingFixtures {

    @Autowired private UserRepository users;
    @Autowired private RoleRepository roles;
    @Autowired private AreaRepository areas;
    @Autowired private StableStallRepository stalls;
    @Autowired private HorseRepository horses;
    @Autowired private SubjectRepository subjects;
    @Autowired private CourseRepository courses;
    @Autowired private CourseSubjectRepository courseSubjects;

    private Area area;
    private int stallNumber = 500;

    /** Trainer mới phụ trách khu A. (A new Trainer in charge of area A.) */
    public User trainer() {
        User trainer = user("HEAD_TRAINER", "trainer");
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
        User groom = user("GROOM", "groom");
        User owner = user("HORSE_OWNER", "owner");
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

    public Subject subject(User trainer, String name, int minutes) {
        Subject subject = new Subject();
        subject.setTrainerId(trainer.getId());
        subject.setCategory(SubjectCategory.SPEED);
        subject.setName(name);
        subject.setSurfaceType(SurfaceType.TURF);
        subject.setTargetDistanceMeters(BigDecimal.valueOf(800));
        subject.setIntensityLevel(IntensityLevel.HIGH);
        subject.setDurationMinutes(minutes);
        return subjects.saveAndFlush(subject);
    }

    public Course course(User trainer, String name, int totalSessions, Subject... orderedSubjects) {
        Course course = new Course();
        course.setName(name);
        course.setTotalSessions(totalSessions);
        course.setCreatedById(trainer.getId());
        course.setTrainerId(trainer.getId());
        course = courses.saveAndFlush(course);
        int order = 1;
        for (Subject subject : orderedSubjects) {
            CourseSubject link = new CourseSubject();
            link.setCourseId(course.getId());
            link.setSubjectId(subject.getId());
            link.setOrderIndex(order++);
            courseSubjects.saveAndFlush(link);
        }
        return course;
    }
}
