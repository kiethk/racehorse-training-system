package com.rtms.backend.training.dto;

/**
 * Một chiến mã trong khu của Trainer và việc nó có vào được nhóm không; reason nêu vì sao không vào được.
 * (A horse in the Trainer's areas and whether it can join the group; reason says why it cannot.)
 */
public record GroupCandidateResponse(Long horseId,
                                     String horseName,
                                     String groomName,
                                     boolean joinable,
                                     String reason) {
}
