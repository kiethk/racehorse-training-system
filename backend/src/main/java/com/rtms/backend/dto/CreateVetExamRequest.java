package com.rtms.backend.dto;

import com.rtms.backend.enums.VetExamType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record CreateVetExamRequest(
        @NotNull Long horseId,
        @NotNull VetExamType examType,
        @NotBlank String reason,
        LocalDate requestedForDate) { }
