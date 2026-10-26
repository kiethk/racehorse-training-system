package com.rtms.backend.stable.service;

import com.rtms.backend.horse.entity.Horse;
import com.rtms.backend.horse.repository.HorseRepository;
import com.rtms.backend.identity.entity.*;
import com.rtms.backend.identity.repository.*;
import com.rtms.backend.stable.dto.*;
import com.rtms.backend.stable.entity.*;
import com.rtms.backend.stable.enums.AreaType;
import com.rtms.backend.stable.repository.*;
import com.rtms.backend.training.service.TrainingWorkoutService;
import jakarta.persistence.*;
import org.junit.jupiter.api.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class StableAssignmentServiceTest {
    AreaRepository areas;
    StableStallRepository stalls;
    UserRepository users;
    GroomProfileRepository profiles;
    HorseRepository horses;
    TrainingWorkoutService workouts;
    EntityManager em;
    StableAssignmentService service;
    AreaTrainerAssignmentPolicy trainerPolicy;
    com.rtms.backend.admission.service.AdmissionGroomReviewService admissionAllocation;
    List<Area> allAreas;
    List<StableStall> allStalls;
    List<User> staff;

    @BeforeEach void setup() {
        areas = mock(AreaRepository.class); stalls = mock(StableStallRepository.class);
        users = mock(UserRepository.class); profiles = mock(GroomProfileRepository.class);
        horses = mock(HorseRepository.class); workouts = mock(TrainingWorkoutService.class);
        em = mock(EntityManager.class);
        Query query = mock(Query.class);
        when(em.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        allAreas = new ArrayList<>(); allStalls = new ArrayList<>(); staff = new ArrayList<>();
        area(1, AreaType.REGULAR, 100L); area(2, AreaType.REGULAR, 101L); area(10, AreaType.QUARANTINE, null);
        staff.add(user(100, "HEAD_TRAINER")); staff.add(user(101, "HEAD_TRAINER"));
        staff.add(user(200, "GROOM")); staff.add(user(201, "GROOM"));
        when(areas.findAllForAssignmentUpdate()).thenReturn(allAreas);
        when(stalls.findAllForAssignmentUpdate()).thenReturn(allStalls);
        when(users.findByIdsForUpdate(anyCollection())).thenAnswer(i -> staff.stream()
                .filter(u -> ((Collection<?>) i.getArgument(0)).contains(u.getId())).toList());
        when(areas.save(any(Area.class))).thenAnswer(i -> i.getArgument(0));
        when(stalls.save(any(StableStall.class))).thenAnswer(i -> i.getArgument(0));
        when(profiles.findById(anyLong())).thenReturn(Optional.empty());
        trainerPolicy = mock(AreaTrainerAssignmentPolicy.class);
        admissionAllocation = mock(com.rtms.backend.admission.service.AdmissionGroomReviewService.class);
        service = new StableAssignmentService(areas, stalls, users, profiles, horses, workouts, em, trainerPolicy,
                admissionAllocation);
    }

    Area area(long id, AreaType type, Long trainer) {
        Area a = new Area(); a.setId(id); a.setCode("Area" + id); a.setName(a.getCode()); a.setType(type); a.setTrainerId(trainer);
        allAreas.add(a);
        for (int n = 1; n <= 12; n++) {
            StableStall s = new StableStall(); s.setId(id * 100 + n); s.setAreaId(id); s.setStallNumber(n); allStalls.add(s);
        }
        return a;
    }
    User user(long id, String role) {
        User u = new User(); u.setId(id); u.setFullName(role + id); u.setActive(true);
        Role r = new Role(); r.setName(role); u.setRole(r); return u;
    }
    List<StableStall> block(long area, int number) {
        return allStalls.stream().filter(s -> s.getAreaId() == area && (s.getStallNumber() - 1) / 3 + 1 == number).toList();
    }
    void owner(long area, int block, Long groom) { block(area, block).forEach(s -> s.setGroomId(groom)); }
    BlockGroomAssignmentDto groom(long area, int number, Long id) {
        var edit = new BlockGroomAssignmentDto(area, number, id);
        edit.setExpectedGroomIds(block(area, number).stream().map(StableStall::getGroomId).toList()); return edit;
    }
    AreaTrainerAssignmentDto trainer(long area, Long id) {
        Long baseline = allAreas.stream().filter(a -> a.getId() == area).findFirst().orElseThrow().getTrainerId();
        return new AreaTrainerAssignmentDto(area, id, baseline);
    }
    StableAssignmentResponse apply(BlockGroomAssignmentDto... edits) {
        return service.applyAssignments(new BatchStableAssignmentRequest(null, List.of(edits)));
    }
    void status(HttpStatus status, Runnable action) {
        assertEquals(status, assertThrows(ResponseStatusException.class, action::run).getStatusCode());
        verify(stalls, never()).save(any()); verify(areas, never()).save(any()); verify(profiles, never()).save(any());
    }
    Horse occupied(long stallId, long horseId) {
        Horse h = new Horse(); h.setId(horseId); h.setCurrentStallId(stallId); return h;
    }

    @Test void assignsWholeRegularBlockAndReportingTrainer() {
        var result = apply(groom(1, 1, 200L));
        assertEquals(3, result.getUpdatedStalls().size());
        assertTrue(block(1, 1).stream().allMatch(s -> s.getGroomId() == 200L));
        verify(profiles).save(argThat(p -> p.getUserId() == 200L && p.getTrainerId() == 100L));
    }
    @Test void quarantineGroomHasNoTrainer() {
        apply(groom(10, 1, 200L));
        verify(profiles).save(argThat(p -> p.getUserId() == 200L && p.getTrainerId() == null));
    }
    @Test void regularGroomRequiresTrainer() {
        allAreas.getFirst().setTrainerId(null);
        status(HttpStatus.BAD_REQUEST, () -> apply(groom(1, 1, 200L)));
    }
    @Test void quarantineRejectsTrainer() {
        status(HttpStatus.BAD_REQUEST, () -> service.applyAssignments(new BatchStableAssignmentRequest(List.of(trainer(10, 100L)), null)));
    }
    @Test void trainerCannotTakeThirdArea() {
        area(3, AreaType.REGULAR, 100L);
        status(HttpStatus.BAD_REQUEST, () -> service.applyAssignments(new BatchStableAssignmentRequest(List.of(trainer(2, 100L)), null)));
    }
    @Test void unrelatedLegacyTrainerLimitDoesNotBlockQuarantineAssignment() {
        area(3, AreaType.REGULAR, 100L); area(4, AreaType.REGULAR, 100L);
        assertEquals(3, apply(groom(10, 1, 200L)).getUpdatedStalls().size());
    }
    @Test void canReduceLegacyTrainerOverAllocation() {
        area(3, AreaType.REGULAR, 100L); area(4, AreaType.REGULAR, 100L); area(5, AreaType.REGULAR, 100L);
        service.applyAssignments(new BatchStableAssignmentRequest(List.of(trainer(5, null)), null));
        assertNull(allAreas.getLast().getTrainerId());
    }
    @Test void groomCannotHoldSecondBlock() {
        owner(1, 1, 200L);
        status(HttpStatus.BAD_REQUEST, () -> apply(groom(2, 1, 200L)));
    }
    @Test void mixedBlockCannotHideGroomAndAllowFourStalls() {
        block(1, 1).get(0).setGroomId(200L); block(1, 1).get(1).setGroomId(201L); block(1, 1).get(2).setGroomId(201L);
        status(HttpStatus.BAD_REQUEST, () -> apply(groom(2, 1, 200L)));
    }
    @Test void staleMiddleStallRejectsWrite() {
        owner(1, 1, 200L); var edit = groom(1, 1, 201L);
        block(1, 1).get(1).setGroomId(201L);
        status(HttpStatus.CONFLICT, () -> apply(edit));
    }
    @Test void scalarBaselineChecksAllThreeStalls() {
        owner(1, 1, 200L); block(1, 1).get(1).setGroomId(201L);
        status(HttpStatus.CONFLICT, () -> apply(new BlockGroomAssignmentDto(1L, 1, 201L, 200L)));
    }
    @Test void explicitMixedBaselineCanRepairBlock() {
        block(1, 1).get(0).setGroomId(200L); block(1, 1).get(1).setGroomId(201L);
        apply(groom(1, 1, 200L));
        assertTrue(block(1, 1).stream().allMatch(s -> s.getGroomId() == 200L));
        verify(profiles).save(argThat(p -> p.getUserId() == 201L && p.getTrainerId() == null));
    }
    @Test void occupiedBlockCannotBeCleared() {
        owner(1, 1, 200L);
        when(horses.findByCurrentStallIdIn(anyList())).thenReturn(List.of(occupied(102, 50)));
        status(HttpStatus.CONFLICT, () -> apply(groom(1, 1, null)));
        verifyNoInteractions(workouts);
    }
    @Test void emptyBlockCanBeCleared() {
        owner(1, 1, 200L); apply(groom(1, 1, null));
        assertTrue(block(1, 1).stream().allMatch(s -> s.getGroomId() == null));
        verify(profiles).save(argThat(p -> p.getUserId() == 200L && p.getTrainerId() == null));
    }
    @Test void occupiedBlocksSwapInOneWorkoutBatch() {
        owner(1, 1, 200L); owner(1, 2, 201L);
        var first = occupied(101, 50); var second = occupied(104, 51);
        when(horses.findByCurrentStallIdIn(anyList())).thenReturn(List.of(first, second));
        when(workouts.reassignFutureWorkoutsToGrooms(anyMap())).thenReturn(2);
        var result = apply(groom(1, 1, 201L), groom(1, 2, 200L));
        assertEquals(2, result.getAffectedWorkoutsCount());
        verify(workouts).reassignFutureWorkoutsToGrooms(Map.of(50L, 201L, 51L, 200L));
        assertEquals(101L, first.getCurrentStallId()); assertEquals(104L, second.getCurrentStallId());
    }
    @Test void genuineWorkoutConflictDoesNotWriteAssignments() {
        when(horses.findByCurrentStallIdIn(anyList())).thenReturn(List.of(occupied(101, 50)));
        when(workouts.reassignFutureWorkoutsToGrooms(anyMap())).thenThrow(new IllegalStateException("BR-09"));
        status(HttpStatus.CONFLICT, () -> apply(groom(1, 1, 200L)));
    }
    @Test void missingBaselineIsRejected() {
        status(HttpStatus.BAD_REQUEST, () -> apply(new BlockGroomAssignmentDto(1L, 1, 200L)));
        verifyNoInteractions(em);
    }
    @Test void missingTrainerBaselineIsRejected() {
        status(HttpStatus.BAD_REQUEST, () -> service.applyAssignments(new BatchStableAssignmentRequest(List.of(new AreaTrainerAssignmentDto(1L, 101L)), null)));
    }
    @Test void staleTrainerIsRejected() {
        status(HttpStatus.CONFLICT, () -> service.applyAssignments(new BatchStableAssignmentRequest(List.of(new AreaTrainerAssignmentDto(1L, 101L, null)), null)));
    }
    @Test void missingStaffIsRejected() {
        status(HttpStatus.BAD_REQUEST, () -> apply(groom(1, 1, 999L)));
    }
    @Test void inactiveStaffIsRejected() {
        staff.get(2).setActive(false);
        status(HttpStatus.BAD_REQUEST, () -> apply(groom(1, 1, 200L)));
    }
    @Test void wrongRoleIsRejected() {
        status(HttpStatus.BAD_REQUEST, () -> apply(groom(1, 1, 100L)));
    }
    @Test void removeTrainerWithGroomsIsRejected() {
        owner(1, 1, 200L);
        status(HttpStatus.BAD_REQUEST, () -> service.applyAssignments(new BatchStableAssignmentRequest(List.of(trainer(1, null)), null)));
    }
    @Test void replaceTrainerUpdatesGroomReporting() {
        owner(1, 1, 200L);
        service.applyAssignments(new BatchStableAssignmentRequest(List.of(trainer(1, 101L)), null));
        verify(profiles).save(argThat(p -> p.getUserId() == 200L && p.getTrainerId() == 101L));
        verify(trainerPolicy).requireChangeAllowed(1L);
    }
    @Test void blockedTrainerChangeRejectsTheWholeBatchBeforeChangingGroomWorkouts() {
        doThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Area has horses"))
                .when(trainerPolicy).requireChangeAllowed(1L);
        owner(1, 1, 200L);
        status(HttpStatus.CONFLICT, () -> service.applyAssignments(new BatchStableAssignmentRequest(
                List.of(trainer(1, 101L)), List.of(groom(1, 1, 201L)))));
        verify(areas, never()).save(any()); verify(stalls, never()).save(any());
        verifyNoInteractions(workouts);
    }
    @Test void unchangedTrainerDoesNotBlockGroomOnlyChanges() {
        service.applyAssignments(new BatchStableAssignmentRequest(
                List.of(trainer(1, 100L)), List.of(groom(1, 1, 200L))));
        verifyNoInteractions(trainerPolicy);
        verify(stalls, times(3)).save(any());
    }
    @Test void removeEmptyGroomBlocksAndTrainerTogether() {
        owner(1, 1, 200L);
        service.applyAssignments(new BatchStableAssignmentRequest(List.of(trainer(1, null)), List.of(groom(1, 1, null))));
        assertNull(allAreas.getFirst().getTrainerId());
    }
    @Test void lockFailureAbortsBeforeReadingOrWriting() {
        when(em.createNativeQuery(anyString())).thenThrow(new IllegalStateException("lock failed"));
        assertThrows(IllegalStateException.class, () -> apply(groom(1, 1, 200L)));
        verify(areas, never()).findAllForAssignmentUpdate(); verify(stalls, never()).save(any());
    }
    @Test void incompleteBlockIsRejected() {
        allStalls.remove(block(1, 1).get(2));
        status(HttpStatus.BAD_REQUEST, () -> apply(new BlockGroomAssignmentDto(1L, 1, 200L, null)));
    }
    @Test void duplicateTargetsAreRejected() {
        var edit = groom(1, 1, 200L);
        status(HttpStatus.BAD_REQUEST, () -> apply(edit, edit));
    }
    @Test void nullListElementReturnsBadRequest() {
        status(HttpStatus.BAD_REQUEST, () -> service.applyAssignments(new BatchStableAssignmentRequest(null, Arrays.asList((BlockGroomAssignmentDto) null))));
    }
    @Test void nullBaselineEntriesDeserializeAndInternalFlagCannotDisableChecking() throws Exception {
        var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        var t = mapper.readValue("{\"areaId\":1,\"trainerId\":101,\"expectedTrainerId\":null,\"expectedTrainerIdSet\":false}", AreaTrainerAssignmentDto.class);
        assertTrue(t.isExpectedTrainerIdSet());
        status(HttpStatus.CONFLICT, () -> service.applyAssignments(new BatchStableAssignmentRequest(List.of(t), null)));
        var b = mapper.readValue("{\"areaId\":1,\"blockNumber\":1,\"groomId\":200,\"expectedGroomIds\":[null,null,null]}", BlockGroomAssignmentDto.class);
        assertEquals(3, apply(b).getUpdatedStalls().size());
    }
}
