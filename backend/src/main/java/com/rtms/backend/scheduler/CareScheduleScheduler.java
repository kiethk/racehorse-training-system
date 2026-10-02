package com.rtms.backend.scheduler;

import com.rtms.backend.service.CareScheduleService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class CareScheduleScheduler {

    private final CareScheduleService careScheduleService;

    public CareScheduleScheduler(CareScheduleService careScheduleService) {
        this.careScheduleService = careScheduleService;
    }

    @Scheduled(fixedRate = 60000)
    public void runCareScheduleMaintenance() {
        careScheduleService.expirePendingOffers();
        careScheduleService.dispatchRequestedSchedules();
    }
}