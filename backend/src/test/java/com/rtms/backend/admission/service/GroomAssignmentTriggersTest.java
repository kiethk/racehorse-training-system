package com.rtms.backend.admission.service;

import com.rtms.backend.admission.entity.AdmissionApplication;
import com.rtms.backend.admission.enums.AdmissionStatus;
import com.rtms.backend.admission.repository.AdmissionApplicationRepository;
import com.rtms.backend.event.AdmissionEnteredGroomReviewEvent;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class GroomAssignmentTriggersTest {
    private final GroomAssignmentService assignments = mock(GroomAssignmentService.class);
    private final AdmissionApplicationRepository admissions = mock(AdmissionApplicationRepository.class);
    private final GroomAssignmentTriggers triggers = new GroomAssignmentTriggers(assignments, admissions);

    @Test
    void listenerRunsAfterCommitAndAssignsSubmittedAdmission() throws Exception {
        var listener = GroomAssignmentTriggers.class.getMethod("onEnteredGroomReview", AdmissionEnteredGroomReviewEvent.class)
                .getAnnotation(TransactionalEventListener.class);
        assertEquals(TransactionPhase.AFTER_COMMIT, listener.phase());
        triggers.onEnteredGroomReview(new AdmissionEnteredGroomReviewEvent(10L));
        verify(assignments).assignIfUnassigned(10L);
    }

    @Test
    void periodicJobsAssignOldestFirstAndContinueAfterFailure() {
        AdmissionApplication first = new AdmissionApplication(); first.setId(1L);
        AdmissionApplication second = new AdmissionApplication(); second.setId(2L);
        when(admissions.findByStatusAndAssignedGroomIdIsNullOrderBySubmittedAtAscIdAsc(AdmissionStatus.GROOM_REVIEW))
                .thenReturn(List.of(first, second));
        when(assignments.assignIfUnassigned(1L)).thenThrow(new IllegalStateException("Temporary conflict"));
        triggers.runPeriodicJobs();
        var order = inOrder(assignments);
        order.verify(assignments).assignIfUnassigned(1L);
        order.verify(assignments).assignIfUnassigned(2L);
        order.verify(assignments).reassignLostQuarantine();
        order.verify(assignments).reassignOverdue();
    }
    @Test
    void fullAndLostQuarantineReassignmentWaitForCommit() {
        TransactionSynchronizationManager.initSynchronization();
        try {
            triggers.reassignFullAfterCommit(7L);
            triggers.reassignLostQuarantineAfterCommit();
            verifyNoInteractions(assignments);
            TransactionSynchronizationManager.getSynchronizations().forEach(sync -> sync.afterCommit());
            verify(assignments).reassignPendingOfFullGroom(7L);
            verify(assignments).reassignLostQuarantine();
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void rollbackDoesNotReassign() {
        TransactionSynchronizationManager.initSynchronization();
        try {
            triggers.reassignFullAfterCommit(7L);
            TransactionSynchronizationManager.getSynchronizations().forEach(sync -> sync.afterCompletion(1));
            verifyNoInteractions(assignments);
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }
}
