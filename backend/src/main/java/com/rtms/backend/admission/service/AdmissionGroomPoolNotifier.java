package com.rtms.backend.admission.service;

import com.rtms.backend.admission.entity.AdmissionApplication;
import com.rtms.backend.admission.entity.CandidateHorseProfile;
import com.rtms.backend.admission.enums.AdmissionStatus;
import com.rtms.backend.admission.repository.AdmissionApplicationRepository;
import com.rtms.backend.admission.repository.CandidateHorseProfileRepository;
import com.rtms.backend.notification.entity.Notification;
import com.rtms.backend.notification.service.NotificationService;
import com.rtms.backend.notification.service.NotificationTypes;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Tells grooms about admissions entering their review pool. Written inside the caller's transaction,
 * so a rolled-back submission or reopen leaves no notification behind.
 */
@Service
public class AdmissionGroomPoolNotifier {

    private final AdmissionGroomPoolService groomPool;
    private final AdmissionApplicationRepository admissions;
    private final CandidateHorseProfileRepository candidates;
    private final NotificationService notifications;

    public AdmissionGroomPoolNotifier(AdmissionGroomPoolService groomPool, AdmissionApplicationRepository admissions,
            CandidateHorseProfileRepository candidates, NotificationService notifications) {
        this.groomPool = groomPool;
        this.admissions = admissions;
        this.candidates = candidates;
        this.notifications = notifications;
    }

    /** A new submission: the least-loaded grooms see it first, so they hear about it. */
    public void notifySubmitted(AdmissionApplication admission, String horseName) {
        for (Long groomId : groomPool.leastLoadedGroomIds()) {
            send(groomId, admission, horseName + " was submitted for admission and is waiting for groom review.", null);
        }
    }

    /**
     * A reopened admission is a fresh review. The key carries the new start time, so grooms who were
     * told about its first submission hear about it again.
     */
    public void notifyReopened(AdmissionApplication admission) {
        String suffix = "reopen:" + epoch(admission.getGroomReviewStartedAt());
        for (Long groomId : groomPool.leastLoadedGroomIds()) {
            send(groomId, admission, horseName(admission) + " is back in groom review after its arrival window expired.", suffix);
        }
    }

    /**
     * Admissions that waited past the fallback window are now shown to every groom with a free quarantine
     * stall; tell the ones who could not see them before. Each admission is handled once.
     *
     * @return how many admissions were widened
     */
    @Transactional
    public int notifyWidenedPools() {
        List<AdmissionApplication> due = admissions
                .findByStatusAndGroomPoolWidenedAtIsNullAndGroomReviewStartedAtLessThanEqualOrderByGroomReviewStartedAtAscIdAsc(
                        AdmissionStatus.GROOM_REVIEW, groomPool.fallbackCutoff());
        if (due.isEmpty()) return 0;

        Set<Long> leastLoaded = new HashSet<>(groomPool.leastLoadedGroomIds());
        List<Long> newlyShown = groomPool.eligibleGroomIds().stream().filter(id -> !leastLoaded.contains(id)).toList();
        int widened = 0;
        for (AdmissionApplication candidate : due) {
            AdmissionApplication admission = admissions.findByIdForUpdate(candidate.getId()).orElse(null);
            if (admission == null || admission.getStatus() != AdmissionStatus.GROOM_REVIEW
                    || admission.getGroomPoolWidenedAt() != null) {
                continue;
            }
            String suffix = "widened:" + epoch(admission.getGroomReviewStartedAt());
            for (Long groomId : newlyShown) {
                send(groomId, admission, horseName(admission)
                        + " has waited for groom review for a while and is now open to you.", suffix);
            }
            admission.setGroomPoolWidenedAt(LocalDateTime.now());
            admissions.save(admission);
            widened++;
        }
        return widened;
    }

    private void send(Long groomId, AdmissionApplication admission, String message, String keySuffix) {
        String type = NotificationTypes.ADMISSION_NEW_FOR_GROOM;
        String refType = NotificationTypes.REFERENCE_ADMISSION;
        String key = Notification.buildDeduplicationKey(groomId, refType, admission.getId(), type);
        notifications.createNotification(groomId, "New admission to review", message, type, refType,
                admission.getId(), keySuffix == null ? key : key + ":" + keySuffix);
    }

    private String horseName(AdmissionApplication admission) {
        return candidates.findByAdmissionId(admission.getId()).map(CandidateHorseProfile::getName)
                .orElse("Admission #" + admission.getId());
    }

    private static long epoch(LocalDateTime time) {
        return time == null ? 0 : time.toEpochSecond(ZoneOffset.UTC);
    }
}
