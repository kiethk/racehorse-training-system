package com.rtms.backend.event;

import com.rtms.backend.dto.UrgentAssignmentAlert;

public record UrgentAssignmentCommittedEvent(UrgentAssignmentAlert alert) {}
