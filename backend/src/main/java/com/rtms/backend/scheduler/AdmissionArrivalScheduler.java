package com.rtms.backend.scheduler;

import com.rtms.backend.service.AdmissionGroomReviewService;
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
}
