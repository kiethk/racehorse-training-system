package com.rtms.backend.admission.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record GroomAssignmentRequest(@NotNull @Positive Long groomId) {}
