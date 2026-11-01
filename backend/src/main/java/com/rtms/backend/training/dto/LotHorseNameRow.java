package com.rtms.backend.training.dto;

/** Một con ngựa còn hiệu lực trong một lot (mã và tên). (An active horse in a lot: id and name.) */
public record LotHorseNameRow(Long lotId, Long horseId, String horseName) {
}
