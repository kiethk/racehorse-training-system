package com.rtms.backend.admission.service;

import com.rtms.backend.admission.entity.AdmissionApplication;
import com.rtms.backend.admission.entity.AdmissionGroomAssignment;
import com.rtms.backend.admission.enums.AdmissionStatus;
import com.rtms.backend.admission.enums.AssignmentEndReason;
import com.rtms.backend.admission.repository.AdmissionApplicationRepository;
import com.rtms.backend.admission.repository.AdmissionGroomAssignmentRepository;
import com.rtms.backend.stable.repository.StableStallRepository;
import com.rtms.backend.admission.dto.GroomAssignmentHistoryItem;
import com.rtms.backend.identity.entity.User;
import com.rtms.backend.identity.repository.UserRepository;
import com.rtms.backend.security.AuthenticatedUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Service
public class GroomAssignmentService {
    public static final int REVIEW_WINDOW_HOURS = 72;
    private static final Logger log = LoggerFactory.getLogger(GroomAssignmentService.class);
    private final AdmissionApplicationRepository admissions;
    private final AdmissionGroomAssignmentRepository assignments;
    private final GroomWorkloadService workload;
    private final StableStallRepository stalls;
    private final TransactionTemplate perAdmission;
    private final GroomAssignmentNotificationService notifications;
    private final UserRepository users;

    public GroomAssignmentService(AdmissionApplicationRepository admissions,
                                  AdmissionGroomAssignmentRepository assignments,
                                  GroomWorkloadService workload, StableStallRepository stalls,
                                  PlatformTransactionManager transactionManager,
                                  GroomAssignmentNotificationService notifications, UserRepository users) {
        this.admissions = admissions;
        this.assignments = assignments;
        this.workload = workload;
        this.stalls = stalls;
        this.notifications = notifications;
        this.users = users;
        this.perAdmission = new TransactionTemplate(transactionManager);
        this.perAdmission.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Optional<Long> assignIfUnassigned(Long admissionId) {
        AdmissionApplication admission = lockPending(admissionId);
        if (admission == null) {
            return Optional.empty();
        }
        if (admission.getAssignedGroomId() != null) {
            return Optional.of(admission.getAssignedGroomId());
        }
        Optional<Long> selected = workload.selectGroom(Set.of(), false);
        selected.ifPresent(id -> open(admission, id, null, null, LocalDateTime.now()));
        return selected;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Optional<Long> reassign(Long admissionId, AssignmentEndReason reason,
                                    Long managerId, Long targetGroomId) {
        AdmissionApplication admission = admissions.findByIdForUpdate(admissionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Admission not found"));
        if (reason == AssignmentEndReason.MANUAL) {
            if (admission.getStatus() != AdmissionStatus.GROOM_REVIEW) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Admission is not awaiting Groom review");
            }
            if (targetGroomId == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Groom is required");
            }
        }
        return reassignLocked(admission.getStatus() == AdmissionStatus.GROOM_REVIEW ? admission : null,
                reason, managerId, targetGroomId);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void reassignOverdue() {
        List<AdmissionApplication> pending = admissions
                .findByStatusAndGroomReviewDueAtLessThanEqualOrderByGroomReviewDueAtAscIdAsc(
                        AdmissionStatus.GROOM_REVIEW, LocalDateTime.now());
        reassignBatch(pending, AssignmentEndReason.OVERDUE, null);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void reassignLostQuarantine() {
        Set<Long> eligible = new HashSet<>(stalls.findQuarantineGroomIds());
        List<AdmissionApplication> pending = admissions.findByStatus(AdmissionStatus.GROOM_REVIEW).stream()
                .filter(a -> a.getAssignedGroomId() != null && !eligible.contains(a.getAssignedGroomId())).toList();
        reassignBatch(pending, AssignmentEndReason.LOST_QUARANTINE, null);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void reassignPendingOfFullGroom(Long groomId) {
        if (workload.isFull(groomId)) {
            reassignBatch(admissions.findByStatusAndAssignedGroomIdOrderBySubmittedAtAscIdAsc(
                    AdmissionStatus.GROOM_REVIEW, groomId), AssignmentEndReason.FULL, groomId);
        }
    }

    @Transactional
    public void closeAsDecided(Long admissionId) {
        admissions.findByIdForUpdate(admissionId).ifPresent(a -> {
            if (a.getStatus() != AdmissionStatus.GROOM_REVIEW && a.getGroomDecision() != null) {
                close(a.getId(), AssignmentEndReason.DECIDED, LocalDateTime.now());
            }
        });
    }

    @Transactional
    public void closeAsReopened(Long admissionId) {
        AdmissionApplication admission = lockPending(admissionId);
        if (admission != null && admission.getGroomDecision() == null) {
            close(admissionId, AssignmentEndReason.REOPENED, LocalDateTime.now());
            admission.setAssignedGroomId(null);
            admission.setGroomAssignedAt(null);
            admission.setGroomReviewDueAt(null);
            admissions.save(admission);
        }
    }

    private AdmissionApplication lockPending(Long admissionId) {
        return admissions.findByIdForUpdate(admissionId)
                .filter(a -> a.getStatus() == AdmissionStatus.GROOM_REVIEW).orElse(null);
    }

    private Optional<Long> reassignLocked(AdmissionApplication admission, AssignmentEndReason reason,
                                          Long managerId, Long targetGroomId) {
        if (admission == null || (admission.getAssignedGroomId() == null && targetGroomId == null)) {
            return Optional.empty();
        }
        if (reason == null || reason == AssignmentEndReason.DECIDED || reason == AssignmentEndReason.REOPENED) {
            throw new IllegalArgumentException("A reassignment reason is required");
        }
        Long previous = admission.getAssignedGroomId();
        LocalDateTime now = LocalDateTime.now();
        if (targetGroomId == null) {
            if (reason == AssignmentEndReason.OVERDUE && (admission.getGroomReviewDueAt() == null
                    || admission.getGroomReviewDueAt().isAfter(now))) {
                return Optional.of(previous);
            }
            if (reason == AssignmentEndReason.LOST_QUARANTINE && stalls.findQuarantineGroomIds().contains(previous)) {
                return Optional.of(previous);
            }
            if (reason == AssignmentEndReason.FULL && !workload.isFull(previous)) {
                return Optional.of(previous);
            }
        }
        Optional<Long> selected;
        if (targetGroomId != null) {
            // Take the same candidate locks as automatic selection before validating a manual target.
            workload.selectGroom(Set.of(), false);
            if (!stalls.findQuarantineGroomIds().contains(targetGroomId)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Target must be an active Quarantine Groom");
            }
            selected = Optional.of(targetGroomId);
        } else {
            Set<Long> alreadyHad = new HashSet<>(assignments.findPreviousGroomIds(admission.getId()));
            alreadyHad.add(previous);
            selected = workload.selectGroom(alreadyHad, reason == AssignmentEndReason.FULL);
            if (selected.isEmpty() && reason != AssignmentEndReason.FULL) {
                selected = workload.selectGroom(Set.of(previous), false);
            }
        }
        if (selected.isEmpty()) {
            if (reason == AssignmentEndReason.FULL) {
                return Optional.of(previous);
            }
            selected = Optional.of(previous);
        }
        close(admission.getId(), reason, now);
        open(admission, selected.get(), managerId, reason, now);
        return selected;
    }

    private void reassignBatch(List<AdmissionApplication> pending, AssignmentEndReason reason, Long expectedGroomId) {
        for (AdmissionApplication candidate : pending) {
            try {
                // Each admission commits before the next selection. Never self-invoke a transactional method.
                perAdmission.executeWithoutResult(status -> {
                    AdmissionApplication locked = lockPending(candidate.getId());
                    if (locked != null && (expectedGroomId == null
                            || Objects.equals(locked.getAssignedGroomId(), expectedGroomId))) {
                        reassignLocked(locked, reason, null, null);
                    }
                });
            } catch (Exception e) {
                log.error("Groom reassignment failed for admission #{}", candidate.getId(), e);
            }
        }
    }

    private void close(Long admissionId, AssignmentEndReason reason, LocalDateTime now) {
        assignments.findOpenByAdmissionId(admissionId).ifPresent(row -> {
            row.setEndedAt(now);
            row.setEndReason(reason);
            // Flush the closed row before inserting its successor under the partial unique index.
            assignments.saveAndFlush(row);
        });
    }

    private void open(AdmissionApplication admission, Long groomId, Long managerId,
                      AssignmentEndReason reason, LocalDateTime now) {
        Long previous = admission.getAssignedGroomId();
        AdmissionGroomAssignment row = new AdmissionGroomAssignment();
        row.setAdmissionId(admission.getId());
        row.setGroomId(groomId);
        row.setAssignedAt(now);
        row.setAssignedById(managerId);
        assignments.save(row);
        admission.setAssignedGroomId(groomId);
        admission.setGroomAssignedAt(now);
        admission.setGroomReviewDueAt(now.plusHours(REVIEW_WINDOW_HOURS));
        admissions.save(admission);
        afterAssignment(admission, row, previous, reason);
    }

    private void afterAssignment(AdmissionApplication admission, AdmissionGroomAssignment row,
                                 Long previousGroomId, AssignmentEndReason reason) {
        notifications.afterAssignment(admission, row, previousGroomId, reason);
    }

    @Transactional(readOnly = true)
    public List<GroomAssignmentHistoryItem> history(Long admissionId, AuthenticatedUser viewer) {
        AdmissionApplication admission = admissions.findById(admissionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Admission not found"));
        if (!"CLUB_MANAGER".equals(viewer.getRole()) && !("GROOM".equals(viewer.getRole())
                && Objects.equals(admission.getAssignedGroomId(), viewer.getUserId()))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admission is not assigned to you");
        }
        return assignments.findByAdmissionIdOrderByAssignedAtAsc(admissionId).stream()
                .map(row -> new GroomAssignmentHistoryItem(row.getId(), row.getGroomId(), name(row.getGroomId()),
                        row.getAssignedAt(), row.getEndedAt(), row.getEndReason(), name(row.getAssignedById())))
                .toList();
    }

    private String name(Long userId) {
        return userId == null ? null : users.findById(userId).map(User::getFullName).orElse(null);
    }
}
