package com.rtms.backend.identity.controller;

import com.rtms.backend.common.dto.ApiResponse;
import com.rtms.backend.identity.dto.UserSummaryResponse;
import com.rtms.backend.identity.repository.UserRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import com.rtms.backend.identity.entity.User;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

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

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('USER_VIEW')")
    public ApiResponse<UserSummaryResponse> getUserById(@PathVariable Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "User not found with id: " + id));

        return ApiResponse.success(new UserSummaryResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getRole() != null ? user.getRole().getName() : null));
    }
}
