package com.rtms.backend.ai.hitl.controller;

import com.rtms.backend.ai.dto.ActionCommitRequestDto;
import com.rtms.backend.ai.dto.ActionCommitResponseDto;
import com.rtms.backend.ai.hitl.service.ActionCommitService;
import com.rtms.backend.security.AuthenticatedUser;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller tiếp nhận và xử lý phê duyệt hành động HITL (Phase 2 Commit).
 */
@RestController
@RequestMapping("/api/v1/ai")
public class AiActionController {

    private final ActionCommitService actionCommitService;

    public AiActionController(ActionCommitService actionCommitService) {
        this.actionCommitService = actionCommitService;
    }

    /**
     * Xác nhận và áp dụng một hành động đề xuất của AI vào CSDL.
     * Cấm triệt để HORSE_OWNER (Read-Only 100%).
     */
    @PostMapping("/actions/commit")
    @PreAuthorize("isAuthenticated() and principal.role != 'HORSE_OWNER'")
    public ResponseEntity<ActionCommitResponseDto> commitAction(
            @Valid @RequestBody ActionCommitRequestDto request,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        ActionCommitResponseDto response = actionCommitService.commitAction(request, currentUser);
        return ResponseEntity.ok(response);
    }
}
