package com.rtms.backend.scheduler;

import com.rtms.backend.service.CareScheduleService;
import com.rtms.backend.service.TrainerAssignmentTriggers;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class CareScheduleScheduler {

    private final CareScheduleService careScheduleService;
    private final TrainerAssignmentTriggers trainerAssignmentTriggers;

    public CareScheduleScheduler(CareScheduleService careScheduleService,
                                 TrainerAssignmentTriggers trainerAssignmentTriggers) {
        this.careScheduleService = careScheduleService;
        this.trainerAssignmentTriggers = trainerAssignmentTriggers;
    }

    @Scheduled(fixedRate = 60000)
    public void runCareScheduleMaintenance() {
        careScheduleService.assignRequestedSchedules();
        trainerAssignmentTriggers.retryPendingAssignments();
    }
}
