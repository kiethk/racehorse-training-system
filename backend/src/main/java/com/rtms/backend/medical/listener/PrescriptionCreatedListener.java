package com.rtms.backend.medical.listener;

import com.rtms.backend.horse.entity.Horse;
import com.rtms.backend.horse.repository.HorseRepository;
import com.rtms.backend.medical.event.PrescriptionCreatedEvent;
import com.rtms.backend.notification.service.NotificationService;
import com.rtms.backend.notification.service.NotificationTypes;
import com.rtms.backend.stable.entity.StableStall;
import com.rtms.backend.stable.repository.StableStallRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.HashSet;
import java.util.Set;

/**
 * Chỉ chạy sau khi transaction hoàn tất ca khám COMMIT (pattern ClinicalVectorSyncListener).
 * Bắn thông báo PRESCRIPTION_CREATED tới Groom và Trainer phụ trách ngựa (Track 2a / WF-04).
 */
@Component
public class PrescriptionCreatedListener {

    private static final Logger log = LoggerFactory.getLogger(PrescriptionCreatedListener.class);

    private final HorseRepository horseRepository;
    private final StableStallRepository stableStallRepository;
    private final NotificationService notificationService;

    public PrescriptionCreatedListener(HorseRepository horseRepository,
                                       StableStallRepository stableStallRepository,
                                       NotificationService notificationService) {
        this.horseRepository = horseRepository;
        this.stableStallRepository = stableStallRepository;
        this.notificationService = notificationService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPrescriptionCreated(PrescriptionCreatedEvent event) {
        if (event == null || event.horseId() == null || event.prescriptionIds() == null || event.prescriptionIds().isEmpty()) {
            return;
        }
        log.info("PrescriptionCreated: schedule={} record={} horse={} rx={}",
                event.scheduleId(), event.healthRecordId(), event.horseId(), event.prescriptionIds());

        Horse horse = horseRepository.findById(event.horseId()).orElse(null);
        String horseName = horse != null && horse.getName() != null ? horse.getName() : ("#" + event.horseId());
        int rxCount = event.prescriptionIds().size();
        String title = "Medication Prescribed: " + horseName;
        String message = "New prescription recorded for " + horseName + " (" + rxCount + " " + (rxCount == 1 ? "medication" : "medications")
                + "). Review administration instructions and withdrawal guidelines.";

        Set<Long> recipients = new HashSet<>();

        // Groom assigned to stall
        if (horse != null && horse.getCurrentStallId() != null) {
            stableStallRepository.findById(horse.getCurrentStallId())
                    .map(StableStall::getGroomId)
                    .ifPresent(recipients::add);
        }

        // Trainer responsible for horse
        horseRepository.findTrainerIdByHorseId(event.horseId()).ifPresent(recipients::add);

        for (Long recipientId : recipients) {
            if (recipientId != null) {
                try {
                    notificationService.sendAssignmentNotification(
                            recipientId,
                            NotificationTypes.REFERENCE_CARE_SCHEDULE,
                            event.scheduleId(),
                            event.horseId(),
                            NotificationTypes.PRESCRIPTION_CREATED,
                            title,
                            message
                    );
                } catch (Exception ex) {
                    log.warn("Failed to dispatch prescription notification to recipient {}", recipientId, ex);
                }
            }
        }
    }
}
