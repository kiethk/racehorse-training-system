package com.rtms.backend.controller;

import com.rtms.backend.dto.ApiResponse;
import com.rtms.backend.dto.LoginRequest;
import com.rtms.backend.dto.LoginResponse;
import com.rtms.backend.dto.OwnerRegistrationRequest;
import com.rtms.backend.dto.OwnerRegistrationResponse;
import com.rtms.backend.security.AuthenticatedUser;
import com.rtms.backend.service.AuthService;
import com.rtms.backend.service.AuthService.LoginResult;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    @Value("${jwt.access-expiration}")
    private long accessExpirationMs;

    @Value("${jwt.refresh-expiration}")
    private long refreshExpirationMs;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@RequestBody LoginRequest request) {
        LoginResult result = authService.login(request);
        return ApiResponse.success(result.loginResponse());
    }

    @PostMapping("/refresh")
    public ApiResponse<com.rtms.backend.dto.TokenRefreshResponse> refresh(@RequestBody com.rtms.backend.dto.RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();
        if (refreshToken == null || refreshToken.isBlank()) {
            return ApiResponse.error("Refresh token missing", "UNAUTHORIZED");
        }

        String newAccessToken = authService.refresh(refreshToken);

        return ApiResponse.success(new com.rtms.backend.dto.TokenRefreshResponse(newAccessToken));
    }

    @PostMapping("/logout")
    public ApiResponse<String> logout(@RequestBody(required = false) com.rtms.backend.dto.RefreshTokenRequest request) {
        if (request != null && request.getRefreshToken() != null) {
            authService.logout(request.getRefreshToken());
        }

        return ApiResponse.success("Logged out successfully");
    }

    @GetMapping("/me")
    public ApiResponse<LoginResponse> getMe() {
        AuthenticatedUser currentUser = (AuthenticatedUser) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();

        return ApiResponse.success(authService.getMe(currentUser.getUserId()));
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<OwnerRegistrationResponse>> register(
            @RequestBody OwnerRegistrationRequest request) {
        OwnerRegistrationResponse response = authService.registerOwner(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response));
    }
}
