package com.rtms.backend.training.service;

import com.rtms.backend.config.ApiException;
import com.rtms.backend.identity.entity.User;
import com.rtms.backend.security.AuthenticatedUser;
import com.rtms.backend.training.dto.CreateTrainingGroupRequest;
import com.rtms.backend.training.dto.GroupPlanDetailResponse;
import com.rtms.backend.training.dto.GroupPlanRequest;
import com.rtms.backend.training.dto.PlanSessionResponse;
import com.rtms.backend.training.dto.TrainingGroupResponse;
import com.rtms.backend.training.entity.Course;
import com.rtms.backend.training.enums.TrainingDay;
import com.rtms.backend.training.repository.GroupPlanRepository;
import com.rtms.backend.training.repository.TrainingGroupMemberRepository;
import com.rtms.backend.training.support.MutableClock;
import com.rtms.backend.training.support.TestClockConfig;
import com.rtms.backend.training.support.TrainingFixtures;
import com.rtms.backend.training.entity.TrainingGroup;
import com.rtms.backend.training.repository.TrainingGroupRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Savepoint;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

/**
 * Giải tán nhóm qua service: hủy kế hoạch chưa đóng theo luật Cancel plan, buổi đang chạy vẫn thành Held đúng giờ, ngựa rời nhóm,
 * nhóm chưa từng có kế hoạch thì bị xóa hẳn, tên dùng lại được và nhóm đã giải tán không nhận ngựa hay kế hoạch mới.
 * (Disbanding a group through the service: unfinished plans are cancelled with the Cancel plan rules, a running Session still
 * becomes Held on time, horses leave, a group that never had a plan is removed, the name can be reused and a Disbanded group takes
 * no horse or new plan.)
 */
@SpringBootTest
@Import({TestClockConfig.class, TrainingFixtures.class})
@Transactional
class GroupDisbandIntegrationTest {

    private static final List<TrainingDay> MON_WED_FRI =
            List.of(TrainingDay.MONDAY, TrainingDay.WEDNESDAY, TrainingDay.FRIDAY);

    @Autowired private TrainingGroupService groupService;
    @Autowired private GroupPlanService planService;
    @Autowired private SessionSettlementService settlement;
    @Autowired private TrainingFixtures fixtures;
    @Autowired private MutableClock clock;
    @Autowired private TrainingGroupMemberRepository members;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private TrainingGroupRepository groups;
    @Autowired private GroupPlanRepository plans;
    @Autowired private EntityManager entityManager;
    @Autowired private DataSource dataSource;
    @MockitoSpyBean private GroupWorkoutSyncService groupSync;

    private User trainerUser;
    private AuthenticatedUser trainer;
    private LocalDate monday;

    @BeforeEach
    void buildFixture() {
        clock.reset();
        jdbc.execute("SET CONSTRAINTS ALL IMMEDIATE");
        trainerUser = fixtures.trainer();
        trainer = fixtures.principal(trainerUser);
        monday = LocalDate.now().plusDays(14);
        while (monday.getDayOfWeek() != DayOfWeek.MONDAY) {
            monday = monday.plusDays(1);
        }
    }

    @AfterEach
    void restoreClock() {
        clock.reset();
    }

    @Test
    void disbandingCancelsUnfinishedPlansFreesHorsesAndKeepsTheRunningSessionAsHeld() {
        TrainingGroupResponse group = groupWithHorses("Nhom 1", "Thunder", "Blaze");
        Long horseId = group.members().get(0).horseId();
        GroupPlanDetailResponse plan = assignCourse(group);
        clock.set(monday.atTime(6, 15));

        groupService.disbandGroup(group.id(), trainer);

        GroupPlanDetailResponse after = planService.getPlan(plan.plan().id(), trainer);
        assertEquals("CANCELLED", after.plan().status());
        assertEquals(List.of("SCHEDULED", "CANCELLED", "CANCELLED"),
                after.sessions().stream().map(PlanSessionResponse::status).toList());
        assertTrue(members.findByHorseId(horseId).isEmpty());
        assertEquals(0, members.countByGroupId(group.id()));

        clock.set(monday.atTime(12, 0));
        settlement.settle();
        assertEquals("HELD", planService.getPlan(plan.plan().id(), trainer).sessions().get(0).status());
    }

    @Test
    void aDisbandedGroupLeavesTheLiveListAndShowsInTheDisbandedList() {
        TrainingGroupResponse group = groupWithHorses("Nhom 1", "Thunder");
        assignCourse(group);

        groupService.disbandGroup(group.id(), trainer);

        assertTrue(groupService.getGroups(trainer).stream().noneMatch(g -> g.id().equals(group.id())));
        List<TrainingGroupResponse> disbanded = groupService.getDisbandedGroups(trainer);
        assertEquals(1, disbanded.size());
        assertTrue(disbanded.get(0).disbanded());
        assertEquals("Nhom 1", disbanded.get(0).name());
        assertTrue(disbanded.get(0).members().isEmpty());
    }

    @Test
    void aGroupThatNeverHadAPlanIsRemovedWithoutATrace() {
        TrainingGroupResponse group = groupWithHorses("Test", "Thunder");

        groupService.disbandGroup(group.id(), trainer);

        assertTrue(groupService.getGroups(trainer).isEmpty());
        assertTrue(groupService.getDisbandedGroups(trainer).isEmpty());
        assertTrue(members.findByHorseId(group.members().get(0).horseId()).isEmpty());
    }

    @Test
    void theNameOfADisbandedGroupCanBeReused() {
        TrainingGroupResponse group = groupWithHorses("Nhom 1", "Thunder");
        assignCourse(group);
        groupService.disbandGroup(group.id(), trainer);

        TrainingGroupResponse again = groupService.createGroup(new CreateTrainingGroupRequest("Nhom 1", List.of()), trainer);

        assertEquals("Nhom 1", again.name());
        assertFalse(again.disbanded());
    }

    @Test
    void aDisbandedGroupRefusesNewHorsesNewPlansAndASecondDisband() {
        TrainingGroupResponse group = groupWithHorses("Nhom 1", "Thunder");
        Course course = course();
        planService.createPlan(new GroupPlanRequest(group.id(), course.getId(), monday, MON_WED_FRI), trainer);
        groupService.disbandGroup(group.id(), trainer);
        Long newHorse = fixtures.horse("Echo").getId();

        assertEquals("GROUP_DISBANDED", assertThrows(ApiException.class,
                () -> groupService.addHorse(group.id(), newHorse, trainer)).getErrorCode());
        assertEquals("GROUP_DISBANDED", assertThrows(ApiException.class, () -> planService.createPlan(
                new GroupPlanRequest(group.id(), course.getId(), monday.plusDays(30), MON_WED_FRI), trainer)).getErrorCode());
        assertEquals("GROUP_DISBANDED", assertThrows(ApiException.class,
                () -> groupService.disbandGroup(group.id(), trainer)).getErrorCode());
    }

    @Test
    void anotherTrainerCannotDisbandTheGroup() {
        TrainingGroupResponse group = groupWithHorses("Nhom 1", "Thunder");
        AuthenticatedUser other = fixtures.principal(fixtures.user("HEAD_TRAINER", "other"));

        assertEquals("GROUP_NOT_FOUND", assertThrows(ApiException.class,
                () -> groupService.disbandGroup(group.id(), other)).getErrorCode());
        assertEquals(1, groupService.getGroups(trainer).get(0).members().size());
    }

    @Test
    void theDisbandingMomentComesFromTheClock() {
        TrainingGroupResponse group = groupWithHorses("Nhom 1", "Thunder");
        assignCourse(group);
        clock.set(monday.atTime(6, 15));

        groupService.disbandGroup(group.id(), trainer);

        assertEquals(monday.atTime(6, 15), groups.findById(group.id()).orElseThrow().getDisbandedAt());
    }

    @Test
    void aFailureWhileDisbandingLeavesTheGroupItsHorsesAndItsPlansUntouched() throws Exception {
        TrainingGroupResponse group = groupWithHorses("Nhom 1", "Thunder", "Blaze");
        GroupPlanDetailResponse plan = assignCourse(group);
        doThrow(new IllegalStateException("boom")).when(groupSync).exitGroup(any());

        // The test transaction cannot roll back on its own, so a JDBC savepoint stands in for the request's transaction:
        // the failure comes after the plans were cancelled, and rolling back to the savepoint must undo all of it.
        entityManager.flush();
        Connection connection = DataSourceUtils.getConnection(dataSource);
        Savepoint beforeRequest = connection.setSavepoint();
        assertThrows(IllegalStateException.class, () -> groupService.disbandGroup(group.id(), trainer));
        connection.rollback(beforeRequest);
        entityManager.clear();

        assertNull(groups.findById(group.id()).orElseThrow().getDisbandedAt());
        assertEquals(2, members.countByGroupId(group.id()));
        assertEquals("UPCOMING", plans.findById(plan.plan().id()).orElseThrow().getStatus().name());
        assertEquals(List.of("SCHEDULED", "SCHEDULED", "SCHEDULED"),
                planService.getPlan(plan.plan().id(), trainer).sessions().stream().map(PlanSessionResponse::status).toList());
    }

    @Test
    void liveGroupNamesAreUniquePerTrainerWhateverTheirCase() {
        TrainingGroup first = new TrainingGroup();
        first.setTrainerId(trainerUser.getId());
        first.setName("Alpha");
        groups.saveAndFlush(first);
        TrainingGroup second = new TrainingGroup();
        second.setTrainerId(trainerUser.getId());
        second.setName("alpha");

        assertThrows(DataIntegrityViolationException.class, () -> groups.saveAndFlush(second));
    }

    // ------------------------------------------------------------------ helpers

    private TrainingGroupResponse groupWithHorses(String name, String... horseNames) {
        List<Long> ids = Arrays.stream(horseNames).map(n -> fixtures.horse(n).getId()).toList();
        return groupService.createGroup(new CreateTrainingGroupRequest(name, ids), trainer);
    }

    private Course course() {
        return fixtures.course(trainerUser, "Sprint " + System.nanoTime(), 3, fixtures.subject(trainerUser, "S1", 30));
    }

    private GroupPlanDetailResponse assignCourse(TrainingGroupResponse group) {
        return planService.createPlan(new GroupPlanRequest(group.id(), course().getId(), monday, MON_WED_FRI), trainer);
    }
}
