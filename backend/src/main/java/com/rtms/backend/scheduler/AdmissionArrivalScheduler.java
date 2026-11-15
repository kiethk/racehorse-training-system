package com.rtms.backend.scheduler;

import com.rtms.backend.admission.service.AdmissionGroomReviewService;
import com.rtms.backend.admission.service.GroomAssignmentTriggers;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class AdmissionArrivalScheduler {

    private final AdmissionGroomReviewService admissionGroomReviewService;

    private final GroomAssignmentTriggers groomAssignmentTriggers;

    public AdmissionArrivalScheduler(AdmissionGroomReviewService admissionGroomReviewService,
                                     GroomAssignmentTriggers groomAssignmentTriggers) {
        this.groomAssignmentTriggers = groomAssignmentTriggers;
        this.admissionGroomReviewService = admissionGroomReviewService;
    }

    @Scheduled(fixedRate = 60000, initialDelay = 20000)
    public void assignGroomReviews() {
        groomAssignmentTriggers.runPeriodicJobs();
    }

    @Scheduled(fixedRate = 60000)
    public void expireOverdueArrivalReservations() {
        admissionGroomReviewService.expireOverdueArrivalReservations();
    }

}
