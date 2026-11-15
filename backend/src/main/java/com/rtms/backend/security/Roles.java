package com.rtms.backend.security;

/**
 * Tên các vai trò như được lưu trong bảng roles và trong token. Dùng hằng số thay cho chuỗi gõ tay để một lỗi chính tả không âm thầm
 * làm một vai trò mất quyền.
 * (Role names as stored in the roles table and in the token. Constants replace hand-typed strings so a typo cannot silently
 * take a role's access away.)
 */
public final class Roles {

    public static final String HEAD_TRAINER = "HEAD_TRAINER";
    public static final String CLUB_MANAGER = "CLUB_MANAGER";
    public static final String GROOM = "GROOM";
    public static final String HORSE_OWNER = "HORSE_OWNER";
    public static final String VETERINARIAN = "VETERINARIAN";

    private Roles() {
        // lớp tiện ích, không cho khởi tạo (utility class)
    }
}
