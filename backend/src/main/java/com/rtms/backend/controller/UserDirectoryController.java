package com.rtms.backend.controller;

import com.rtms.backend.dto.ApiResponse;
import com.rtms.backend.dto.UserSummaryResponse;
import com.rtms.backend.repository.UserRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Danh bạ nhân sự — CHỈ ĐỌC.
 *
 * Đặt tên "Directory" thay vì "UserController" để nói rõ phạm vi: đây không
 * phải module quản lý tài khoản (thuộc Club Manager, ngoài phạm vi đồ án),
 * mà chỉ là nguồn dữ liệu cho các ô chọn người phụ trách.
 */
@RestController
@RequestMapping("/api/users")
public class UserDirectoryController {

    private final UserRepository userRepository;

    public UserDirectoryController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * @param role lọc theo tên vai trò, ví dụ ?role=GROOM.
     *             Không truyền thì trả toàn bộ người đang hoạt động.
     */
    @GetMapping
    @PreAuthorize("hasAuthority('USER_VIEW')")
    public ApiResponse<List<UserSummaryResponse>> getUsers(
            @RequestParam(required = false) String role) {

        List<UserSummaryResponse> result = userRepository.findAll().stream()
                .filter(u -> u.isActive())
                .filter(u -> u.getRole() != null)
                .filter(u -> role == null || role.equalsIgnoreCase(u.getRole().getName()))
                .map(u -> new UserSummaryResponse(
                        u.getId(),
                        u.getFullName(),
                        u.getEmail(),
                        u.getRole().getName()))
                .toList();

        return ApiResponse.success(result);
    }
}
