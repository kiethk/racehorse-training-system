package com.rtms.backend.training.service;

import com.rtms.backend.config.ApiException;
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
import com.rtms.backend.training.dto.CreateTrainingGroupRequest;
import com.rtms.backend.training.dto.GroupCandidateResponse;
import com.rtms.backend.training.dto.RenameTrainingGroupRequest;
import com.rtms.backend.training.dto.TrainingGroupResponse;
import com.rtms.backend.training.entity.Course;
import com.rtms.backend.training.entity.GroupPlan;
import com.rtms.backend.training.enums.TrainingDay;
import com.rtms.backend.training.enums.TrainingDecision;
import com.rtms.backend.training.enums.TrainingPlanStatus;
import com.rtms.backend.training.repository.CourseRepository;
import com.rtms.backend.training.repository.GroupPlanRepository;
import com.rtms.backend.training.repository.TrainingGroupMemberRepository;
import com.rtms.backend.training.repository.TrainingGroupRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Quản lý Training Group qua service trên PostgreSQL thật: luật thành viên (BR-09, BR-10, khu của Trainer, được phép
 * tập), đổi tên, chuyển ngựa và truy vấn ứng viên. Mọi dữ liệu nằm trong một transaction rồi rollback.
 * (Training Group management through the service on a real PostgreSQL: membership rules (BR-09, BR-10, the Trainer's
 * area, allowed to train), rename, move, candidate query (disband is in GroupDisbandIntegrationTest). All data lives in one transaction that rolls back.)
 */
@SpringBootTest
@Transactional
class TrainingGroupServiceIntegrationTest {

    @Autowired private TrainingGroupService service;
    @Autowired private UserRepository users;
    @Autowired private RoleRepository roles;
    @Autowired private AreaRepository areas;
    @Autowired private StableStallRepository stalls;
    @Autowired private HorseRepository horses;
    @Autowired private CourseRepository courses;
    @Autowired private GroupPlanRepository plans;
    @Autowired private TrainingGroupRepository groups;
    @Autowired private TrainingGroupMemberRepository members;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private EntityManager entityManager;

    private User trainer;
    private AuthenticatedUser trainerPrincipal;
    private Area area;
    private int stallNumber = 700;

    @BeforeEach
    void buildFixture() {
        trainer = user("HEAD_TRAINER", "trainer");
        trainerPrincipal = new AuthenticatedUser(trainer.getId(), trainer.getEmail(), "HEAD_TRAINER");
        area = areas.findAll().stream().filter(a -> "A".equals(a.getCode())).findFirst().orElseThrow();
        area.setTrainerId(trainer.getId());
        areas.saveAndFlush(area);
        jdbc.execute("SET CONSTRAINTS ALL IMMEDIATE");
    }

    // ------------------------------------------------------------------ create

    @Test
    void aTrainerCreatesAGroupWithHorses() {
        Horse thunder = horse("Thunder", groom("An"));
        Horse blaze = horse("Blaze", groom("Binh"));

        TrainingGroupResponse group = service.createGroup(
                new CreateTrainingGroupRequest("Nhom 1", List.of(thunder.getId(), blaze.getId())), trainerPrincipal);

        assertEquals("Nhom 1", group.name());
        assertEquals(List.of("Thunder", "Blaze"), group.members().stream().map(m -> m.horseName()).toList());
        assertEquals(List.of("An", "Binh"), group.members().stream().map(m -> m.groomName()).toList());
    }

    @Test
    void aGroupCanBeCreatedEmptyAndFilledLater() {
        TrainingGroupResponse empty = service.createGroup(new CreateTrainingGroupRequest("Nhom 1", List.of()), trainerPrincipal);
        Horse thunder = horse("Thunder", groom("An"));

        TrainingGroupResponse filled = service.addHorse(empty.id(), thunder.getId(), trainerPrincipal);

        assertEquals(1, filled.members().size());
    }

    @Test
    void theSeventhHorseIsRefused() {
        TrainingGroupResponse group = service.createGroup(new CreateTrainingGroupRequest("Nhom 1", List.of()), trainerPrincipal);
        for (int i = 0; i < 6; i++) {
            service.addHorse(group.id(), horse("H" + i, groom("G" + i)).getId(), trainerPrincipal);
        }
        Horse seventh = horse("H7", groom("G7"));

        ApiException error = assertThrows(ApiException.class,
                () -> service.addHorse(group.id(), seventh.getId(), trainerPrincipal));
        assertEquals("GROUP_FULL", error.getErrorCode());
    }

    @Test
    void twoHorsesOfTheSameGroomAreRefusedInOneGroup() {
        User an = groom("An");
        Horse first = horse("Thunder", an);
        Horse second = horse("Echo", an);
        TrainingGroupResponse group = service.createGroup(
                new CreateTrainingGroupRequest("Nhom 1", List.of(first.getId())), trainerPrincipal);

        ApiException error = assertThrows(ApiException.class,
                () -> service.addHorse(group.id(), second.getId(), trainerPrincipal));
        assertEquals("GROOM_ALREADY_IN_GROUP", error.getErrorCode());
    }

    @Test
    void theSameGroomMayLeadHorsesInDifferentGroups() {
        User an = groom("An");
        Horse first = horse("Thunder", an);
        Horse second = horse("Echo", an);
        service.createGroup(new CreateTrainingGroupRequest("Nhom 1", List.of(first.getId())), trainerPrincipal);

        TrainingGroupResponse other = service.createGroup(
                new CreateTrainingGroupRequest("Nhom 2", List.of(second.getId())), trainerPrincipal);

        assertEquals(1, other.members().size());
    }

    @Test
    void aHorseAlreadyInAnotherGroupIsRefused() {
        Horse thunder = horse("Thunder", groom("An"));
        service.createGroup(new CreateTrainingGroupRequest("Nhom 1", List.of(thunder.getId())), trainerPrincipal);
        TrainingGroupResponse other = service.createGroup(new CreateTrainingGroupRequest("Nhom 2", List.of()), trainerPrincipal);

        ApiException error = assertThrows(ApiException.class,
                () -> service.addHorse(other.id(), thunder.getId(), trainerPrincipal));
        assertEquals("HORSE_IN_OTHER_GROUP", error.getErrorCode());
    }

    @Test
    void aHorseOutsideTheTrainersAreaIsRefused() {
        User otherTrainer = user("HEAD_TRAINER", "other-trainer");
        Area otherArea = areas.findAll().stream().filter(a -> !"A".equals(a.getCode())).findFirst().orElseThrow();
        otherArea.setTrainerId(otherTrainer.getId());
        areas.saveAndFlush(otherArea);
        Horse foreign = horseIn(otherArea, "Foreign", groom("An"));
        TrainingGroupResponse group = service.createGroup(new CreateTrainingGroupRequest("Nhom 1", List.of()), trainerPrincipal);

        assertThrows(AccessDeniedException.class, () -> service.addHorse(group.id(), foreign.getId(), trainerPrincipal));
    }

    @Test
    void aHorseBlockedFromTrainingIsRefused() {
        Horse resting = horse("Resting", groom("An"));
        resting.setTrainingDecision(TrainingDecision.BLOCKED);
        horses.saveAndFlush(resting);
        TrainingGroupResponse group = service.createGroup(new CreateTrainingGroupRequest("Nhom 1", List.of()), trainerPrincipal);

        ApiException error = assertThrows(ApiException.class,
                () -> service.addHorse(group.id(), resting.getId(), trainerPrincipal));
        assertEquals("TRAINING_BLOCKED", error.getErrorCode());
    }

    @Test
    void aHorseWithoutAGroomIsRefused() {
        Horse orphan = horse("Orphan", null);
        TrainingGroupResponse group = service.createGroup(new CreateTrainingGroupRequest("Nhom 1", List.of()), trainerPrincipal);

        ApiException error = assertThrows(ApiException.class,
                () -> service.addHorse(group.id(), orphan.getId(), trainerPrincipal));
        assertEquals("STALL_NO_GROOM", error.getErrorCode());
    }

    @Test
    void aDuplicateGroupNameIsRefused() {
        service.createGroup(new CreateTrainingGroupRequest("Nhom 1", List.of()), trainerPrincipal);

        ApiException error = assertThrows(ApiException.class,
                () -> service.createGroup(new CreateTrainingGroupRequest("nhom 1", List.of()), trainerPrincipal));
        assertEquals("DUPLICATE_GROUP_NAME", error.getErrorCode());
    }

    // ------------------------------------------------------------ rename, move

    @Test
    void aGroupCanBeRenamed() {
        TrainingGroupResponse group = service.createGroup(new CreateTrainingGroupRequest("Nhom 1", List.of()), trainerPrincipal);

        TrainingGroupResponse renamed = service.renameGroup(group.id(), new RenameTrainingGroupRequest("Nhom sang"), trainerPrincipal);

        assertEquals("Nhom sang", renamed.name());
    }

    @Test
    void removingAHorseKeepsTheOthersInTheGroup() {
        Horse thunder = horse("Thunder", groom("An"));
        Horse blaze = horse("Blaze", groom("Binh"));
        TrainingGroupResponse group = service.createGroup(
                new CreateTrainingGroupRequest("Nhom 1", List.of(thunder.getId(), blaze.getId())), trainerPrincipal);

        TrainingGroupResponse after = service.removeHorse(group.id(), thunder.getId(), trainerPrincipal);

        assertEquals(List.of("Blaze"), after.members().stream().map(m -> m.horseName()).toList());
    }

    @Test
    void movingAHorseLeavesTheOldGroupAndJoinsTheNewOne() {
        Horse thunder = horse("Thunder", groom("An"));
        TrainingGroupResponse from = service.createGroup(
                new CreateTrainingGroupRequest("Nhom 1", List.of(thunder.getId())), trainerPrincipal);
        TrainingGroupResponse to = service.createGroup(new CreateTrainingGroupRequest("Nhom 2", List.of()), trainerPrincipal);

        TrainingGroupResponse moved = service.moveHorse(to.id(), thunder.getId(), trainerPrincipal);

        assertEquals(1, moved.members().size());
        assertEquals(0, members.countByGroupIdAndLeftAtIsNull(from.id()));
    }

    // -------------------------------------------------------------- visibility

    @Test
    void anotherTrainerDoesNotSeeOrChangeTheGroup() {
        TrainingGroupResponse group = service.createGroup(new CreateTrainingGroupRequest("Nhom 1", List.of()), trainerPrincipal);
        User other = user("HEAD_TRAINER", "other");
        AuthenticatedUser otherPrincipal = new AuthenticatedUser(other.getId(), other.getEmail(), "HEAD_TRAINER");

        assertTrue(service.getGroups(otherPrincipal).isEmpty());
        ApiException error = assertThrows(ApiException.class,
                () -> service.renameGroup(group.id(), new RenameTrainingGroupRequest("X"), otherPrincipal));
        assertEquals("GROUP_NOT_FOUND", error.getErrorCode());
    }

    @Test
    void theListShowsTheCurrentCourseOfAGroup() {
        TrainingGroupResponse group = service.createGroup(new CreateTrainingGroupRequest("Nhom 1", List.of()), trainerPrincipal);
        plan(group.id(), TrainingPlanStatus.ACTIVE);

        TrainingGroupResponse listed = service.getGroups(trainerPrincipal).stream()
                .filter(g -> g.id().equals(group.id())).findFirst().orElseThrow();

        assertEquals("ACTIVE", listed.currentPlanStatus());
        assertTrue(listed.currentCourseName().startsWith("Group Course"));
    }

    // -------------------------------------------------------------- candidates

    @Test
    void candidatesExplainWhyAHorseCannotJoin() {
        User an = groom("An");
        Horse member = horse("Thunder", an);
        Horse sameGroom = horse("Echo", an);
        Horse free = horse("Free", groom("Binh"));
        Horse inOther = horse("Taken", groom("Chi"));
        TrainingGroupResponse group = service.createGroup(
                new CreateTrainingGroupRequest("Nhom 1", List.of(member.getId())), trainerPrincipal);
        service.createGroup(new CreateTrainingGroupRequest("Nhom 2", List.of(inOther.getId())), trainerPrincipal);

        List<GroupCandidateResponse> candidates = service.getCandidates(group.id(), trainerPrincipal);

        GroupCandidateResponse echo = byName(candidates, "Echo");
        assertFalse(echo.joinable());
        assertTrue(echo.reason().contains("Thunder"));
        assertTrue(byName(candidates, "Free").joinable());
        assertFalse(byName(candidates, "Taken").joinable());
        assertTrue(candidates.stream().noneMatch(c -> c.horseName().equals("Thunder")));
    }

    // ------------------------------------------------------------------ helpers

    private GroupCandidateResponse byName(List<GroupCandidateResponse> candidates, String name) {
        return candidates.stream().filter(c -> c.horseName().equals(name)).findFirst().orElseThrow();
    }

    private User user(String role, String label) {
        User user = new User();
        user.setFullName(label);
        user.setEmail("tg-" + label + "-" + System.nanoTime() + "@test.invalid");
        user.setPasswordHash("not-used");
        user.setRole(roles.findByName(role).orElseThrow());
        user.setActive(true);
        return users.saveAndFlush(user);
    }

    private User groom(String name) {
        return user("GROOM", name);
    }

    private Horse horse(String name, User groom) {
        return horseIn(area, name, groom);
    }

    private Horse horseIn(Area targetArea, String name, User groom) {
        User owner = user("HORSE_OWNER", "owner");
        StableStall stall = new StableStall();
        stall.setAreaId(targetArea.getId());
        stall.setGroomId(groom == null ? null : groom.getId());
        stall.setStallNumber(stallNumber);
        stall.setStallCode("TG-" + stallNumber + "-" + System.nanoTime());
        stallNumber++;
        stall = stalls.saveAndFlush(stall);
        Horse horse = new Horse();
        horse.setName(name);
        horse.setOwnerId(owner.getId());
        horse.setCurrentStallId(stall.getId());
        return horses.saveAndFlush(horse);
    }

    private GroupPlan plan(Long groupId, TrainingPlanStatus status) {
        Course course = new Course();
        course.setName("Group Course " + System.nanoTime());
        course.setTotalSessions(4);
        course.setCreatedById(trainer.getId());
        course.setTrainerId(trainer.getId());
        course = courses.saveAndFlush(course);

        GroupPlan plan = new GroupPlan();
        plan.setGroupId(groupId);
        plan.setCourseId(course.getId());
        plan.setTrainerId(trainer.getId());
        plan.setStartDate(LocalDate.now().plusDays(10));
        plan.setEndDate(LocalDate.now().plusDays(20));
        Set<TrainingDay> days = new LinkedHashSet<>(List.of(TrainingDay.MONDAY));
        plan.setTrainingDays(days);
        plan.setStatus(status);
        return plans.saveAndFlush(plan);
    }
}
