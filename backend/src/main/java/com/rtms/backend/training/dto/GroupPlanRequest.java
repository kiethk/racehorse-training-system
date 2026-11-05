package com.rtms.backend.training.dto;

import com.rtms.backend.training.enums.TrainingDay;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

/** Giao khóa cho nhóm: dùng chung cho xem trước và tạo. (Assigning a course to a group: shared by preview and create.) */
public record GroupPlanRequest(@NotNull Long groupId,
                               @NotNull Long courseId,
                               @NotNull LocalDate startDate,
                               @NotEmpty List<TrainingDay> trainingDays) {
}
