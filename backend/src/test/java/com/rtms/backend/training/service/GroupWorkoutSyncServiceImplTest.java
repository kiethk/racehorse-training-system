package com.rtms.backend.training.service;

import com.rtms.backend.training.entity.TrainingGroupMember;
import com.rtms.backend.training.entity.TrainingWorkout;
import com.rtms.backend.training.repository.GroupPlanRepository;
import com.rtms.backend.training.repository.TrainingGroupMemberRepository;
import com.rtms.backend.training.repository.TrainingSessionRepository;
import com.rtms.backend.training.repository.TrainingWorkoutRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GroupWorkoutSyncServiceImplTest {

    private GroupPlanRepository planRepository;
    private TrainingSessionRepository sessionRepository;
    private TrainingWorkoutRepository workoutRepository;
    private TrainingGroupMemberRepository memberRepository;
    private SessionSettlementService settlement;
    private Clock clock;
    private GroupWorkoutSyncServiceImpl service;

    @BeforeEach
    void setUp() {
        planRepository = mock(GroupPlanRepository.class);
        sessionRepository = mock(TrainingSessionRepository.class);
        workoutRepository = mock(TrainingWorkoutRepository.class);
        memberRepository = mock(TrainingGroupMemberRepository.class);
        settlement = mock(SessionSettlementService.class);
        clock = Clock.fixed(Instant.parse("2026-11-09T10:00:00Z"), ZoneId.of("UTC"));

        service = new GroupWorkoutSyncServiceImpl(
                planRepository, sessionRepository, workoutRepository, memberRepository, settlement, clock);
    }

    @Test
    void reassignFutureWorkoutsToGrooms_swapsGroomsInSameGroupWithTwoPhaseFlush() {
        // Horse 6 (groom 10) and Horse 8 (groom 20) are both in Group 3
        TrainingGroupMember m1 = new TrainingGroupMember();
        m1.setId(1L);
        m1.setGroupId(3L);
        m1.setHorseId(6L);
        m1.setGroomId(10L);

        TrainingGroupMember m2 = new TrainingGroupMember();
        m2.setId(2L);
        m2.setGroupId(3L);
        m2.setHorseId(8L);
        m2.setGroomId(20L);

        when(memberRepository.findByHorseIdInAndLeftAtIsNull(Set.of(6L, 8L)))
                .thenReturn(List.of(m1, m2));
        when(memberRepository.findByGroupIdInAndLeftAtIsNull(List.of(3L)))
                .thenReturn(List.of(m1, m2));

        TrainingWorkout w1 = new TrainingWorkout();
        w1.setId(101L);
        w1.setHorseId(6L);
        w1.setAssignedToId(10L);

        TrainingWorkout w2 = new TrainingWorkout();
        w2.setId(102L);
        w2.setHorseId(8L);
        w2.setAssignedToId(20L);

        when(workoutRepository.findNotStartedOfHorses(any(), any(), any()))
                .thenReturn(List.of(w1, w2));

        // Swap: Horse 6 -> Groom 20, Horse 8 -> Groom 10
        Map<Long, Long> groomByHorse = Map.of(6L, 20L, 8L, 10L);
        int updated = service.reassignFutureWorkoutsToGrooms(groomByHorse);

        assertEquals(2, updated);
        assertEquals(20L, w1.getAssignedToId());
        assertEquals(10L, w2.getAssignedToId());

        // Verify two-phase flush on memberRepository:
        // Invocation 1: members have null groomId (to satisfy Postgres unique partial index)
        // Invocation 2: members have swapped groomId
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<TrainingGroupMember>> captor = ArgumentCaptor.forClass(List.class);
        verify(memberRepository, times(2)).saveAllAndFlush(captor.capture());

        List<List<TrainingGroupMember>> allInvocations = captor.getAllValues();
        // Final state in members:
        assertEquals(20L, m1.getGroomId());
        assertEquals(10L, m2.getGroomId());
    }

    @Test
    void reassignFutureWorkoutsToGrooms_throwsClashWhenTwoHorsesAssignedSameGroomInGroup() {
        TrainingGroupMember m1 = new TrainingGroupMember();
        m1.setId(1L);
        m1.setGroupId(3L);
        m1.setHorseId(6L);
        m1.setGroomId(10L);

        TrainingGroupMember m2 = new TrainingGroupMember();
        m2.setId(2L);
        m2.setGroupId(3L);
        m2.setHorseId(8L);
        m2.setGroomId(20L);

        when(memberRepository.findByHorseIdInAndLeftAtIsNull(Set.of(6L, 8L)))
                .thenReturn(List.of(m1, m2));
        when(memberRepository.findByGroupIdInAndLeftAtIsNull(List.of(3L)))
                .thenReturn(List.of(m1, m2));

        // Assign both to groom 10
        Map<Long, Long> groomByHorse = Map.of(6L, 10L, 8L, 10L);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> service.reassignFutureWorkoutsToGrooms(groomByHorse));
        assertEquals("BR-09: a Groom cannot lead two horses in group #3. Choose another Groom or move a horse to another group.", ex.getMessage());
    }
}
