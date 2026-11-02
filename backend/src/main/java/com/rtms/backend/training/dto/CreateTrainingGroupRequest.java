package com.rtms.backend.training.dto;

import com.rtms.backend.config.TrainingLimits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

/** Tạo nhóm: tên và các chiến mã ban đầu (có thể rỗng). (Creating a group: its name and the initial horses (may be empty).) */
public record CreateTrainingGroupRequest(
        @NotBlank @Size(max = TrainingLimits.GROUP_NAME_MAX_LENGTH) String name,
        @Size(max = TrainingLimits.MAX_HORSES_PER_GROUP) List<Long> horseIds) {
}
