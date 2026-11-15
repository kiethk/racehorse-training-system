package com.rtms.backend.admission.service;

import com.rtms.backend.admission.enums.AdmissionStatus;
import com.rtms.backend.admission.repository.AdmissionApplicationRepository;
import com.rtms.backend.identity.entity.GroomProfile;
import com.rtms.backend.identity.entity.User;
import com.rtms.backend.identity.repository.GroomProfileRepository;
import com.rtms.backend.identity.repository.UserRepository;
import com.rtms.backend.stable.repository.StableStallRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GroomWorkloadServiceTest {
    @Mock StableStallRepository stalls;
    @Mock GroomProfileRepository profiles;
    @Mock AdmissionApplicationRepository admissions;
    @Mock UserRepository users;
    GroomWorkloadService service;
    final List<Long> ids = List.of(1L, 2L, 3L);

    @BeforeEach
    void setup() {
        service = new GroomWorkloadService(stalls, profiles, admissions, users);
    }

    private void candidates() {
        when(stalls.findQuarantineGroomIds()).thenReturn(List.of(3L, 1L, 2L));
        when(profiles.findByUserIdsForUpdate(ids)).thenReturn(List.of(
                profile(1L, LocalDateTime.now().minusDays(2)), profile(2L, null), profile(3L, null)));
    }

    @Test
    void sumsHorsesAndPendingAdmissionsBeforeRanking() {
        candidates();
        when(stalls.countHorsesByGroomIds(ids)).thenReturn(List.<Object[]>of(new Object[]{1L, 1L}, new Object[]{2L, 3L}));
        when(admissions.countPendingByAssignedGroomIds(ids, AdmissionStatus.GROOM_REVIEW))
                .thenReturn(List.<Object[]>of(new Object[]{1L, 4L}, new Object[]{3L, 4L}));
        assertEquals(Optional.of(2L), service.selectGroom(Set.of(), false));
        var order = inOrder(profiles, stalls, admissions);
        order.verify(profiles).ensureProfile(1L);
        order.verify(profiles).ensureProfile(2L);
        order.verify(profiles).ensureProfile(3L);
        order.verify(profiles).findByUserIdsForUpdate(ids);
        order.verify(stalls).countHorsesByGroomIds(ids);
        order.verify(admissions).countPendingByAssignedGroomIds(ids, AdmissionStatus.GROOM_REVIEW);
    }

    @Test
    void tiesUseNullArrivalFirstThenLowestId() {
        candidates();
        assertEquals(Optional.of(2L), service.selectGroom(Set.of(), false));
    }

    @Test
    void tiesUseOldestArrival() {
        candidates();
        when(profiles.findByUserIdsForUpdate(ids)).thenReturn(List.of(
                profile(1L, LocalDateTime.now()), profile(2L, LocalDateTime.now().minusDays(3)),
                profile(3L, LocalDateTime.now().minusDays(1))));
        assertEquals(Optional.of(2L), service.selectGroom(Set.of(), false));
    }

    @Test
    void excludesPreviousGrooms() {
        candidates();
        assertEquals(Optional.of(1L), service.selectGroom(Set.of(2L, 3L), false));
        assertTrue(service.selectGroom(Set.copyOf(ids), false).isEmpty());
    }

    @Test
    void requiresFreeStallOnlyWhenRequested() {
        candidates();
        when(stalls.findGroomIdsWithFreeQuarantineStall(ids)).thenReturn(List.of(1L));
        assertEquals(Optional.of(1L), service.selectGroom(Set.of(), true));
        assertEquals(Optional.of(2L), service.selectGroom(Set.of(), false));
        assertTrue(service.selectGroom(Set.of(1L), true).isEmpty());
    }

    @Test
    void noCandidatesDoesNotQueryWorkloads() {
        assertTrue(service.selectGroom(Set.of(), false).isEmpty());
        assertTrue(service.describeQuarantineGrooms().isEmpty());
        verifyNoInteractions(profiles, admissions, users);
    }

    @Test
    void pickerUsesSameWorkloadAndFreeStallDefinition() {
        when(stalls.findQuarantineGroomIds()).thenReturn(ids);
        when(stalls.countHorsesByGroomIds(ids)).thenReturn(List.<Object[]>of(new Object[]{1L, 3L}));
        when(admissions.countPendingByAssignedGroomIds(ids, AdmissionStatus.GROOM_REVIEW))
                .thenReturn(List.<Object[]>of(new Object[]{1L, 2L}));
        when(stalls.findGroomIdsWithFreeQuarantineStall(ids)).thenReturn(List.of(2L));
        when(users.findAllById(ids)).thenReturn(ids.stream().map(id -> {
            User user = new User(); user.setId(id); user.setFullName("Groom " + id); return user;
        }).toList());
        assertEquals(new GroomWorkloadService.GroomCandidate(1L, "Groom 1", 5L, true),
                service.describeQuarantineGrooms().getFirst());
        when(stalls.findGroomIdsWithFreeQuarantineStall(List.of(2L))).thenReturn(List.of(2L));
        assertFalse(service.isFull(2L));
        assertTrue(service.isFull(3L));
    }

    private GroomProfile profile(Long id, LocalDateTime arrival) {
        GroomProfile p = new GroomProfile(); p.setUserId(id); p.setLastArrivalConfirmedAt(arrival); return p;
    }
}
