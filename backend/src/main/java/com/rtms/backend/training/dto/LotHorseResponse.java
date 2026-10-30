package com.rtms.backend.training.dto;

/** Một con ngựa trong lot: mã để mở hồ sơ, tên để hiển thị. (A horse in a lot: id to open its profile, name to show.) */
public record LotHorseResponse(Long horseId, String name) {
}
