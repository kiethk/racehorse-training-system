package com.rtms.backend.admission.service;

import com.rtms.backend.admission.entity.AdmissionApplication;
import com.rtms.backend.admission.entity.AdmissionGroomAssignment;
import com.rtms.backend.admission.entity.CandidateHorseProfile;
import com.rtms.backend.admission.enums.AssignmentEndReason;
import com.rtms.backend.admission.repository.CandidateHorseProfileRepository;
import com.rtms.backend.identity.entity.User;
import com.rtms.backend.identity.repository.UserRepository;
import com.rtms.backend.notification.entity.Notification;
import com.rtms.backend.notification.service.EmailService;
import com.rtms.backend.notification.service.NotificationService;
import com.rtms.backend.notification.service.NotificationTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.time.LocalDateTime;

@Service
public class GroomAssignmentNotificationService {
    private static final Logger log = LoggerFactory.getLogger(GroomAssignmentNotificationService.class);
    private final UserRepository users;
    private final CandidateHorseProfileRepository candidates;
    private final NotificationService notifications;
    private final EmailService emails;
    private final TransactionTemplate delivery;
    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;

    public GroomAssignmentNotificationService(UserRepository users, CandidateHorseProfileRepository candidates,
            NotificationService notifications, EmailService emails, PlatformTransactionManager transactions) {
        this.users = users;
        this.candidates = candidates;
        this.notifications = notifications;
        this.emails = emails;
        this.delivery = new TransactionTemplate(transactions);
        this.delivery.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    public void afterAssignment(AdmissionApplication admission, AdmissionGroomAssignment row,
            Long previousGroomId, AssignmentEndReason reason) {
        Long admissionId = admission.getId();
        Long assignmentId = row.getId();
        Long groomId = row.getGroomId();
        LocalDateTime due = admission.getGroomReviewDueAt();
        Runnable action = () -> {
            try {
                // AFTER_COMMIT still has the old transaction's resources bound, so writes need a new transaction.
                delivery.executeWithoutResult(status -> deliver(admissionId, assignmentId, groomId,
                        previousGroomId, reason, due));
            } catch (Exception e) {
                log.error("Could not notify groom assignment #{}", assignmentId, e);
            }
        };
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() { action.run(); }
            });
        } else {
            action.run();
        }
    }

    private void deliver(Long admissionId, Long assignmentId, Long groomId, Long previousGroomId,
            AssignmentEndReason reason, LocalDateTime due) {
        String horse = candidates.findByAdmissionId(admissionId).map(CandidateHorseProfile::getName)
                .orElse("Candidate horse");
        User groom = users.findById(groomId).orElse(null);
        notify(groomId, admissionId, assignmentId, NotificationTypes.ADMISSION_GROOM_REVIEW_ASSIGNED,
                "New admission to review", horse + " is assigned to you. Review due: " + due);
        if (groom != null) {
            emails.sendAdmissionGroomReviewAssignedEmail(groom.getEmail(), groom.getFullName(), horse, due,
                    frontendUrl + "/groom/admissions/" + admissionId);
        }
        if (reason == null) return;
        String label = reasonLabel(reason);
        if (previousGroomId != null) {
            notify(previousGroomId, admissionId, assignmentId, NotificationTypes.ADMISSION_GROOM_REVIEW_REASSIGNED,
                    "Admission moved to another groom", horse + ". Reason: " + label);
        }
        String oldName = previousGroomId == null ? "Unassigned"
                : users.findById(previousGroomId).map(User::getFullName).orElse("Previous groom");
        String newName = groom == null ? "Assigned groom" : groom.getFullName();
        for (User manager : users.findActiveClubManagers()) {
            notify(manager.getId(), admissionId, assignmentId, NotificationTypes.ADMISSION_GROOM_REVIEW_REASSIGNED,
                    "Admission reassigned", horse + " moved from " + oldName + " to " + newName + ". Reason: " + label);
        }
    }

    private void notify(Long recipient, Long admission, Long assignment, String type, String title, String message) {
        notifications.createNotification(recipient, title, message, type, NotificationTypes.REFERENCE_ADMISSION,
                admission, Notification.buildDeduplicationKey(recipient, NotificationTypes.REFERENCE_ADMISSION,
                        admission, type) + ":assignment:" + assignment);
    }

    static String reasonLabel(AssignmentEndReason reason) {
        return switch (reason) {
            case DECIDED -> "Decided";
            case OVERDUE -> "Not decided in 72 hours";
            case FULL -> "Stalls full";
            case LOST_QUARANTINE -> "No longer a quarantine groom";
            case MANUAL -> "Reassigned by manager";
            case REOPENED -> "Reopened";
        };
    }
}
