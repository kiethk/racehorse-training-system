package com.rtms.backend.dto;
import com.rtms.backend.entity.HorseTrainingPlan;

import java.util.List;

public class HorseTrainingPlanDetailResponse {

    private HorseTrainingPlan plan;
    private List<PlanWorkoutItemResponse> workouts;   // ĐỔI KIỂU

    /**
     * Tên chiến mã và tên khoá.
     *
     * Entity HorseTrainingPlan chỉ mang horseId / courseId dạng số vì dự án
     * không dùng quan hệ JPA trên entity nghiệp vụ. Không có hai trường này
     * thì trang chi tiết chỉ hiện "Kế hoạch #27" — Trainer không biết đang
     * xem kế hoạch của con nào, theo khoá gì.
     */
    private String horseName;
    private String courseName;

    /** Giữ chữ ký 2 tham số cho các lời gọi đã có. */
    public HorseTrainingPlanDetailResponse(HorseTrainingPlan plan,
                                           List<PlanWorkoutItemResponse> workouts) {
        this(plan, workouts, null, null);
    }

    public HorseTrainingPlanDetailResponse(HorseTrainingPlan plan,
                                           List<PlanWorkoutItemResponse> workouts,
                                           String horseName,
                                           String courseName) {
        this.plan = plan;
        this.workouts = workouts;
        this.horseName = horseName;
        this.courseName = courseName;
    }

    public HorseTrainingPlan getPlan() { return plan; }
    public void setPlan(HorseTrainingPlan plan) { this.plan = plan; }

    public List<PlanWorkoutItemResponse> getWorkouts() { return workouts; }
    public void setWorkouts(List<PlanWorkoutItemResponse> workouts) { this.workouts = workouts; }

    public String getHorseName() { return horseName; }
    public void setHorseName(String horseName) { this.horseName = horseName; }

    public String getCourseName() { return courseName; }
    public void setCourseName(String courseName) { this.courseName = courseName; }
}