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
    public ApiResponse<LoginResponse> login(@RequestBody LoginRequest request, HttpServletResponse response) {
        LoginResult result = authService.login(request);

        setCookies(response, result.token(), result.refreshToken());

        return ApiResponse.success(result.loginResponse());
    }

    @PostMapping("/refresh")
    public ApiResponse<String> refresh(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = extractCookie(request, "rtms_refresh_token");
        if (refreshToken == null || refreshToken.isBlank()) {
            return ApiResponse.error("Refresh token missing", "UNAUTHORIZED");
        }

        String newAccessToken = authService.refresh(refreshToken);

        ResponseCookie accessCookie = ResponseCookie.from("rtms_access_token", newAccessToken)
                .httpOnly(true)
                .secure(false) // false because localhost (http, no https)
                .path("/")
                .maxAge(accessExpirationMs / 1000)
                .sameSite("Lax")
                .build();

        response.addHeader("Set-Cookie", accessCookie.toString());

        return ApiResponse.success("Token refreshed");
    }

    @PostMapping("/logout")
    public ApiResponse<String> logout(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = extractCookie(request, "rtms_refresh_token");
        
        authService.logout(refreshToken);

        ResponseCookie accessCookie = ResponseCookie.from("rtms_access_token", "")
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(0)
                .sameSite("Lax")
                .build();

        ResponseCookie refreshCookie = ResponseCookie.from("rtms_refresh_token", "")
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(0)
                .sameSite("Lax")
                .build();

        response.addHeader("Set-Cookie", accessCookie.toString());
        response.addHeader("Set-Cookie", refreshCookie.toString());

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

    private void setCookies(HttpServletResponse response, String accessToken, String refreshToken) {
        ResponseCookie accessCookie = ResponseCookie.from("rtms_access_token", accessToken)
                .httpOnly(true)
                .secure(false) 
                .path("/")
                .maxAge(accessExpirationMs / 1000)
                .sameSite("Lax")
                .build();

        ResponseCookie refreshCookie = ResponseCookie.from("rtms_refresh_token", refreshToken)
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(refreshExpirationMs / 1000)
                .sameSite("Lax")
                .build();

        response.addHeader("Set-Cookie", accessCookie.toString());
        response.addHeader("Set-Cookie", refreshCookie.toString());
    }

    private String extractCookie(HttpServletRequest request, String name) {
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if (name.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
}
