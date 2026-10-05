package com.rtms.backend.scheduler;

import com.rtms.backend.service.AdmissionGroomReviewService;
import com.rtms.backend.service.CareScheduleService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class CareScheduleScheduler {

    private final CareScheduleService careScheduleService;
    private final AdmissionGroomReviewService admissionGroomReviewService;

    public CareScheduleScheduler(CareScheduleService careScheduleService,
                                 AdmissionGroomReviewService admissionGroomReviewService) {
        this.careScheduleService = careScheduleService;
        this.admissionGroomReviewService = admissionGroomReviewService;
    }

    @Scheduled(fixedRate = 60000)
    public void runCareScheduleMaintenance() {
        careScheduleService.assignRequestedSchedules();
        admissionGroomReviewService.assignPendingTrainers();
    }
}
