package com.rtms.backend.event;

import java.util.List;

public record ClinicalRecordCreatedEvent(
        Long healthRecordId,
        Long horseId,
        String registrationNumber,
        String symptoms,
        String diagnosis,
        List<String> allowedRoles
) {}