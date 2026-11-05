package com.rtms.backend.identity.dto;

/**
 * Result returned after creating an account (Staff, Owner, or Club Manager).
 * Extends StaffCreationResponse to preserve full backward compatibility with staff creation results.
 */
public class AccountCreationResponse extends StaffCreationResponse {
    // Inherits all fields from StaffCreationResponse:
    // userId, fullName, email, phone, address, role, active, createdAt, profileSummary,
    // assignmentStatus, assignedAreaType, assignedAreaIds, assignedAreaCodes,
    // trainerId, trainerName, assignedAreaId, assignedAreaCode, assignedStallIds, assignedStallCodes,
    // noStallBlockAvailable
}
