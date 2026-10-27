package com.rtms.backend.admission.service;

import com.rtms.backend.admission.entity.AdmissionApplication;
import com.rtms.backend.admission.entity.CandidateHorseProfile;
import com.rtms.backend.admission.enums.AdmissionStatus;
import com.rtms.backend.admission.repository.AdmissionApplicationRepository;
import com.rtms.backend.admission.repository.CandidateHorseProfileRepository;
import com.rtms.backend.notification.service.NotificationService;
import com.rtms.backend.notification.service.NotificationTypes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AdmissionGroomPoolNotifierTest {
    private static final LocalDateTime STARTED = LocalDateTime.of(2026, 10, 25, 9, 0);
    private static final LocalDateTime CUTOFF = LocalDateTime.of(2026, 10, 26, 10, 0);

    private AdmissionGroomPoolService pool;
    private AdmissionApplicationRepository admissions;
    private CandidateHorseProfileRepository candidates;
    private NotificationService notifications;
    private AdmissionGroomPoolNotifier notifier;

    @BeforeEach
    void setUp() {
        pool = mock(AdmissionGroomPoolService.class);
        admissions = mock(AdmissionApplicationRepository.class);
        candidates = mock(CandidateHorseProfileRepository.class);
        notifications = mock(NotificationService.class);
        notifier = new AdmissionGroomPoolNotifier(pool, admissions, candidates, notifications);
        CandidateHorseProfile profile = new CandidateHorseProfile();
        profile.setName("QABALAH MERCURY");
        when(candidates.findByAdmissionId(1L)).thenReturn(Optional.of(profile));
        when(pool.fallbackCutoff()).thenReturn(CUTOFF);
    }

    private static AdmissionApplication waiting() {
        AdmissionApplication admission = new AdmissionApplication();
        admission.setId(1L);
        admission.setStatus(AdmissionStatus.GROOM_REVIEW);
        admission.setGroomReviewStartedAt(STARTED);
        return admission;
    }

    private static String key(long groomId) {
        return groomId + ":ADMISSION_1:ADMISSION_NEW_FOR_GROOM";
    }

    @Test
    @DisplayName("A reopened admission notifies the least-loaded grooms under a fresh key")
    void reopenNotifiesLeastLoadedWithFreshKey() {
        when(pool.leastLoadedGroomIds()).thenReturn(List.of(31L));

        notifier.notifyReopened(waiting());

        verify(notifications).createNotification(eq(31L), anyString(), contains("QABALAH MERCURY"),
                eq(NotificationTypes.ADMISSION_NEW_FOR_GROOM), eq(NotificationTypes.REFERENCE_ADMISSION), eq(1L),
                eq(key(31) + ":reopen:" + STARTED.toEpochSecond(ZoneOffset.UTC)));
    }

    @Test
    @DisplayName("D8.5: widening tells only grooms who could not see the admission before, and marks it")
    void wideningNotifiesNewlyShownGroomsOnce() {
        AdmissionApplication admission = waiting();
        when(admissions.findByStatusAndGroomPoolWidenedAtIsNullAndGroomReviewStartedAtLessThanEqualOrderByGroomReviewStartedAtAscIdAsc(
                AdmissionStatus.GROOM_REVIEW, CUTOFF)).thenReturn(List.of(admission));
        when(admissions.findByIdForUpdate(1L)).thenReturn(Optional.of(admission));
        when(pool.leastLoadedGroomIds()).thenReturn(List.of(31L));
        when(pool.eligibleGroomIds()).thenReturn(List.of(31L, 32L, 33L));

        assertEquals(1, notifier.notifyWidenedPools());

        String suffix = ":widened:" + STARTED.toEpochSecond(ZoneOffset.UTC);
        verify(notifications).createNotification(eq(32L), anyString(), anyString(), anyString(), anyString(), eq(1L), eq(key(32) + suffix));
        verify(notifications).createNotification(eq(33L), anyString(), anyString(), anyString(), anyString(), eq(1L), eq(key(33) + suffix));
        verifyNoMoreInteractions(notifications);
        assertNotNull(admission.getGroomPoolWidenedAt());
        verify(admissions).save(admission);
    }

    @Test
    @DisplayName("An admission that left the pool or was already widened is skipped")
    void wideningSkipsHandledAdmissions() {
        AdmissionApplication claimed = waiting();
        claimed.setStatus(AdmissionStatus.WAITING_FOR_ARRIVAL);
        when(admissions.findByStatusAndGroomPoolWidenedAtIsNullAndGroomReviewStartedAtLessThanEqualOrderByGroomReviewStartedAtAscIdAsc(
                AdmissionStatus.GROOM_REVIEW, CUTOFF)).thenReturn(List.of(waiting()));
        when(admissions.findByIdForUpdate(1L)).thenReturn(Optional.of(claimed));
        when(pool.eligibleGroomIds()).thenReturn(List.of(31L, 32L));

        assertEquals(0, notifier.notifyWidenedPools());

        verifyNoInteractions(notifications);
        verify(admissions, never()).save(any());
    }

    @Test
    @DisplayName("Nothing due: the pool is not even read")
    void nothingDue() {
        when(admissions.findByStatusAndGroomPoolWidenedAtIsNullAndGroomReviewStartedAtLessThanEqualOrderByGroomReviewStartedAtAscIdAsc(
                AdmissionStatus.GROOM_REVIEW, CUTOFF)).thenReturn(List.of());

        assertEquals(0, notifier.notifyWidenedPools());

        verify(pool, never()).eligibleGroomIds();
        verifyNoInteractions(notifications);
    }
}
