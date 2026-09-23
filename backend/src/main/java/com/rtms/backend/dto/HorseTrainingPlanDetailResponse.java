package com.rtms.backend.dto;

import com.rtms.backend.entity.HorseTrainingPlan;

import java.util.List;

public class HorseTrainingPlanDetailResponse {

    private HorseTrainingPlan plan;
    private List<PlanWorkoutItemResponse> workouts;   // ĐỔI KIỂU

    public HorseTrainingPlanDetailResponse(HorseTrainingPlan plan,
                                           List<PlanWorkoutItemResponse> workouts) {
        this.plan = plan;
        this.workouts = workouts;
    }

    public HorseTrainingPlan getPlan() { return plan; }
    public void setPlan(HorseTrainingPlan plan) { this.plan = plan; }

    public List<PlanWorkoutItemResponse> getWorkouts() { return workouts; }
    public void setWorkouts(List<PlanWorkoutItemResponse> workouts) { this.workouts = workouts; }
}