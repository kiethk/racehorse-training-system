package com.rtms.backend.scheduler;

import com.rtms.backend.admission.service.AdmissionGroomReviewService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class AdmissionArrivalScheduler {

    private final AdmissionGroomReviewService admissionGroomReviewService;

    public AdmissionArrivalScheduler(AdmissionGroomReviewService admissionGroomReviewService) {
        this.admissionGroomReviewService = admissionGroomReviewService;
    }

    @Scheduled(fixedRate = 60000)
    public void expireOverdueArrivalReservations() {
        admissionGroomReviewService.expireOverdueArrivalReservations();
    }

    /** Safety net: every capacity change already triggers allocation, this catches anything skipped (e.g. SKIP LOCKED). */
    @Scheduled(fixedRate = 60000, initialDelay = 30000)
    public void allocateWaitingAdmissions() {
        admissionGroomReviewService.allocateWaitingAdmissions();
    }
}
