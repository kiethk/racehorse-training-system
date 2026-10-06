package com.rtms.backend.controller;

import com.rtms.backend.dto.ApiResponse;
import com.rtms.backend.dto.LoginRequest;
import com.rtms.backend.dto.LoginResponse;
import com.rtms.backend.dto.OwnerRegistrationRequest;
import com.rtms.backend.dto.OwnerRegistrationResponse;
import com.rtms.backend.security.AuthenticatedUser;
import com.rtms.backend.service.AuthService;
import com.rtms.backend.service.AuthService.LoginResult;
import io.swagger.v3.oas.annotations.Operation;
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

    @Value("${jwt.refresh-expiration}")
    private long refreshExpirationMs;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(security = {})
    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@RequestBody LoginRequest request, HttpServletResponse response) {
        LoginResult result = authService.login(request);
        
        setRefreshCookie(response, result.refreshToken(), refreshExpirationMs / 1000);
        
        return ApiResponse.success(result.loginResponse());
    }

    public record TokenRefreshResponse(String accessToken) {}

    @Operation(security = {})
    @PostMapping("/refresh")
    public ApiResponse<TokenRefreshResponse> refresh(
            @CookieValue(name = "rtms_refresh_token", required = false) String refreshToken,
            HttpServletResponse response) {
        
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new com.rtms.backend.config.ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Refresh token missing");
        }

        AuthService.RefreshResult result = authService.refresh(refreshToken);
        
        setRefreshCookie(response, result.refreshToken(), refreshExpirationMs / 1000);

        return ApiResponse.success(new TokenRefreshResponse(result.accessToken()));
    }

    @Operation(security = {})
    @PostMapping("/logout")
    public ApiResponse<String> logout(
            @CookieValue(name = "rtms_refresh_token", required = false) String refreshToken,
            HttpServletResponse response) {
        
        if (refreshToken != null && !refreshToken.isBlank()) {
            authService.logout(refreshToken);
        }

        setRefreshCookie(response, "deleted", 0); // Expire the cookie

        return ApiResponse.success("Logged out successfully");
    }

    @GetMapping("/me")
    public ApiResponse<LoginResponse> getMe() {
        AuthenticatedUser currentUser = (AuthenticatedUser) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();

        return ApiResponse.success(authService.getMe(currentUser.getUserId()));
    }

    @Operation(security = {})
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<OwnerRegistrationResponse>> register(
            @RequestBody OwnerRegistrationRequest request) {
        OwnerRegistrationResponse response = authService.registerOwner(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response));
    }

    private void setRefreshCookie(HttpServletResponse response, String refreshToken, long maxAgeSeconds) {
        ResponseCookie refreshCookie = ResponseCookie.from("rtms_refresh_token", refreshToken)
                .httpOnly(true)
                .secure(false) // false because localhost
                .path("/api/auth")
                .maxAge(maxAgeSeconds)
                .sameSite("Lax")
                .build();

        response.addHeader("Set-Cookie", refreshCookie.toString());
    }
}

