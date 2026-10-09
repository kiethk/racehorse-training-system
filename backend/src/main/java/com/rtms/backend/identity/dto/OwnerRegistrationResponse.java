package com.rtms.backend.identity.dto;

public class OwnerRegistrationResponse {
    private Long userId;
    private String fullName;
    private String email;
    private String role;

    public OwnerRegistrationResponse(Long userId, String fullName, String email, String role) {
        this.userId = userId;
        this.fullName = fullName;
        this.email = email;
        this.role = role;
    }

    public Long getUserId() {
        return userId;
    }

    public String getFullName() {
        return fullName;
    }

    public String getEmail() {
        return email;
    }

    public String getRole() {
        return role;
    }
}
