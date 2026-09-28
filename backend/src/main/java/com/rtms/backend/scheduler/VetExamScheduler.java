package com.rtms.backend.scheduler;

import com.rtms.backend.service.VetExamService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class VetExamScheduler {
    private final VetExamService vetExamService;

    public VetExamScheduler(VetExamService vetExamService) {
        this.vetExamService = vetExamService;
    }

    @Scheduled(fixedDelayString = "${rtms.vet-exam.scheduler-delay-ms:60000}")
    public void assignRequestedExams() {
        vetExamService.scheduleRequestedBatch(100);
    }
}
