package com.rtms.backend.event;

import com.rtms.backend.notification.dto.UrgentAssignmentAlert;

public record UrgentAssignmentCommittedEvent(UrgentAssignmentAlert alert) {}
