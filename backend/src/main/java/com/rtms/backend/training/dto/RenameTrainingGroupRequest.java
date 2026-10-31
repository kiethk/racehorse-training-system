package com.rtms.backend.training.dto;

import com.rtms.backend.config.TrainingLimits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RenameTrainingGroupRequest(
        @NotBlank @Size(max = TrainingLimits.GROUP_NAME_MAX_LENGTH) String name) {
}
