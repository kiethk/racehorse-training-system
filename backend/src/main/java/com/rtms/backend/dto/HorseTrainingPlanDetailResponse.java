package com.rtms.backend.dto;

import com.rtms.backend.entity.HorseTrainingPlan;
import com.rtms.backend.entity.TrainingSession;
import java.util.List;

public class HorseTrainingPlanDetailResponse {
    private HorseTrainingPlan plan;
    private List<TrainingSession> sessions;

    public HorseTrainingPlanDetailResponse(HorseTrainingPlan plan, List<TrainingSession> sessions) {
        this.plan = plan;
        this.sessions = sessions;
    }

    public HorseTrainingPlan getPlan() { return plan; }
    public void setPlan(HorseTrainingPlan plan) { this.plan = plan; }

    public List<TrainingSession> getSessions() { return sessions; }
    public void setSessions(List<TrainingSession> sessions) { this.sessions = sessions; }
}