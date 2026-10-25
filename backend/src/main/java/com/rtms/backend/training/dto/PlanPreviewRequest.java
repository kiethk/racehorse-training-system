package com.rtms.backend.training.dto;

import com.rtms.backend.training.enums.TrainingDay;

import java.time.LocalDate;
import java.util.List;

/**
 * Xem trước một kế hoạch trước khi tạo: cùng ba trường đồng bộ như CreateHorseTrainingPlanRequest, cộng các ngựa
 * đang được chọn để tính ngày cả nhóm cùng rảnh.
 * (Previews a plan before creating it: the same three sync fields as CreateHorseTrainingPlanRequest, plus the horses
 * currently selected so the date the whole group is free can be computed.)
 */
public class PlanPreviewRequest {

    private Long courseId;

    private LocalDate startDate;

    private List<TrainingDay> trainingDays;

    /** Ngựa đang được chọn; có thể rỗng. (Horses currently selected; may be empty.) */
    private List<Long> selectedHorseIds;

    public Long getCourseId() { return courseId; }
    public void setCourseId(Long courseId) { this.courseId = courseId; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public List<TrainingDay> getTrainingDays() { return trainingDays; }
    public void setTrainingDays(List<TrainingDay> trainingDays) { this.trainingDays = trainingDays; }

    public List<Long> getSelectedHorseIds() { return selectedHorseIds; }
    public void setSelectedHorseIds(List<Long> selectedHorseIds) { this.selectedHorseIds = selectedHorseIds; }
}
