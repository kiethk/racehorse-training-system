package com.rtms.backend.scheduler;

import com.rtms.backend.medical.service.CareScheduleService;
import com.rtms.backend.admission.service.TrainerAssignmentTriggers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class CareScheduleScheduler {

    private static final Logger log = LoggerFactory.getLogger(CareScheduleScheduler.class);

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
        // D1 (WF-01/WF-03): auto-activate, EOD rollover, URGENT escalation. Bọc riêng để
        // sweep lỗi không chặn vòng assign của tick sau.
        try {
            careScheduleService.careScheduleLifecycleSweep();
        } catch (RuntimeException ex) {
            log.warn("careScheduleLifecycleSweep failed: {}", ex.getMessage());
        }
    }
}
