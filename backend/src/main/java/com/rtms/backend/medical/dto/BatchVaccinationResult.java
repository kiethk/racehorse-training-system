package com.rtms.backend.medical.dto;

/** Mỗi ngựa trong lô nhận một health record VACCINATION riêng. */
public record BatchVaccinationResult(Long horseId, Long healthRecordId) {
}
