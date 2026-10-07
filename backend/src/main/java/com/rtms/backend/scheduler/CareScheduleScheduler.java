package com.rtms.backend.scheduler;
import com.rtms.backend.service.CareScheduleService;
import com.rtms.backend.service.TrainerScheduleAssignmentService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class CareScheduleScheduler {

    private final CareScheduleService careScheduleService;
    private final TrainerScheduleAssignmentService trainerScheduleAssignmentService;

    public CareScheduleScheduler(CareScheduleService careScheduleService,
                                 TrainerScheduleAssignmentService trainerScheduleAssignmentService) {
        this.careScheduleService = careScheduleService;
        this.trainerScheduleAssignmentService = trainerScheduleAssignmentService;
    }

    @Scheduled(fixedRate = 60000)
    public void runCareScheduleMaintenance() {
        careScheduleService.assignRequestedSchedules();
        trainerScheduleAssignmentService.retryPendingAssignments();
    }
}
