package com.rtms.backend.identity.dto;

public class LoginResponse {
    private Long userId;
    private String fullName;
    private String email;
    private String role;
    private Object profile;
    private String phone;
    private String address;
    private String avatarUrl;
    private java.time.LocalDateTime createdAt;

    public static LoginResponse fromUser(com.rtms.backend.identity.entity.User user, Object profile) {
        LoginResponse result = new LoginResponse(user.getId(), user.getFullName(), user.getEmail(), user.getRole().getName(), profile);
        result.phone = user.getPhone();
        result.address = user.getAddress();
        result.createdAt = user.getCreatedAt();
        String key = user.getAvatarUrl();
        if (key != null && !key.isBlank()) {
            try {
                byte[] version = java.security.MessageDigest.getInstance("SHA-256").digest(key.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                result.avatarUrl = "/api/auth/me/avatar?v=" + java.util.HexFormat.of().formatHex(version, 0, 12);
            } catch (java.security.NoSuchAlgorithmException impossible) {
                throw new IllegalStateException(impossible);
            }
        }
        return result;
    }

    // Constructor cho login (không cần profile)
    public LoginResponse(Long userId, String fullName, String email, String role) {
        this.userId = userId;
        this.fullName = fullName;
        this.email = email;
        this.role = role;
    }

    // Constructor cho /me (có profile)
    public LoginResponse(Long userId, String fullName, String email, String role, Object profile) {
        this.userId = userId;
        this.fullName = fullName;
        this.email = email;
        this.role = role;
        this.profile = profile;
    }

    private String accessToken;

    public Long getUserId() { return userId; }
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public String getRole() { return role; }
    public Object getProfile() { return profile; }
    public String getPhone() { return phone; }
    public String getAddress() { return address; }
    public String getAvatarUrl() { return avatarUrl; }
    public java.time.LocalDateTime getCreatedAt() { return createdAt; }

    public String getAccessToken() { return accessToken; }
    public void setAccessToken(String accessToken) { this.accessToken = accessToken; }
}
