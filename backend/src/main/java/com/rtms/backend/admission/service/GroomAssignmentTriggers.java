package com.rtms.backend.admission.service;

import com.rtms.backend.admission.enums.AdmissionStatus;
import com.rtms.backend.admission.repository.AdmissionApplicationRepository;
import com.rtms.backend.event.AdmissionEnteredGroomReviewEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class GroomAssignmentTriggers {
    private static final Logger log = LoggerFactory.getLogger(GroomAssignmentTriggers.class);
    private final GroomAssignmentService assignments;
    private final AdmissionApplicationRepository admissions;

    public GroomAssignmentTriggers(GroomAssignmentService assignments, AdmissionApplicationRepository admissions) {
        this.assignments = assignments;
        this.admissions = admissions;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onEnteredGroomReview(AdmissionEnteredGroomReviewEvent event) {
        assign(event.admissionId());
    }

    public void reassignFullAfterCommit(Long groomId) {
        afterCommit(() -> assignments.reassignPendingOfFullGroom(groomId));
    }

    public void reassignLostQuarantineAfterCommit() {
        afterCommit(assignments::reassignLostQuarantine);
    }

    private void afterCommit(Runnable action) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    runSafely(action);
                }
            });
        } else {
            runSafely(action);
        }
    }

    private void runSafely(Runnable action) {
        try {
            action.run();
        } catch (Exception e) {
            log.error("Groom reassignment failed and will retry", e);
        }
    }

    public void runPeriodicJobs() {
        admissions.findByStatusAndAssignedGroomIdIsNullOrderBySubmittedAtAscIdAsc(AdmissionStatus.GROOM_REVIEW)
                .forEach(a -> assign(a.getId()));
        assignments.reassignLostQuarantine();
        assignments.reassignOverdue();
    }

    private void assign(Long admissionId) {
        try {
            assignments.assignIfUnassigned(admissionId);
        } catch (Exception e) {
            log.error("Groom assignment failed for admission #{} and will retry", admissionId, e);
        }
    }
}
