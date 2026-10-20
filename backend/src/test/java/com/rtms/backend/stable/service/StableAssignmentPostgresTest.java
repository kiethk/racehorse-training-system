package com.rtms.backend.stable.service;

import com.rtms.backend.horse.repository.HorseRepository;
import com.rtms.backend.identity.repository.*;
import com.rtms.backend.security.AuthenticatedUser;
import com.rtms.backend.stable.dto.*;
import com.rtms.backend.stable.repository.*;
import com.rtms.backend.training.service.TrainingWorkoutService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

/** Never enable against a real application database. These tests intentionally commit fixtures. */
@SpringBootTest(properties = {"spring.datasource.url=${RTMS_STABLE_TEST_DB_URL}", "spring.config.import="})
@ActiveProfiles("local")
@EnabledIfEnvironmentVariable(named = "RTMS_STABLE_TEST_DB_URL",
        matches = "jdbc:postgresql://localhost:5433/rtms_stable_test_[a-zA-Z0-9_]+")
class StableAssignmentPostgresTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired StableAssignmentService assignments;
    @Autowired AreaService areaService;
    @Autowired AreaRepository areas;
    @Autowired StableStallRepository stalls;
    @Autowired UserRepository users;
    @Autowired GroomProfileRepository profiles;
    @Autowired HorseRepository horses;
    @Autowired TrainingWorkoutService workouts;
    @Autowired EntityManager em;
    @Autowired AreaTrainerAssignmentPolicy trainerPolicy;
    @Autowired com.rtms.backend.identity.service.StaffManagementService staffManagement;
    @Autowired PlatformTransactionManager transactionManager;
    @Autowired WebApplicationContext web;
    long trainer1, trainer2, groom1, groom2, area1, area2, quarantine, course;
    String tag;

    @BeforeEach void fixtures() {
        assertTrue(System.getenv("RTMS_STABLE_TEST_DB_URL").contains("/rtms_stable_test_"));
        tag = UUID.randomUUID().toString().replace("-", "").substring(0, 9);
        trainer1 = employee("HEAD_TRAINER", "t1"); trainer2 = employee("HEAD_TRAINER", "t2");
        groom1 = employee("GROOM", "g1"); groom2 = employee("GROOM", "g2");
        jdbc.update("INSERT INTO groom_profiles(user_id) VALUES (?), (?)", groom1, groom2);
        area1 = area("A", "REGULAR", trainer1); area2 = area("B", "REGULAR", trainer2);
        quarantine = area("Q", "QUARANTINE", null);
        course = jdbc.queryForObject("INSERT INTO courses(name,total_sessions,trainer_id) VALUES (?,8,?) RETURNING id",
                Long.class, "Stable test course " + tag, trainer1);
    }
    long employee(String role, String suffix) {
        return jdbc.queryForObject("""
            INSERT INTO users(full_name,email,password_hash,role_id,is_active)
            SELECT ?,?,'unused',id,true FROM roles WHERE name=? RETURNING id
            """, Long.class, "Stable test " + tag + suffix, tag + suffix + "@test.invalid", role);
    }
    long area(String suffix, String type, Long trainer) {
        String code = "I" + tag + suffix;
        long id = jdbc.queryForObject("INSERT INTO areas(code,name,type,trainer_id) VALUES (?,?,?,?) RETURNING id",
                Long.class, code, code, type, trainer);
        jdbc.update("""
            INSERT INTO stable_stalls(area_id,stall_number,stall_code,status)
            SELECT ?,n,?||LPAD(n::text,2,'0'),'AVAILABLE' FROM generate_series(1,12) n
            """, id, code);
        return id;
    }
    long stall(long area, int number) {
        return jdbc.queryForObject("SELECT id FROM stable_stalls WHERE area_id=? AND stall_number=?", Long.class, area, number);
    }
    void owner(long area, int block, Long groom) {
        jdbc.update("UPDATE stable_stalls SET groom_id=? WHERE area_id=? AND stall_number BETWEEN ? AND ?",
                groom, area, (block - 1) * 3 + 1, block * 3);
    }
    long horse(long area, int number) {
        long stall = stall(area, number);
        long id = jdbc.queryForObject("""
            INSERT INTO horses(name,current_status,training_decision,current_stall_id)
            VALUES (?,'ELIGIBLE','ALLOWED',?) RETURNING id
            """, Long.class, "Stable horse " + tag + number, stall);
        jdbc.update("UPDATE stable_stalls SET status='OCCUPIED' WHERE id=?", stall);
        return id;
    }
    BlockGroomAssignmentDto edit(long area, int block, Long groom, Long expected) {
        return new BlockGroomAssignmentDto(area, block, groom, expected);
    }
    BatchStableAssignmentRequest request(BlockGroomAssignmentDto... edits) {
        return new BatchStableAssignmentRequest(null, List.of(edits));
    }
    long plan(long horse) {
        return jdbc.queryForObject("""
            INSERT INTO horse_training_plans(horse_id,course_id,trainer_id,start_date,end_date,status)
            VALUES (?,?,?,CURRENT_DATE,CURRENT_DATE+30,'ACTIVE') RETURNING id
            """, Long.class, horse, course, trainer1);
    }
    long lot() {
        return jdbc.queryForObject("""
            INSERT INTO training_lots(trainer_id,subject_id,lot_date,start_time,end_time)
            SELECT ?,id,CURRENT_DATE+1,'08:00','09:00' FROM subjects ORDER BY id LIMIT 1 RETURNING id
            """, Long.class, trainer1);
    }
    void workout(long horse, long lot, long groom, String status) {
        jdbc.update("INSERT INTO training_workouts(horse_id,plan_id,lot_id,assigned_to_id,status) VALUES (?,?,?,?,?)",
                horse, plan(horse), lot, groom, status);
    }
    Long assigned(long area, int number) {
        return jdbc.queryForObject("SELECT groom_id FROM stable_stalls WHERE area_id=? AND stall_number=?", Long.class, area, number);
    }
    StableAssignmentService independentService(GroomProfileRepository profileRepository) {
        return new StableAssignmentService(areas, stalls, users, profileRepository, horses, workouts, em, trainerPolicy);
    }
    String emptyAssignmentJson(long target, long groom) {
        return "{\"groomAssignments\":[{\"areaId\":" + target + ",\"blockNumber\":1,\"groomId\":" + groom
                + ",\"expectedGroomIds\":[null,null,null]}]}";
    }
    UsernamePasswordAuthenticationToken auth(boolean manager) {
        var principal = new AuthenticatedUser(trainer1, "stable-test@test.invalid", manager ? "CLUB_MANAGER" : "HEAD_TRAINER");
        var permissions = manager ? List.of(new SimpleGrantedAuthority("AREA_UPDATE"), new SimpleGrantedAuthority("STALL_GROOM_ASSIGN"))
                : List.of(new SimpleGrantedAuthority("AREA_VIEW"));
        return new UsernamePasswordAuthenticationToken(principal, "unused", permissions);
    }

    @Test void httpEnforcesPermissionsAndParsesNullBaseline() throws Exception {
        var mvc = webAppContextSetup(web).apply(springSecurity()).build();
        String body = emptyAssignmentJson(area1, groom1);
        mvc.perform(put("/api/areas/assignments").with(authentication(auth(false))).contentType("application/json").content(body))
                .andExpect(status().isForbidden());
        mvc.perform(put("/api/areas/assignments").with(authentication(auth(true))).contentType("application/json")
                .content("{\"groomAssignments\":[{\"areaId\":" + area1 + ",\"blockNumber\":1,\"groomId\":" + groom1 + "}]}"))
                .andExpect(status().isBadRequest());
        mvc.perform(put("/api/areas/assignments").with(authentication(auth(true))).contentType("application/json").content(body))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.updatedStalls.length()").value(3));
        mvc.perform(put("/api/areas/assignments").with(authentication(auth(true))).contentType("application/json").content(body))
                .andExpect(status().isConflict());
    }

    @Test void httpClientCannotDisableExpectedTrainerCheck() throws Exception {
        var mvc = webAppContextSetup(web).apply(springSecurity()).build();
        String body = "{\"trainerAssignments\":[{\"areaId\":" + area1 + ",\"trainerId\":" + trainer2
                + ",\"expectedTrainerId\":null,\"expectedTrainerIdSet\":false}]}";
        mvc.perform(put("/api/areas/assignments").with(authentication(auth(true))).contentType("application/json").content(body))
                .andExpect(status().isConflict());
    }

    BatchStableAssignmentRequest trainerChange(Long next) {
        return new BatchStableAssignmentRequest(List.of(new AreaTrainerAssignmentDto(area1, next, trainer1)), null);
    }
    Long currentTrainer(long areaId) {
        return jdbc.queryForObject("SELECT trainer_id FROM areas WHERE id=?", Long.class, areaId);
    }
    AreaTrainerChangeCheck trainerCheck(long areaId) {
        return trainerPolicy.checks().stream().filter(c -> c.areaId()==areaId).findFirst().orElseThrow();
    }

    @Test void emptyAreaWithoutTrainingCanChangeAndRemoveTrainer() {
        assertTrue(trainerCheck(area1).canChange());
        assignments.applyAssignments(trainerChange(trainer2));
        assertEquals(trainer2, currentTrainer(area1));
        assignments.applyAssignments(new BatchStableAssignmentRequest(
                List.of(new AreaTrainerAssignmentDto(area1, null, trainer2)), null));
        assertNull(currentTrainer(area1));
    }

    @Test void occupiedAreaRejectsTrainerSwapAndTheWholeMixedBatch() {
        owner(area1, 1, groom1); long h = horse(area1, 1);
        // A stale AVAILABLE flag must not hide the real horse reference.
        jdbc.update("UPDATE stable_stalls SET status='AVAILABLE' WHERE id=?", stall(area1,1));
        var batch = new BatchStableAssignmentRequest(
                List.of(new AreaTrainerAssignmentDto(area2, trainer1, trainer2), new AreaTrainerAssignmentDto(area1, trainer2, trainer1)),
                List.of(edit(area1, 1, groom2, groom1)));
        assertEquals(409, assertThrows(ResponseStatusException.class, () -> assignments.applyAssignments(batch)).getStatusCode().value());
        assertEquals(trainer1, currentTrainer(area1)); assertEquals(trainer2, currentTrainer(area2));
        assertEquals(groom1, assigned(area1,1));
        assertEquals(stall(area1,1), jdbc.queryForObject("SELECT current_stall_id FROM horses WHERE id=?",Long.class,h));
        assertFalse(trainerCheck(area1).canChange());
        assertTrue(trainerCheck(area1).reasons().stream().anyMatch(r -> r.contains("Area has horses")));
    }

    @Test void occupiedAreaRejectsInitialTrainerAssignmentAndRemoval() {
        horse(area1, 1);
        assertEquals(409, assertThrows(ResponseStatusException.class, () -> assignments.applyAssignments(trainerChange(null))).getStatusCode().value());
        jdbc.update("UPDATE areas SET trainer_id=NULL WHERE id=?",area1);
        var initial = new BatchStableAssignmentRequest(List.of(new AreaTrainerAssignmentDto(area1,trainer1,null)),null);
        assertEquals(409, assertThrows(ResponseStatusException.class, () -> assignments.applyAssignments(initial)).getStatusCode().value());
        assertNull(currentTrainer(area1));
    }

    @ParameterizedTest @ValueSource(strings={"RESERVED","OCCUPIED"})
    void occupiedOrReservedFlagBlocksTrainerChangeEvenWithoutHorse(String status) {
        jdbc.update("UPDATE stable_stalls SET status=? WHERE id=?",status,stall(area1,1));
        assertFalse(trainerCheck(area1).canChange());
        assertEquals(409, assertThrows(ResponseStatusException.class, () -> assignments.applyAssignments(trainerChange(trainer2))).getStatusCode().value());
    }

    @ParameterizedTest @ValueSource(strings={"UPCOMING","ACTIVE"})
    void emptyAreaCannotLeaveTrainerWhoseSharedScheduleStillHasPlans(String status) {
        long h = horse(area2,1), p = plan(h);
        jdbc.update("UPDATE horse_training_plans SET status=? WHERE id=?",status,p);
        assertFalse(trainerCheck(area1).canChange());
        assertEquals(409, assertThrows(ResponseStatusException.class, () -> assignments.applyAssignments(trainerChange(trainer2))).getStatusCode().value());
        assertEquals(trainer1,currentTrainer(area1));
    }

    @Test void scheduledLotAloneBlocksEmptyAreaAndCompletedOrCancelledLotsDoNot() {
        long l = lot();
        assertFalse(trainerCheck(area1).canChange());
        assertEquals(409, assertThrows(ResponseStatusException.class, () -> assignments.applyAssignments(trainerChange(trainer2))).getStatusCode().value());
        jdbc.update("UPDATE training_lots SET status='COMPLETED' WHERE id=?",l);
        long cancelled = lot(); jdbc.update("UPDATE training_lots SET status='CANCELLED' WHERE id=?",cancelled);
        assertTrue(trainerCheck(area1).canChange());
        assignments.applyAssignments(trainerChange(trainer2));
        assertEquals(trainer2,currentTrainer(area1));
        assertEquals(trainer1,jdbc.queryForObject("SELECT trainer_id FROM training_lots WHERE id=?",Long.class,l));
    }

    @Test void unfinishedWorkoutBlocksEvenWhenItsPlanAndLotWereMarkedCompleted() {
        long h = horse(area2,1), l = lot(); workout(h,l,groom1,"SCHEDULED");
        jdbc.update("UPDATE horse_training_plans SET status='COMPLETED' WHERE horse_id=?",h);
        jdbc.update("UPDATE training_lots SET status='COMPLETED' WHERE id=?",l);
        assertFalse(trainerCheck(area1).canChange());
        assertEquals(409, assertThrows(ResponseStatusException.class, () -> assignments.applyAssignments(trainerChange(trainer2))).getStatusCode().value());
        jdbc.update("UPDATE training_workouts SET status='COMPLETED' WHERE horse_id=?",h);
        assertTrue(trainerCheck(area1).canChange());
        assignments.applyAssignments(trainerChange(trainer2));
        assertEquals(trainer1,jdbc.queryForObject("SELECT trainer_id FROM horse_training_plans WHERE horse_id=?",Long.class,h));
        assertEquals(groom1,jdbc.queryForObject("SELECT assigned_to_id FROM training_workouts WHERE horse_id=?",Long.class,h));
    }

    @Test void cancelledTrainingDoesNotBlockEmptyAreaAndIsNotRewritten() {
        long h = horse(area2,1), l = lot(); workout(h,l,groom1,"CANCELLED");
        jdbc.update("UPDATE horse_training_plans SET status='CANCELLED' WHERE horse_id=?",h);
        jdbc.update("UPDATE training_lots SET status='CANCELLED' WHERE id=?",l);
        assertTrue(trainerCheck(area1).canChange());
        assignments.applyAssignments(trainerChange(trainer2));
        assertEquals(trainer1,jdbc.queryForObject("SELECT trainer_id FROM horse_training_plans WHERE horse_id=?",Long.class,h));
        assertEquals("CANCELLED",jdbc.queryForObject("SELECT status FROM training_workouts WHERE horse_id=?",String.class,h));
    }

    @Test void eligibilityReadIsAdvisoryAndBothWriteEndpointsRecheckIt() throws Exception {
        assertTrue(trainerCheck(area1).canChange()); horse(area1,1);
        var mvc = webAppContextSetup(web).apply(springSecurity()).build();
        String edit = "{\"areaId\":"+area1+",\"trainerId\":"+trainer2+",\"expectedTrainerId\":"+trainer1+"}";
        mvc.perform(put("/api/areas/"+area1+"/trainer").with(authentication(auth(true)))
                .contentType("application/json").content(edit)).andExpect(status().isConflict());
        mvc.perform(put("/api/areas/assignments").with(authentication(auth(true)))
                .contentType("application/json").content("{\"trainerAssignments\":["+edit+"]}"))
                .andExpect(status().isConflict());
        mvc.perform(get("/api/areas/trainer-assignment-checks").with(authentication(auth(false))))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/areas/trainer-assignment-checks").with(authentication(auth(true))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data").isArray());
        assertEquals(trainer1,currentTrainer(area1));
    }

    @Test void creatingTrainerCannotBypassEmptyAreaPolicy() {
        jdbc.update("UPDATE areas SET trainer_id=NULL WHERE id IN (?,?)",area1,area2);
        horse(area1,1);
        jdbc.update("UPDATE stable_stalls SET status='RESERVED' WHERE id=?",stall(area2,1));
        var req = new com.rtms.backend.identity.dto.StaffCreationRequest();
        req.setFullName("Trainer auto-check "+tag); req.setEmail(tag+"new-trainer@test.invalid");
        req.setPassword("LocalTest@2026"); req.setRole("HEAD_TRAINER"); req.setCertificationNumber("CERT-"+tag);
        var created = staffManagement.createStaff(req);
        assertNotNull(created.getUserId());
        assertFalse(created.getAssignedAreaIds().contains(area1)); assertFalse(created.getAssignedAreaIds().contains(area2));
        assertNull(currentTrainer(area1)); assertNull(currentTrainer(area2));
        for (Long id : created.getAssignedAreaIds()) assertTrue(trainerCheck(id).canChange());
    }

    @Test void occupiedBlockCannotLoseGroomAndEmptyBlockCan() {
        owner(area1, 1, groom1); horse(area1, 2);
        var ex = assertThrows(ResponseStatusException.class, () -> assignments.applyAssignments(request(edit(area1, 1, null, groom1))));
        assertEquals(409, ex.getStatusCode().value()); assertEquals(groom1, assigned(area1, 1));
        owner(area1, 2, groom2);
        assignments.applyAssignments(request(edit(area1, 2, null, groom2)));
        assertNull(assigned(area1, 4));
    }

    @Test void mixedBlockCannotHideFourthStallOrOverwriteStaleMiddle() {
        owner(area1, 1, groom2);
        jdbc.update("UPDATE stable_stalls SET groom_id=? WHERE id=?", groom1, stall(area1, 1));
        var capacity = assertThrows(ResponseStatusException.class, () -> assignments.applyAssignments(request(edit(area2, 1, groom1, null))));
        assertEquals(400, capacity.getStatusCode().value()); assertNull(assigned(area2, 1));
        var stale = assertThrows(ResponseStatusException.class, () -> assignments.applyAssignments(request(edit(area1, 1, groom1, groom1))));
        assertEquals(409, stale.getStatusCode().value()); assertEquals(groom2, assigned(area1, 2));
    }

    @Test void occupiedSameLotSwapUpdatesWorkoutsAndRetainsHistory() {
        owner(area1, 1, groom1); owner(area1, 2, groom2);
        long h1 = horse(area1, 1), h2 = horse(area1, 4), l = lot();
        workout(h1, l, groom1, "SCHEDULED"); workout(h2, l, groom2, "SCHEDULED");
        long historicalLot = lot(); workout(h1, historicalLot, groom1, "COMPLETED");
        var result = assignments.applyAssignments(request(edit(area1, 1, groom2, groom1), edit(area1, 2, groom1, groom2)));
        assertEquals(2, result.getAffectedWorkoutsCount());
        assertEquals(groom2, assigned(area1, 1)); assertEquals(groom1, assigned(area1, 4));
        assertEquals(groom2, jdbc.queryForObject("SELECT assigned_to_id FROM training_workouts WHERE lot_id=? AND horse_id=?", Long.class, l, h1));
        assertEquals(groom1, jdbc.queryForObject("SELECT assigned_to_id FROM training_workouts WHERE lot_id=? AND horse_id=?", Long.class, l, h2));
        assertEquals(groom1, jdbc.queryForObject("SELECT assigned_to_id FROM training_workouts WHERE lot_id=?", Long.class, historicalLot));
        assertEquals(stall(area1, 1), jdbc.queryForObject("SELECT current_stall_id FROM horses WHERE id=?", Long.class, h1));
    }

    @Test void realConflictRollsBackWholeAssignment() {
        owner(area1, 1, groom1); owner(area1, 2, groom2);
        long h1 = horse(area1, 1), h2 = horse(area1, 4), l = lot();
        workout(h1, l, groom1, "SCHEDULED"); workout(h2, l, groom2, "COMPLETED");
        var ex = assertThrows(ResponseStatusException.class, () -> assignments.applyAssignments(request(edit(area1, 1, groom2, groom1), edit(area1, 2, groom1, groom2))));
        assertEquals(409, ex.getStatusCode().value());
        assertEquals(groom1, assigned(area1, 1)); assertEquals(groom2, assigned(area1, 4));
    }

    @Test void failureAfterFlushedAssignmentWritesReallyRollsBack() {
        owner(area1, 1, groom1);
        long h = horse(area1, 1), l = lot();
        workout(h, l, groom1, "SCHEDULED");
        var failingProfiles = mock(GroomProfileRepository.class);
        when(failingProfiles.findById(groom1)).thenAnswer(i -> profiles.findById(groom1));
        AtomicBoolean observedWritten = new AtomicBoolean();
        when(failingProfiles.save(any())).thenAnswer(i -> {
            em.flush();
            observedWritten.set(jdbc.queryForObject("SELECT COUNT(*) FROM stable_stalls WHERE area_id=? AND groom_id=?", Integer.class, area1, groom2) == 3
                    && groom2 == jdbc.queryForObject("SELECT assigned_to_id FROM training_workouts WHERE horse_id=? AND lot_id=?", Long.class, h, l));
            throw new IllegalStateException("Injected failure after writes");
        });
        var tx = new TransactionTemplate(transactionManager);
        assertThrows(IllegalStateException.class, () -> tx.execute(s -> independentService(failingProfiles).applyAssignments(request(edit(area1, 1, groom2, groom1)))));
        assertTrue(observedWritten.get()); assertEquals(groom1, assigned(area1, 1));
        assertEquals(groom1, jdbc.queryForObject("SELECT assigned_to_id FROM training_workouts WHERE horse_id=? AND lot_id=?", Long.class, h, l));
    }

    @Test void convenienceAndLegacyEndpointsRequireTheSameJsonBaseline() throws Exception {
        jdbc.update("UPDATE areas SET trainer_id=NULL WHERE id=?", area1);
        var mvc = webAppContextSetup(web).apply(springSecurity()).build();
        mvc.perform(put("/api/areas/" + area1 + "/trainer").with(authentication(auth(true))).contentType("application/json")
                .content("{\"trainerId\":" + trainer1 + ",\"expectedTrainerId\":null}"))
                .andExpect(status().isOk());
        long stall = stall(area1, 1);
        mvc.perform(put("/api/stalls/" + stall + "/assign-groom").with(authentication(auth(true))).contentType("application/json")
                .content("{\"groomId\":" + groom1 + ",\"expectedGroomIds\":[null,null,null]}"))
                .andExpect(status().isOk());
        assertEquals(3, jdbc.queryForObject("SELECT COUNT(*) FROM stable_stalls WHERE area_id=? AND groom_id=?", Integer.class, area1, groom1));
        mvc.perform(put("/api/stalls/" + stall + "/assign-groom?groomId=" + groom2).with(authentication(auth(true))))
                .andExpect(status().isBadRequest());
    }

    @Test void separateServiceInstancesAndTransactionsCannotGiveGroomTwoBlocks() throws Exception {
        CyclicBarrier barrier = new CyclicBarrier(2);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var futures = new ArrayList<Future<Integer>>();
            for (long target : new long[]{area1, area2}) futures.add(executor.submit(() -> {
                barrier.await(10, TimeUnit.SECONDS);
                try {
                    return new TransactionTemplate(transactionManager).execute(s -> {
                        independentService(profiles).applyAssignments(request(edit(target, 1, groom1, null))); return 200;
                    });
                } catch (ResponseStatusException ex) { return ex.getStatusCode().value(); }
            }));
            var statuses = List.of(futures.get(0).get(25, TimeUnit.SECONDS), futures.get(1).get(25, TimeUnit.SECONDS));
            assertEquals(1, statuses.stream().filter(s -> s == 200).count());
            assertEquals(1, statuses.stream().filter(s -> s == 400).count());
            assertEquals(3, jdbc.queryForObject("SELECT COUNT(*) FROM stable_stalls WHERE groom_id=?", Integer.class, groom1));
        }
    }

    @Test void simultaneousCreatesProduceDistinctAreasWithTwelveStalls() throws Exception {
        CyclicBarrier barrier = new CyclicBarrier(2);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var a = executor.submit(() -> { barrier.await(); return areaService.createArea(com.rtms.backend.stable.enums.AreaType.QUARANTINE); });
            var b = executor.submit(() -> { barrier.await(); return areaService.createArea(com.rtms.backend.stable.enums.AreaType.QUARANTINE); });
            var first = a.get(25, TimeUnit.SECONDS); var second = b.get(25, TimeUnit.SECONDS);
            assertNotEquals(first.getCode(), second.getCode());
            for (var area : List.of(first, second)) {
                assertEquals(12, jdbc.queryForObject("SELECT COUNT(*) FROM stable_stalls WHERE area_id=?", Integer.class, area.getId()));
                assertEquals(area.getCode() + "01", jdbc.queryForObject("SELECT stall_code FROM stable_stalls WHERE area_id=? AND stall_number=1", String.class, area.getId()));
            }
        }
    }
}
