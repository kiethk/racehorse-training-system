package com.rtms.backend.stable.service;

import com.rtms.backend.horse.service.HorseService;
import com.rtms.backend.security.AuthenticatedUser;
import com.rtms.backend.stable.dto.*;
import com.rtms.backend.stable.enums.AreaType;
import com.rtms.backend.stable.repository.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
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
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

/** Deliberately commits fixtures: enable only against a disposable local PostgreSQL database. */
@SpringBootTest(properties = {"spring.datasource.url=${RTMS_STABLE_TEST_DB_URL}", "spring.config.import="})
@ActiveProfiles("local")
@EnabledIfEnvironmentVariable(named = "RTMS_STABLE_TEST_DB_URL",
        matches = "jdbc:postgresql://localhost:5433/rtms_stable_test_[a-zA-Z0-9_]+")
class AreaLifecyclePostgresTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired AreaLifecycleService lifecycle;
    @Autowired AreaService areaService;
    @Autowired AreaRepository areas;
    @Autowired StableStallRepository stalls;
    @Autowired StableAssignmentService assignments;
    @Autowired HorseService horseService;
    @Autowired EntityManager em;
    @Autowired PlatformTransactionManager transactionManager;
    @Autowired WebApplicationContext web;
    List<Long> fixtureAreas;
    String tag;
    long area, quarantine, employee;

    @BeforeEach void fixtures() {
        tag = UUID.randomUUID().toString().substring(0, 8);
        fixtureAreas = new ArrayList<>();
        area = area("REGULAR", "R"); quarantine = area("QUARANTINE", "Q");
        employee = jdbc.queryForObject("""
                INSERT INTO users(full_name,email,password_hash,role_id,is_active)
                SELECT 'Lifecycle QA',?,'unused',id,true FROM roles WHERE name='GROOM' RETURNING id
                """, Long.class, tag + "@lifecycle.test.invalid");
    }

    @AfterEach void releaseArchivedFixtures() {
        // Do not let a previous test's archived Area become another test's restore candidate.
        for (long id : fixtureAreas) {
            jdbc.update("UPDATE stable_stalls SET deleted_at=NULL WHERE area_id=?", id);
            jdbc.update("UPDATE areas SET deleted_at=NULL WHERE id=?", id);
        }
    }

    long area(String type, String suffix) {
        String code = "TEST-" + tag + suffix;
        long id = jdbc.queryForObject("INSERT INTO areas(code,name,type) VALUES (?,?,?) RETURNING id", Long.class, code, code, type);
        fixtureAreas.add(id);
        jdbc.update("""
                INSERT INTO stable_stalls(area_id,stall_number,stall_code,status)
                SELECT ?,n,?||LPAD(n::text,2,'0'),'AVAILABLE' FROM generate_series(1,12)n
                """, id, code);
        return id;
    }
    long stall(long id) { return jdbc.queryForObject("SELECT id FROM stable_stalls WHERE area_id=? AND stall_number=1", Long.class, id); }
    long horse(Long target) {
        return jdbc.queryForObject("INSERT INTO horses(name,current_status,training_decision,current_stall_id) VALUES ('Lifecycle QA','ELIGIBLE','ALLOWED',?) RETURNING id", Long.class, target);
    }
    void conflict(long id) {
        int archivedBefore = jdbc.queryForObject("SELECT COUNT(*) FROM stable_stalls WHERE area_id=? AND deleted_at IS NOT NULL", Integer.class, id);
        assertFalse(lifecycle.check(id).canDelete());
        assertEquals(409, assertThrows(ResponseStatusException.class, () -> lifecycle.delete(id)).getStatusCode().value());
        assertTrue(areas.findById(id).isPresent());
        assertEquals(archivedBefore, jdbc.queryForObject("SELECT COUNT(*) FROM stable_stalls WHERE area_id=? AND deleted_at IS NOT NULL", Integer.class, id));
    }

    @Test void softDeleteHidesOrmAndEveryNativeAllocationQuery() {
        long target = stall(quarantine);
        // Only staffed stalls count as admission capacity, so give the fixture stalls a groom first.
        Long groom = jdbc.queryForObject("""
                SELECT u.id FROM users u JOIN roles r ON r.id = u.role_id
                WHERE r.name = 'GROOM' ORDER BY u.id LIMIT 1
                """, Long.class);
        jdbc.update("UPDATE stable_stalls SET groom_id = ? WHERE area_id IN (?, ?)", groom, quarantine, area);
        long available = stalls.countAvailableQuarantineStalls();
        long regularAvailable = stalls.countAvailableRegularStalls();
        assertTrue(lifecycle.check(quarantine).canDelete());
        assertEquals(12, lifecycle.delete(quarantine).stallCount());
        lifecycle.delete(area);
        assertTrue(areas.findById(quarantine).isEmpty());
        assertTrue(stalls.findById(target).isEmpty());
        assertTrue(stalls.findByAreaId(quarantine).isEmpty());
        assertEquals(available - 12, stalls.countAvailableQuarantineStalls());
        assertEquals(regularAvailable - 12, stalls.countAvailableRegularStalls());
        assertFalse(areas.findAllForAssignmentUpdate().stream().anyMatch(a -> a.getId().equals(quarantine)));
        assertFalse(stalls.findAllForAssignmentUpdate().stream().anyMatch(s -> s.getAreaId().equals(quarantine)));
        assertFalse(stalls.findByAreaIdInOrdered(List.of(quarantine,area)).iterator().hasNext());
        assertFalse(stalls.findAllAvailableRegularStallsOrdered().stream().anyMatch(s -> s.getAreaId().equals(area)));
        new TransactionTemplate(transactionManager).executeWithoutResult(tx -> {
            assertTrue(stalls.findActiveByIdForUpdate(target).isEmpty());
            assertTrue(stalls.findQuarantineStallByIdForUpdate(target).isEmpty());
            assertTrue(stalls.findAvailableRegularStallByIdForUpdate(stall(area)).isEmpty());
            assertTrue(stalls.findBlockForUpdate(quarantine, 1, 3).isEmpty());
            assertFalse(stalls.lockAdmissionCapacityStallsForUpdate().stream().anyMatch(s -> fixtureAreas.contains(s.getAreaId())));
            assertFalse(stalls.findFairAvailableStallForUpdate("QUARANTINE").map(s -> s.getAreaId().equals(quarantine)).orElse(false));
            assertFalse(stalls.findFairAvailableStallForUpdate("REGULAR").map(s -> s.getAreaId().equals(area)).orElse(false));
        });
        assertEquals(12, jdbc.queryForObject("SELECT COUNT(*) FROM stable_stalls WHERE area_id=? AND deleted_at IS NOT NULL", Integer.class, quarantine));
        assertEquals(404, assertThrows(ResponseStatusException.class, () -> lifecycle.delete(quarantine)).getStatusCode().value());
    }

    @Test void addRestoresSameIdsCodesAndAllTwelveStallsBeforeAllocatingNewCode() {
        var before = stalls.findByAreaId(area);
        lifecycle.delete(area); lifecycle.delete(quarantine);
        var restored = areaService.createArea(AreaType.REGULAR);
        assertEquals(area, restored.getId());
        assertEquals(before.stream().map(s -> s.getId()).sorted().toList(), stalls.findByAreaId(area).stream().map(s -> s.getId()).sorted().toList());
        assertNull(restored.getDeletedAt());
        assertTrue(areas.findById(quarantine).isEmpty());
        assertEquals(quarantine, areaService.createArea(AreaType.QUARANTINE).getId());
    }

    @Test void restoreChoosesShortestCodeThenAlphabeticalCode() {
        long longer = area("REGULAR", "AA");
        lifecycle.delete(longer); lifecycle.delete(area);
        assertEquals(area, areaService.createArea(AreaType.REGULAR).getId());
        assertEquals(longer, areaService.createArea(AreaType.REGULAR).getId());
    }

    @Test void addNeverRestoresAnArchivedAreaThatAcquiredInvalidReferences() {
        lifecycle.delete(area);
        jdbc.update("UPDATE stable_stalls SET groom_id=? WHERE id=?", employee, stall(area));
        var created = areaService.createArea(AreaType.REGULAR);
        assertNotEquals(area, created.getId());
        assertTrue(areas.findById(area).isEmpty());
        assertEquals(employee, jdbc.queryForObject("SELECT groom_id FROM stable_stalls WHERE id=?", Long.class, stall(area)));
    }

    @Test void reservationsBlockDeletionAndRemainUnchanged() {
        jdbc.update("UPDATE stable_stalls SET status='RESERVED' WHERE id=?", stall(area));
        conflict(area);
    }
    @Test void trainerOrGroomAssignmentBlocksDeletion() {
        jdbc.update("UPDATE areas SET trainer_id=? WHERE id=?", employee, area);
        conflict(area);
        jdbc.update("UPDATE areas SET trainer_id=NULL WHERE id=?", area);
        jdbc.update("UPDATE stable_stalls SET groom_id=? WHERE id=?", employee, stall(area));
        conflict(area);
    }
    @Test void actualHorseBlocksDeletionEvenIfStallStatusIsIncorrect() {
        horse(stall(area)); conflict(area);
    }
    @Test void historicalRejectedAdmissionStillBlocksDeletion() {
        jdbc.update("INSERT INTO admission_applications(owner_id,status,quarantine_stall_id) VALUES (?,'REJECTED',?)", employee, stall(quarantine));
        conflict(quarantine);
    }
    @Test void incompleteOrPartiallyArchivedAreaCannotBeDeletedOrRestored() {
        jdbc.update("UPDATE stable_stalls SET deleted_at=CURRENT_TIMESTAMP WHERE id=?", stall(area));
        conflict(area);
    }

    @Test void transactionRollbackRestoresVisibilityAndCounts() {
        assertThrows(IllegalStateException.class, () -> new TransactionTemplate(transactionManager).executeWithoutResult(tx -> {
            lifecycle.delete(area); throw new IllegalStateException("Injected failure after archive writes");
        }));
        assertTrue(areas.findById(area).isPresent()); assertEquals(12, stalls.findByAreaId(area).size());
    }

    @Test void lifecycleInvalidatesCachedEntitiesAndFlushesPendingAssignments() {
        new TransactionTemplate(transactionManager).executeWithoutResult(tx -> {
            var cachedArea = areas.findById(area).orElseThrow();
            var cachedStall = stalls.findById(stall(area)).orElseThrow();
            assertNotNull(cachedArea); assertNotNull(cachedStall);
            lifecycle.delete(area);
            assertTrue(areas.findById(area).isEmpty());
            assertTrue(stalls.findById(cachedStall.getId()).isEmpty());
            assertEquals(area, areaService.createArea(AreaType.REGULAR).getId());
            var managed = areas.findById(area).orElseThrow();
            managed.setTrainerId(employee); // not explicitly saved/flushed yet
            assertEquals(409, assertThrows(ResponseStatusException.class, () -> lifecycle.delete(area)).getStatusCode().value());
            // An expected failed mutation marks this outer transaction rollback-only.
            tx.setRollbackOnly();
        });
    }

    @Test void deletedAreaCannotReceiveAssignmentsOrHorses() {
        long target = stall(area); lifecycle.delete(area);
        var edit = new AreaTrainerAssignmentDto(area, employee, null);
        assertEquals(404, assertThrows(ResponseStatusException.class,
                () -> assignments.applyAssignments(new BatchStableAssignmentRequest(List.of(edit), null))).getStatusCode().value());
        assertThrows(RuntimeException.class, () -> horseService.assignStall(horse(null), target));
        assertNull(jdbc.queryForObject("SELECT current_stall_id FROM horses WHERE name='Lifecycle QA' ORDER BY id DESC LIMIT 1", Long.class));
    }

    @Test void permissionAndHttpVisibilityAreEnforced() throws Exception {
        var mvc = webAppContextSetup(web).apply(springSecurity()).build();
        var principal = new AuthenticatedUser(employee, "qa@test.invalid", "CLUB_MANAGER");
        var viewer = new UsernamePasswordAuthenticationToken(principal, "unused", List.of(new SimpleGrantedAuthority("AREA_VIEW"), new SimpleGrantedAuthority("STABLE_STALL_VIEW")));
        var manager = new UsernamePasswordAuthenticationToken(principal, "unused", List.of(new SimpleGrantedAuthority("AREA_DELETE")));
        mvc.perform(delete("/api/areas/" + area).with(authentication(viewer))).andExpect(status().isForbidden());
        mvc.perform(get("/api/areas/" + area + "/deletion-check").with(authentication(manager)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.canDelete").value(true));
        mvc.perform(delete("/api/areas/" + area).with(authentication(manager)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.stallCount").value(12));
        mvc.perform(get("/api/areas/" + area).with(authentication(viewer))).andExpect(status().isNotFound());
        mvc.perform(get("/api/stalls/" + stall(area)).with(authentication(viewer))).andExpect(status().isNotFound());
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM role_permissions rp JOIN permissions p ON rp.permission_id=p.id JOIN roles r ON r.id=rp.role_id WHERE r.name='CLUB_MANAGER' AND p.code='AREA_DELETE'", Integer.class));
    }

    @Test void simultaneousAddsRestoreAnArchivedAreaOnlyOnceAcrossServiceInstances() throws Exception {
        lifecycle.delete(quarantine);
        var barrier = new CyclicBarrier(2);
        try (var executor = Executors.newFixedThreadPool(2)) {
            List<Future<Long>> futures = new ArrayList<>();
            for (int i = 0; i < 2; i++) futures.add(executor.submit(() -> {
                barrier.await(10, TimeUnit.SECONDS);
                return new TransactionTemplate(transactionManager).execute(tx ->
                        new AreaService(areas, stalls, em, lifecycle).createArea(AreaType.QUARANTINE).getId());
            }));
            var ids = List.of(futures.get(0).get(25, TimeUnit.SECONDS), futures.get(1).get(25, TimeUnit.SECONDS));
            assertNotEquals(ids.get(0),ids.get(1)); assertTrue(ids.contains(quarantine));
            for (long id : ids) assertEquals(12, stalls.findByAreaId(id).size());
        }
    }

    @Test void deleteWaitsForReservationAndRevalidatesAfterRowLock() throws Exception {
        long target = stall(quarantine);
        var locked = new CountDownLatch(1); var release = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var reserve = executor.submit(() -> new TransactionTemplate(transactionManager).executeWithoutResult(tx -> {
                stalls.findQuarantineStallByIdForUpdate(target).orElseThrow(); locked.countDown();
                try { assertTrue(release.await(10, TimeUnit.SECONDS)); } catch (InterruptedException e) { throw new RuntimeException(e); }
                jdbc.update("UPDATE stable_stalls SET status='RESERVED' WHERE id=?", target);
            }));
            assertTrue(locked.await(10, TimeUnit.SECONDS));
            var deletion = executor.submit(() -> {
                try { lifecycle.delete(quarantine); return 200; }
                catch (ResponseStatusException ex) { return ex.getStatusCode().value(); }
            });
            try { assertWaitingOnStallLock(); } finally { release.countDown(); }
            reserve.get(15, TimeUnit.SECONDS);
            assertEquals(409, deletion.get(15, TimeUnit.SECONDS)); assertTrue(areas.findById(quarantine).isPresent());
        }
    }

    private void assertWaitingOnStallLock() throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(4);
        while (System.nanoTime() < deadline) {
            int waiting = jdbc.queryForObject("""
                    SELECT COUNT(*) FROM pg_stat_activity WHERE datname=current_database()
                    AND pid<>pg_backend_pid() AND wait_event_type='Lock'
                    AND (query LIKE 'SELECT stall_number,groom_id,status,deleted_at%'
                         OR query LIKE 'SELECT ss.* FROM stable_stalls%')
                    """, Integer.class);
            if (waiting > 0) return;
            Thread.sleep(25);
        }
        fail("Expected the competing transaction to wait on the locked stall row");
    }

    @Test void allocationWaitsForDeletionAndCannotSelectTheArchivedStall() throws Exception {
        long target = stall(quarantine);
        var deleted = new CountDownLatch(1); var release = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var deletion = executor.submit(() -> new TransactionTemplate(transactionManager).executeWithoutResult(tx -> {
                lifecycle.delete(quarantine); deleted.countDown();
                try { assertTrue(release.await(10, TimeUnit.SECONDS)); } catch (InterruptedException e) { throw new RuntimeException(e); }
            }));
            assertTrue(deleted.await(10, TimeUnit.SECONDS));
            var allocation = executor.submit(() -> new TransactionTemplate(transactionManager).execute(tx ->
                    stalls.findActiveByIdForUpdate(target).isPresent()));
            try { assertWaitingOnStallLock(); } finally { release.countDown(); }
            deletion.get(15, TimeUnit.SECONDS);
            assertFalse(allocation.get(15, TimeUnit.SECONDS));
        }
    }
}
