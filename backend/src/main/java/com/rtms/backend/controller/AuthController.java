package com.rtms.backend.controller;

import com.rtms.backend.dto.ApiResponse;
import com.rtms.backend.dto.LoginRequest;
import com.rtms.backend.dto.LoginResponse;
import com.rtms.backend.entity.User;
import com.rtms.backend.repository.UserRepository;
import com.rtms.backend.security.JwtUtil;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseCookie;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@RequestBody LoginRequest request, HttpServletResponse response) {

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Email or Password is incorrect"));

        boolean passwordMatches = passwordEncoder.matches(request.getPassword(), user.getPasswordHash());
        if (!passwordMatches) {
            throw new RuntimeException("Email or Password is incorrect");
        }

        String token = jwtUtil.generateToken(user.getId(), user.getEmail(), user.getRole().getName());

        // Send token via HttpOnly Cookie
        ResponseCookie cookie = ResponseCookie.from("jwt_token", token)
                .httpOnly(true)
                .secure(false) // false because localhost (http, no https)
                .path("/")
                .maxAge(24 * 60 * 60) // 24 hours, in seconds
                .sameSite("Lax")
                .build();

        response.addHeader("Set-Cookie", cookie.toString());

        LoginResponse loginResponse = new LoginResponse(
                user.getId(), user.getFullName(), user.getEmail(), user.getRole().getName());

        return ApiResponse.success(loginResponse);
    }
}