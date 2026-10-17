package com.rtms.backend.identity.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Self-service fields only. Email, role, password, status and professional credentials are not writable. */
public record UpdateMyProfileRequest(
        @NotBlank(message = "Full name is required") @Size(max = 255) String fullName,
        @Size(max = 20, message = "Phone number must be at most 20 characters") String phone,
        @Size(max = 255) String address,
        boolean removeAvatar) {
    @JsonAnySetter
    public void rejectUnexpectedField(String field, Object value) {
        throw new IllegalArgumentException("Unsupported profile field: " + field);
    }
}
