package com.rtms.backend.training.dto;

/** Một con ngựa còn hiệu lực trong một buổi (mã và tên). (An active horse in a Session: id and name.) */
public record SessionHorseNameRow(Long sessionId, Long horseId, String horseName) {
}
