package com.rtms.backend.identity.controller;

import com.rtms.backend.common.dto.ApiResponse;
import com.rtms.backend.identity.dto.LoginResponse;
import com.rtms.backend.identity.dto.UpdateMyProfileRequest;
import com.rtms.backend.identity.service.MyProfileService;
import com.rtms.backend.security.AuthenticatedUser;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/auth/me")
@PreAuthorize("isAuthenticated()")
public class MyProfileController {
    private final MyProfileService profiles;
    public MyProfileController(MyProfileService profiles) { this.profiles = profiles; }

    @PostMapping(value = "/profile", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<LoginResponse> update(@AuthenticationPrincipal AuthenticatedUser actor,
            @Valid @RequestPart("profile") UpdateMyProfileRequest request,
            @RequestPart(value = "avatar", required = false) MultipartFile avatar) {
        return ApiResponse.success(profiles.update(actor.getUserId(), request, avatar));
    }

    @GetMapping("/avatar")
    public ResponseEntity<Resource> avatar(@AuthenticationPrincipal AuthenticatedUser actor) {
        var photo = profiles.avatar(actor.getUserId());
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).contentType(photo.mediaType())
                .header("X-Content-Type-Options", "nosniff").body(photo.resource());
    }
}
