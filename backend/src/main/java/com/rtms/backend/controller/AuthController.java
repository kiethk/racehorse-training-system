package com.rtms.backend.controller;

import com.rtms.backend.dto.ApiResponse;
import com.rtms.backend.dto.LoginRequest;
import com.rtms.backend.dto.LoginResponse;
import com.rtms.backend.entity.User;
import com.rtms.backend.repository.UserRepository;
import com.rtms.backend.security.JwtUtil;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${jwt.expiration}")
    private long jwtExpirationMs;

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
                .maxAge(jwtExpirationMs / 1000) // Convert milliseconds to seconds
                .sameSite("Lax")
                .build();

        response.addHeader("Set-Cookie", cookie.toString());

        LoginResponse loginResponse = new LoginResponse(
                user.getId(), user.getFullName(), user.getEmail(), user.getRole().getName());

        return ApiResponse.success(loginResponse);
    }

    @PostMapping("/logout")
    public ApiResponse<String> logout(HttpServletResponse response) {
        // Tạo một cookie mới đè lên cookie cũ, với maxAge = 0 để trình duyệt xóa nó đi
        ResponseCookie cookie = ResponseCookie.from("jwt_token", "") // Giá trị rỗng
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(0) // Quan trọng nhất: 0 giây sẽ làm cookie hết hạn ngay lập tức
                .sameSite("Lax")
                .build();

        // Gắn cookie vào response
        response.addHeader("Set-Cookie", cookie.toString());

        return ApiResponse.success("Logged out successfully");
    }

}