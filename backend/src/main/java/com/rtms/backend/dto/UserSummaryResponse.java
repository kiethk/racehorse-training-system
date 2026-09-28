package com.rtms.backend.dto;

/**
 * Thông tin tối thiểu để hiển thị một người trong danh sách chọn.
 *
 * CỐ Ý không trả password_hash, không trả is_active, không trả ngày tạo.
 * Đây là DTO cho ô chọn, không phải hồ sơ nhân sự.
 */
public class UserSummaryResponse {

    private Long id;
    private String fullName;
    private String email;
    private String role;

    public UserSummaryResponse(Long id, String fullName, String email, String role) {
        this.id = id;
        this.fullName = fullName;
        this.email = email;
        this.role = role;
    }

    public Long getId() { return id; }
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public String getRole() { return role; }
}
