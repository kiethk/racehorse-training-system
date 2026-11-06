package com.rtms.backend.admission.service;

import com.rtms.backend.admission.entity.AdmissionApplication;
import com.rtms.backend.identity.repository.UserRepository;
import com.rtms.backend.security.AuthenticatedUser;
import com.rtms.backend.admission.entity.AdmissionGroomAssignment;
import com.rtms.backend.admission.enums.AdmissionStatus;
import com.rtms.backend.admission.enums.AssignmentEndReason;
import com.rtms.backend.admission.enums.ReviewDecision;
import com.rtms.backend.admission.repository.AdmissionApplicationRepository;
import com.rtms.backend.admission.repository.AdmissionGroomAssignmentRepository;
import com.rtms.backend.stable.repository.StableStallRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GroomAssignmentServiceTest {
    @Mock AdmissionApplicationRepository admissions;
    @Mock AdmissionGroomAssignmentRepository assignments;
    @Mock GroomWorkloadService workload;
    @Mock StableStallRepository stalls;
    @Mock PlatformTransactionManager transactions;
    @Mock UserRepository users;
    GroomAssignmentService service;

    @BeforeEach
    void setup() {
        service = new GroomAssignmentService(admissions, assignments, workload, stalls, transactions,
                mock(GroomAssignmentNotificationService.class), users);
    }

    private AdmissionApplication pending(Long groomId) {
        AdmissionApplication a = new AdmissionApplication(); a.setId(10L);
        a.setStatus(AdmissionStatus.GROOM_REVIEW); a.setAssignedGroomId(groomId);
        a.setGroomReviewDueAt(LocalDateTime.now().minusHours(1));
        when(admissions.findByIdForUpdate(10L)).thenReturn(Optional.of(a));
        return a;
    }

    @Test
    void assignsOnceWithSeparateReviewerAndSeventyTwoHourWindow() {
        AdmissionApplication a = pending(null);
        when(workload.selectGroom(Set.of(), false)).thenReturn(Optional.of(1L));
        assertEquals(Optional.of(1L), service.assignIfUnassigned(10L));
        assertEquals(a.getGroomAssignedAt().plusHours(72), a.getGroomReviewDueAt());
        assertNull(a.getGroomId());
        assertEquals(Optional.of(1L), service.assignIfUnassigned(10L));
        ArgumentCaptor<AdmissionGroomAssignment> row = ArgumentCaptor.forClass(AdmissionGroomAssignment.class);
        verify(assignments).save(row.capture());
        assertEquals(10L, row.getValue().getAdmissionId());
        assertEquals(1L, row.getValue().getGroomId());
        assertNull(row.getValue().getAssignedById());
        verify(workload).selectGroom(Set.of(), false);
    }

    @Test
    void missingOrWrongStateDoesNothing() {
        assertTrue(service.assignIfUnassigned(10L).isEmpty());
        AdmissionApplication a = pending(null); a.setStatus(AdmissionStatus.REJECTED);
        assertTrue(service.assignIfUnassigned(10L).isEmpty());
        assertEquals(409, assertThrows(ResponseStatusException.class,
                () -> service.reassign(10L, AssignmentEndReason.MANUAL, 9L, 2L)).getStatusCode().value());
        verifyNoInteractions(assignments, workload);
    }

    @Test
    void noCandidateLeavesUnassignedForRetry() {
        AdmissionApplication a = pending(null);
        assertTrue(service.assignIfUnassigned(10L).isEmpty());
        assertNull(a.getAssignedGroomId());
        verify(assignments, never()).save(any());
    }

    @Test
    void overdueSkipsEveryoneWhoAlreadyHadAdmissionAndClosesBeforeOpening() {
        AdmissionApplication a = pending(1L);
        when(assignments.findPreviousGroomIds(10L)).thenReturn(List.of(1L, 2L));
        when(workload.selectGroom(Set.of(1L, 2L), false)).thenReturn(Optional.of(3L));
        AdmissionGroomAssignment old = new AdmissionGroomAssignment();
        when(assignments.findOpenByAdmissionId(10L)).thenReturn(Optional.of(old));
        assertEquals(Optional.of(3L), service.reassign(10L, AssignmentEndReason.OVERDUE, null, null));
        assertEquals(AssignmentEndReason.OVERDUE, old.getEndReason());
        assertNotNull(old.getEndedAt());
        assertEquals(a.getGroomAssignedAt().plusHours(72), a.getGroomReviewDueAt());
        var order = inOrder(assignments);
        order.verify(assignments).findPreviousGroomIds(10L);
        order.verify(assignments).findOpenByAdmissionId(10L);
        order.verify(assignments).saveAndFlush(old);
        order.verify(assignments).save(any());
    }

    @Test
    void rotationRestartsExcludingCurrentGroom() {
        pending(1L);
        when(assignments.findPreviousGroomIds(10L)).thenReturn(List.of(1L, 2L, 3L));
        when(workload.selectGroom(Set.of(1L, 2L, 3L), false)).thenReturn(Optional.empty());
        when(workload.selectGroom(Set.of(1L), false)).thenReturn(Optional.of(2L));
        assertEquals(Optional.of(2L), service.reassign(10L, AssignmentEndReason.OVERDUE, null, null));
    }

    @Test
    void onlyRemainingGroomGetsNewWindow() {
        AdmissionApplication a = pending(1L);
        assertEquals(Optional.of(1L), service.reassign(10L, AssignmentEndReason.OVERDUE, null, null));
        assertTrue(a.getGroomReviewDueAt().isAfter(LocalDateTime.now()));
        verify(assignments).save(any());
    }

    @Test
    void fullKeepsAssignmentAndDeadlineWhenNoFreeTarget() {
        AdmissionApplication a = pending(1L);
        LocalDateTime due = a.getGroomReviewDueAt();
        when(workload.isFull(1L)).thenReturn(true);
        assertEquals(Optional.of(1L), service.reassign(10L, AssignmentEndReason.FULL, null, null));
        verify(workload).selectGroom(Set.of(1L), true);
        verify(assignments, never()).findOpenByAdmissionId(any());
        verify(assignments, never()).save(any());
        assertEquals(due, a.getGroomReviewDueAt());
    }

    @Test
    void fullMovesOnlyToNonFullGroomWhoHasNotHadAdmission() {
        pending(1L);
        when(workload.isFull(1L)).thenReturn(true);
        when(assignments.findPreviousGroomIds(10L)).thenReturn(List.of(1L, 2L));
        when(workload.selectGroom(Set.of(1L, 2L), true)).thenReturn(Optional.of(3L));
        assertEquals(Optional.of(3L), service.reassign(10L, AssignmentEndReason.FULL, null, null));
    }

    @Test
    void manualTargetMayBeFullOrHaveReviewedBefore() {
        pending(1L);
        when(stalls.findQuarantineGroomIds()).thenReturn(List.of(1L, 2L));
        assertEquals(Optional.of(2L), service.reassign(10L, AssignmentEndReason.MANUAL, 9L, 2L));
        ArgumentCaptor<AdmissionGroomAssignment> row = ArgumentCaptor.forClass(AdmissionGroomAssignment.class);
        verify(assignments).save(row.capture());
        assertEquals(9L, row.getValue().getAssignedById());
        verify(workload, never()).isFull(any());
    }

    @Test
    void invalidManualTargetDoesNotCloseExistingAssignment() {
        pending(1L);
        assertEquals(400, assertThrows(ResponseStatusException.class,
                () -> service.reassign(10L, AssignmentEndReason.MANUAL, 9L, 99L)).getStatusCode().value());
        verifyNoInteractions(assignments);
    }

    @Test
    void lostQuarantineMovesToEligibleGroom() {
        pending(1L);
        when(stalls.findQuarantineGroomIds()).thenReturn(List.of(2L));
        when(workload.selectGroom(Set.of(1L), false)).thenReturn(Optional.of(2L));
        assertEquals(Optional.of(2L), service.reassign(10L, AssignmentEndReason.LOST_QUARANTINE, null, null));
    }

    @Test
    void staleOverdueOrLostEligibilityJobsDoNotReassign() {
        AdmissionApplication a = pending(1L); a.setGroomReviewDueAt(LocalDateTime.now().plusHours(1));
        assertEquals(Optional.of(1L), service.reassign(10L, AssignmentEndReason.OVERDUE, null, null));
        when(stalls.findQuarantineGroomIds()).thenReturn(List.of(1L));
        assertEquals(Optional.of(1L), service.reassign(10L, AssignmentEndReason.LOST_QUARANTINE, null, null));
        verifyNoInteractions(workload, assignments);
    }

    @Test
    void overdueBatchUsesIndependentTransactionsAndRechecksDeadline() {
        AdmissionApplication a = pending(1L); a.setGroomReviewDueAt(LocalDateTime.now().plusHours(1));
        when(admissions.findByStatusAndGroomReviewDueAtLessThanEqualOrderByGroomReviewDueAtAscIdAsc(
                eq(AdmissionStatus.GROOM_REVIEW), any())).thenReturn(List.of(a));
        when(transactions.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        service.reassignOverdue();
        verify(transactions).getTransaction(argThat(d -> d.getPropagationBehavior()
                == TransactionDefinition.PROPAGATION_REQUIRES_NEW));
        verify(transactions).commit(any());
        verifyNoInteractions(workload, assignments);
    }

    @Test
    void lostQuarantineBatchRechecksEligibilityUnderAdmissionLock() {
        AdmissionApplication a = pending(1L);
        when(stalls.findQuarantineGroomIds()).thenReturn(List.of(2L));
        when(admissions.findByStatus(AdmissionStatus.GROOM_REVIEW)).thenReturn(List.of(a));
        when(transactions.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        when(workload.selectGroom(Set.of(1L), false)).thenReturn(Optional.of(2L));
        service.reassignLostQuarantine();
        assertEquals(2L, a.getAssignedGroomId());
        verify(admissions).findByIdForUpdate(10L);
        verify(transactions).commit(any());
    }

    @Test
    void fullBatchDoesNotMoveAnAdmissionReassignedSinceScan() {
        AdmissionApplication a = pending(2L);
        when(workload.isFull(1L)).thenReturn(true);
        when(admissions.findByStatusAndAssignedGroomIdOrderBySubmittedAtAscIdAsc(AdmissionStatus.GROOM_REVIEW, 1L))
                .thenReturn(List.of(a));
        when(transactions.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        service.reassignPendingOfFullGroom(1L);
        assertEquals(2L, a.getAssignedGroomId());
        verify(workload, never()).selectGroom(anySet(), anyBoolean());
        verifyNoInteractions(assignments);
    }

    @Test
    void closeDecidedAndReopenedAreIdempotent() {
        AdmissionApplication a = pending(1L);
        a.setStatus(AdmissionStatus.WAITING_FOR_ARRIVAL); a.setGroomDecision(ReviewDecision.APPROVED);
        AdmissionGroomAssignment row = new AdmissionGroomAssignment();
        when(assignments.findOpenByAdmissionId(10L)).thenReturn(Optional.of(row));
        service.closeAsDecided(10L);
        assertEquals(AssignmentEndReason.DECIDED, row.getEndReason());
        a.setStatus(AdmissionStatus.GROOM_REVIEW); a.setGroomDecision(null);
        service.closeAsReopened(10L);
        assertEquals(AssignmentEndReason.REOPENED, row.getEndReason());
        assertNull(a.getAssignedGroomId());
        assertNull(a.getGroomAssignedAt());
        assertNull(a.getGroomReviewDueAt());
    }
    @Test
    void managerCanAssignAnUnassignedPendingAdmission() {
        AdmissionApplication admission = pending(null);
        when(stalls.findQuarantineGroomIds()).thenReturn(List.of(2L));
        assertEquals(Optional.of(2L), service.reassign(10L, AssignmentEndReason.MANUAL, 9L, 2L));
        assertEquals(2L, admission.getAssignedGroomId());
    }

    @Test
    void historyIsRestrictedToManagersAndCurrentAssignedGroom() {
        AdmissionApplication admission = new AdmissionApplication();
        admission.setAssignedGroomId(2L);
        when(admissions.findById(10L)).thenReturn(Optional.of(admission));
        AdmissionGroomAssignment row = new AdmissionGroomAssignment();
        row.setId(100L); row.setGroomId(2L); row.setAssignedAt(LocalDateTime.now());
        when(assignments.findByAdmissionIdOrderByAssignedAtAsc(10L)).thenReturn(List.of(row));
        assertEquals(1, service.history(10L, new AuthenticatedUser(9L, "m@x", "CLUB_MANAGER")).size());
        assertEquals(1, service.history(10L, new AuthenticatedUser(2L, "g@x", "GROOM")).size());
        assertEquals(403, assertThrows(ResponseStatusException.class,
                () -> service.history(10L, new AuthenticatedUser(1L, "old@x", "GROOM"))).getStatusCode().value());
        assertEquals(403, assertThrows(ResponseStatusException.class,
                () -> service.history(10L, new AuthenticatedUser(2L, "owner@x", "HORSE_OWNER"))).getStatusCode().value());
    }

    @Test
    void manualMissingAdmissionReturnsNotFound() {
        assertEquals(404, assertThrows(ResponseStatusException.class,
                () -> service.reassign(999L, AssignmentEndReason.MANUAL, 9L, 2L)).getStatusCode().value());
    }
}
