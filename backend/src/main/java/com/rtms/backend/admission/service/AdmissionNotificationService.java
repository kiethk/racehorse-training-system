package com.rtms.backend.admission.service;

import com.rtms.backend.admission.entity.AdmissionApplication;
import com.rtms.backend.horse.entity.Horse;
import com.rtms.backend.identity.entity.User;
import com.rtms.backend.identity.repository.UserRepository;
import com.rtms.backend.notification.entity.Notification;
import com.rtms.backend.notification.service.EmailService;
import com.rtms.backend.notification.service.NotificationService;
import com.rtms.backend.notification.service.NotificationTypes;
import com.rtms.backend.stable.entity.StableStall;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDate;

@Service
public class AdmissionNotificationService {

    private static final Logger log = LoggerFactory.getLogger(AdmissionNotificationService.class);

    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final EmailService emailService;

    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;

    public AdmissionNotificationService(
            UserRepository userRepository,
            NotificationService notificationService,
            EmailService emailService) {
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.emailService = emailService;
    }

    /**
     * Executes the given action only after the surrounding database transaction commits.
     * Prevents sending phantom emails if the transaction rolls back.
     */
    private void executeAfterCommit(Runnable action) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    try {
                        action.run();
                    } catch (Exception e) {
                        log.warn("[AdmissionNotificationService] Failed to execute afterCommit action: {}", e.getMessage(), e);
                    }
                }
            });
        } else {
            action.run();
        }
    }

    private User findOwner(Long ownerId) {
        if (ownerId == null) {
            return null;
        }
        return userRepository.findById(ownerId).orElse(null);
    }

    /**
     * Stage 1: Quarantine Groom reserves quarantine stall for admission.
     * Alerts owner to deliver horse before arrivalDeadlineAt.
     */
    public void notifyOwnerWaitingForArrival(AdmissionApplication admission, String horseName, StableStall quarantineStall) {
        User owner = findOwner(admission.getOwnerId());
        if (owner == null) {
            log.warn("[AdmissionNotificationService] Owner #{} not found for admission #{}", admission.getOwnerId(), admission.getId());
            return;
        }

        LocalDate deadline = admission.getArrivalDeadlineAt() != null
                ? admission.getArrivalDeadlineAt().toLocalDate()
                : LocalDate.now().plusDays(14);
        String stallCode = quarantineStall != null ? quarantineStall.getStallCode() : "Assigned Quarantine Stall";
        String effectiveHorseName = (horseName != null && !horseName.isBlank()) ? horseName : "your horse";

        // 1. In-app notification
        notificationService.createNotification(
                owner.getId(),
                "Admission accepted - Bring horse to facility",
                "Quarantine stall " + stallCode + " is reserved for " + effectiveHorseName
                        + ". Please deliver your horse to Riverside before " + deadline + ".",
                NotificationTypes.ADMISSION_WAITING_FOR_ARRIVAL,
                NotificationTypes.REFERENCE_ADMISSION,
                admission.getId(),
                Notification.buildDeduplicationKey(owner.getId(), NotificationTypes.REFERENCE_ADMISSION,
                        admission.getId(), NotificationTypes.ADMISSION_WAITING_FOR_ARRIVAL)
        );

        // 2. Async email guarded by afterCommit
        String admissionUrl = frontendUrl + "/owner/admissions/" + admission.getId();
        executeAfterCommit(() -> emailService.sendAdmissionWaitingForArrivalEmail(
                owner.getEmail(),
                owner.getFullName(),
                effectiveHorseName,
                stallCode,
                deadline,
                admission.getGroomFeedback(),
                admissionUrl
        ));
    }

    /**
     * Stage 2: Club Manager gives final approval. Regular stall allocated, horse is eligible for training.
     */
    public void notifyOwnerApproved(AdmissionApplication admission, Horse horse, StableStall regularStall) {
        User owner = findOwner(admission.getOwnerId());
        if (owner == null) {
            log.warn("[AdmissionNotificationService] Owner #{} not found for admission #{}", admission.getOwnerId(), admission.getId());
            return;
        }

        String horseName = horse != null ? horse.getName() : "Your horse";
        String stallCode = regularStall != null ? regularStall.getStallCode() : "Regular Stall";

        // 1. In-app notification
        notificationService.createNotification(
                owner.getId(),
                "Horse admission approved - Welcome to Riverside",
                horseName + " has passed clinical and conditioning assessments and is now assigned to stall "
                        + stallCode + " as an active racehorse.",
                NotificationTypes.ADMISSION_APPROVED,
                NotificationTypes.REFERENCE_ADMISSION,
                admission.getId(),
                Notification.buildDeduplicationKey(owner.getId(), NotificationTypes.REFERENCE_ADMISSION,
                        admission.getId(), NotificationTypes.ADMISSION_APPROVED)
        );

        // 2. Async email guarded by afterCommit
        Long horseId = horse != null ? horse.getId() : admission.getHorseId();
        String horseUrl = horseId != null ? frontendUrl + "/owner/horses/" + horseId : frontendUrl + "/owner/horses";
        executeAfterCommit(() -> emailService.sendAdmissionApprovedEmail(
                owner.getEmail(),
                owner.getFullName(),
                horseName,
                stallCode,
                admission.getManagerFeedback(),
                horseUrl
        ));
    }

    /**
     * Rejection at either Groom or Manager review stage.
     */
    public void notifyOwnerRejected(AdmissionApplication admission, String horseName, String stage, String feedback) {
        User owner = findOwner(admission.getOwnerId());
        if (owner == null) {
            log.warn("[AdmissionNotificationService] Owner #{} not found for admission #{}", admission.getOwnerId(), admission.getId());
            return;
        }

        String effectiveHorseName = (horseName != null && !horseName.isBlank()) ? horseName : "your horse";
        String stageLabel = stage != null ? stage : "Review";
        String reason = (feedback != null && !feedback.isBlank()) ? feedback : "No specific details provided";

        // 1. In-app notification
        notificationService.createNotification(
                owner.getId(),
                "Admission application rejected",
                "Your application for " + effectiveHorseName + " was not accepted at " + stageLabel + ". Reason: " + reason,
                NotificationTypes.ADMISSION_REJECTED,
                NotificationTypes.REFERENCE_ADMISSION,
                admission.getId(),
                Notification.buildDeduplicationKey(owner.getId(), NotificationTypes.REFERENCE_ADMISSION,
                        admission.getId(), NotificationTypes.ADMISSION_REJECTED) + ":" + stageLabel
        );

        // 2. Async email guarded by afterCommit
        String admissionUrl = frontendUrl + "/owner/admissions/" + admission.getId();
        executeAfterCommit(() -> emailService.sendAdmissionRejectedEmail(
                owner.getEmail(),
                owner.getFullName(),
                effectiveHorseName,
                stageLabel,
                feedback,
                admissionUrl
        ));
    }

    /**
     * Expired 14-day quarantine reservation.
     */
    public void notifyOwnerArrivalExpired(AdmissionApplication admission, String horseName) {
        User owner = findOwner(admission.getOwnerId());
        if (owner == null) {
            log.warn("[AdmissionNotificationService] Owner #{} not found for admission #{}", admission.getOwnerId(), admission.getId());
            return;
        }

        String effectiveHorseName = (horseName != null && !horseName.isBlank()) ? horseName : "your horse";

        // 1. In-app notification
        notificationService.createNotification(
                owner.getId(),
                "Quarantine reservation expired",
                "The 14-day arrival window for " + effectiveHorseName + " has expired. The quarantine stall has been released.",
                NotificationTypes.ADMISSION_ARRIVAL_EXPIRED,
                NotificationTypes.REFERENCE_ADMISSION,
                admission.getId(),
                Notification.buildDeduplicationKey(owner.getId(), NotificationTypes.REFERENCE_ADMISSION,
                        admission.getId(), NotificationTypes.ADMISSION_ARRIVAL_EXPIRED)
        );

        // 2. Async email guarded by afterCommit
        String admissionUrl = frontendUrl + "/owner/admissions/" + admission.getId();
        executeAfterCommit(() -> emailService.sendAdmissionArrivalExpiredEmail(
                owner.getEmail(),
                owner.getFullName(),
                effectiveHorseName,
                admissionUrl
        ));
    }
}
