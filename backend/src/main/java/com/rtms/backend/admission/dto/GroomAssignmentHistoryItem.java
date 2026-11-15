package com.rtms.backend.admission.dto;

import com.rtms.backend.admission.enums.AssignmentEndReason;
import java.time.LocalDateTime;

public record GroomAssignmentHistoryItem(Long id, Long groomId, String groomName,
        LocalDateTime assignedAt, LocalDateTime endedAt, AssignmentEndReason endReason,
        String assignedByName) {}
