package com.rtms.backend.config;

import com.rtms.backend.common.dto.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

/**
 * Endpoint cung cấp thời gian thực tế của hệ điều hành (không bị cache bởi trình duyệt Chrome V8),
 * giúp đồng bộ tức thì sau khi người dùng đổi giờ trên Windows.
 */
@RestController
@RequestMapping("/api/system")
public class SystemTimeController {

    @GetMapping("/time")
    public ApiResponse<Map<String, Object>> getSystemTime() {
        Instant now = Instant.now();
        return ApiResponse.success(Map.of(
                "epochMillis", now.toEpochMilli(),
                "isoUtc", now.toString()
        ));
    }
}
