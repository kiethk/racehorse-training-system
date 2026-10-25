package com.rtms.backend.identity.controller;

import com.rtms.backend.common.dto.ApiResponse;
import com.rtms.backend.identity.dto.*;
import com.rtms.backend.identity.service.AccountManagementService;
import com.rtms.backend.security.AuthenticatedUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/accounts")
public class AccountManagementController {

    private final AccountManagementService accountManagementService;

    public AccountManagementController(AccountManagementService accountManagementService) {
        this.accountManagementService = accountManagementService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('USER_MANAGE')")
    public ApiResponse<List<AccountSummaryResponse>> getAllAccounts() {
        return ApiResponse.success(accountManagementService.getAllAccounts());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('USER_MANAGE')")
    public ApiResponse<AccountDetailResponse> getAccountDetail(@PathVariable Long id) {
        return ApiResponse.success(accountManagementService.getAccountDetail(id));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAuthority('USER_MANAGE')")
    public ApiResponse<AccountDetailResponse> updateAccount(
            @PathVariable Long id,
            @RequestBody StaffUpdateRequest request) {
        return ApiResponse.success(accountManagementService.updateAccount(id, request));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('USER_MANAGE')")
    public ApiResponse<AccountCreationResponse> createAccount(@Valid @RequestBody AccountCreationRequest request) {
        return ApiResponse.success(accountManagementService.createAccount(request));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('USER_MANAGE')")
    public ApiResponse<AccountSummaryResponse> updateAccountStatus(
            @PathVariable Long id,
            @RequestBody StaffStatusUpdateRequest request) {
        Long currentUserId = getCurrentUserId();
        return ApiResponse.success(accountManagementService.updateAccountStatus(currentUserId, id, request.isActive()));
    }

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof AuthenticatedUser user) {
            return user.getUserId();
        }
        return null;
    }
}
