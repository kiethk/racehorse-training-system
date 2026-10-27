package com.rtms.backend.scheduler;

import com.rtms.backend.admission.service.AdmissionGroomPoolNotifier;
import com.rtms.backend.admission.service.AdmissionGroomReviewService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class AdmissionArrivalScheduler {

    private final AdmissionGroomReviewService admissionGroomReviewService;
    private final AdmissionGroomPoolNotifier groomPoolNotifier;

    public AdmissionArrivalScheduler(AdmissionGroomReviewService admissionGroomReviewService,
            AdmissionGroomPoolNotifier groomPoolNotifier) {
        this.admissionGroomReviewService = admissionGroomReviewService;
        this.groomPoolNotifier = groomPoolNotifier;
    }

    @Scheduled(fixedRate = 60000)
    public void expireOverdueArrivalReservations() {
        admissionGroomReviewService.expireOverdueArrivalReservations();
    }

    /** Tells every groom with a free quarantine stall about admissions that waited past the fallback window. */
    @Scheduled(fixedRate = 300000, initialDelay = 30000)
    public void widenGroomPool() {
        groomPoolNotifier.notifyWidenedPools();
    }
}
