package com.rtms.backend.training.dto;

import jakarta.validation.constraints.NotNull;

/** Một chiến mã được thêm vào hoặc chuyển sang nhóm. (A horse added or moved into a group.) */
public record GroupHorseRequest(@NotNull Long horseId) {
}
