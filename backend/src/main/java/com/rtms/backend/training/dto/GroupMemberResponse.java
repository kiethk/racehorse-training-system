package com.rtms.backend.training.dto;

import java.time.LocalDateTime;

public record GroupMemberResponse(Long horseId,
                                  String horseName,
                                  Long groomId,
                                  String groomName,
                                  LocalDateTime joinedAt) {
}
