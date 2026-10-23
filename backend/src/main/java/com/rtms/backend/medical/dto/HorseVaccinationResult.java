package com.rtms.backend.medical.dto;

/** Lần ghi nhận tiêm cho một ngựa tạo một health record VACCINATION. */
public record HorseVaccinationResult(Long horseId, Long healthRecordId) {
}
