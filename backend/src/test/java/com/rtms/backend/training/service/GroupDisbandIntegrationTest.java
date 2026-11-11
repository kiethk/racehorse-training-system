package com.rtms.backend.training.service;

import com.rtms.backend.config.ApiException;
import com.rtms.backend.identity.entity.User;
import com.rtms.backend.security.AuthenticatedUser;
import com.rtms.backend.security.Roles;
import com.rtms.backend.training.dto.CreateTrainingGroupRequest;
import com.rtms.backend.training.dto.GroupMemberResponse;
import com.rtms.backend.training.dto.CourseRunDetailResponse;
import com.rtms.backend.training.dto.CourseRunRequest;
import com.rtms.backend.training.dto.SessionSummaryResponse;
import com.rtms.backend.training.dto.TrainingGroupResponse;
import com.rtms.backend.training.entity.Course;
import com.rtms.backend.training.enums.TrainingDay;
import com.rtms.backend.training.repository.CourseRunRepository;
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
 * Giải tán nhóm qua service: hủy kế hoạch chưa đóng theo luật Cancel course run, buổi đang chạy vẫn thành Held đúng giờ, ngựa rời nhóm,
 * nhóm chưa từng có kế hoạch thì bị xóa hẳn, tên dùng lại được và nhóm đã giải tán không nhận ngựa hay kế hoạch mới.
 * (Disbanding a group through the service: unfinished course runs are cancelled with the Cancel course run rules, a running Session still
 * becomes Held on time, horses leave, a group that never had a course run is removed, the name can be reused and a Disbanded group takes
 * no horse or new course run.)
 */
@SpringBootTest
@Import({TestClockConfig.class, TrainingFixtures.class})
@Transactional
class GroupDisbandIntegrationTest {

    private static final List<TrainingDay> MON_WED_FRI =
            List.of(TrainingDay.MONDAY, TrainingDay.WEDNESDAY, TrainingDay.FRIDAY);

    @Autowired private TrainingGroupService groupService;
    @Autowired private CourseRunService courseRunService;
    @Autowired private SessionSettlementService settlement;
    @Autowired private TrainingFixtures fixtures;
    @Autowired private MutableClock clock;
    @Autowired private TrainingGroupMemberRepository members;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private TrainingGroupRepository groups;
    @Autowired private CourseRunRepository courseRuns;
    @Autowired private EntityManager entityManager;
    @Autowired private DataSource dataSource;
    @MockitoSpyBean private GroupMemberSync groupSync;

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
    void disbandingCancelsUnfinishedCourseRunsFreesHorsesAndKeepsTheRunningSessionAsHeld() {
        TrainingGroupResponse group = groupWithHorses("Nhom 1", "Thunder", "Blaze");
        Long horseId = group.members().get(0).horseId();
        CourseRunDetailResponse courseRun = assignCourse(group);
        clock.set(monday.atTime(6, 15));

        groupService.disbandGroup(group.id(), trainer);

        CourseRunDetailResponse after = courseRunService.getCourseRun(courseRun.courseRun().id(), trainer);
        assertEquals("CANCELLED", after.courseRun().status());
        assertEquals(List.of("SCHEDULED", "CANCELLED", "CANCELLED"),
                after.sessions().stream().map(SessionSummaryResponse::status).toList());
        assertTrue(members.findByHorseIdAndLeftAtIsNull(horseId).isEmpty());
        assertEquals(0, members.countByGroupIdAndLeftAtIsNull(group.id()));

        clock.set(monday.atTime(12, 0));
        settlement.settle();
        assertEquals("HELD", courseRunService.getCourseRun(courseRun.courseRun().id(), trainer).sessions().get(0).status());
    }

    @Test
    void aDisbandedGroupLeavesTheLiveListAndShowsTheHorsesItHadWhenDisbanded() {
        TrainingGroupResponse group = groupWithHorses("Nhom 1", "Thunder", "Blaze");
        assignCourse(group);
        groupService.removeHorse(group.id(), group.members().get(1).horseId(), trainer);
        clock.set(monday.minusDays(1).atTime(9, 0));

        groupService.disbandGroup(group.id(), trainer);

        assertTrue(groupService.getGroups(trainer).stream().noneMatch(g -> g.id().equals(group.id())));
        List<TrainingGroupResponse> disbanded = groupService.getDisbandedGroups(trainer);
        assertEquals(1, disbanded.size());
        assertTrue(disbanded.get(0).disbanded());
        assertEquals("Nhom 1", disbanded.get(0).name());
        assertEquals(List.of("Thunder"), disbanded.get(0).members().stream().map(GroupMemberResponse::horseName).toList());
    }

    @Test
    void aGroupThatNeverHadACourseRunIsRemovedWithoutATrace() {
        TrainingGroupResponse group = groupWithHorses("Test", "Thunder");

        groupService.disbandGroup(group.id(), trainer);

        assertTrue(groupService.getGroups(trainer).isEmpty());
        assertTrue(groupService.getDisbandedGroups(trainer).isEmpty());
        assertTrue(members.findByHorseIdAndLeftAtIsNull(group.members().get(0).horseId()).isEmpty());
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
    void aDisbandedGroupRefusesNewHorsesNewCourseRunsAndASecondDisband() {
        TrainingGroupResponse group = groupWithHorses("Nhom 1", "Thunder");
        Course course = course();
        courseRunService.createCourseRun(new CourseRunRequest(group.id(), course.getId(), monday, MON_WED_FRI), trainer);
        groupService.disbandGroup(group.id(), trainer);
        Long newHorse = fixtures.horse("Echo").getId();

        assertEquals("GROUP_DISBANDED", assertThrows(ApiException.class,
                () -> groupService.addHorse(group.id(), newHorse, trainer)).getErrorCode());
        assertEquals("GROUP_DISBANDED", assertThrows(ApiException.class, () -> courseRunService.createCourseRun(
                new CourseRunRequest(group.id(), course.getId(), monday.plusDays(30), MON_WED_FRI), trainer)).getErrorCode());
        assertEquals("GROUP_DISBANDED", assertThrows(ApiException.class,
                () -> groupService.disbandGroup(group.id(), trainer)).getErrorCode());
    }

    @Test
    void anotherTrainerCannotDisbandTheGroup() {
        TrainingGroupResponse group = groupWithHorses("Nhom 1", "Thunder");
        AuthenticatedUser other = fixtures.principal(fixtures.user(Roles.HEAD_TRAINER, "other"));

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
    void aFailureWhileDisbandingLeavesTheGroupItsHorsesAndItsCourseRunsUntouched() throws Exception {
        TrainingGroupResponse group = groupWithHorses("Nhom 1", "Thunder", "Blaze");
        CourseRunDetailResponse courseRun = assignCourse(group);
        doThrow(new IllegalStateException("boom")).when(groupSync).exitGroup(any());

        // The test transaction cannot roll back on its own, so a JDBC savepoint stands in for the request's transaction:
        // the failure comes after the course runs were cancelled, and rolling back to the savepoint must undo all of it.
        entityManager.flush();
        Connection connection = DataSourceUtils.getConnection(dataSource);
        Savepoint beforeRequest = connection.setSavepoint();
        assertThrows(IllegalStateException.class, () -> groupService.disbandGroup(group.id(), trainer));
        connection.rollback(beforeRequest);
        entityManager.clear();

        assertNull(groups.findById(group.id()).orElseThrow().getDisbandedAt());
        assertEquals(2, members.countByGroupIdAndLeftAtIsNull(group.id()));
        assertEquals("UPCOMING", courseRuns.findById(courseRun.courseRun().id()).orElseThrow().getStatus().name());
        assertEquals(List.of("SCHEDULED", "SCHEDULED", "SCHEDULED"),
                courseRunService.getCourseRun(courseRun.courseRun().id(), trainer).sessions().stream().map(SessionSummaryResponse::status).toList());
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
        return fixtures.course(trainerUser, "Sprint " + System.nanoTime(), 3, fixtures.exercise(trainerUser, "S1", 30));
    }

    private CourseRunDetailResponse assignCourse(TrainingGroupResponse group) {
        return courseRunService.createCourseRun(new CourseRunRequest(group.id(), course().getId(), monday, MON_WED_FRI), trainer);
    }
}
