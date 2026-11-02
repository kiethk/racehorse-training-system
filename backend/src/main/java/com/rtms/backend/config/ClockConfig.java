package com.rtms.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Đồng hồ dùng chung cho mọi luật theo giờ của huấn luyện (buổi đã bắt đầu, đã kết thúc, hôm nay còn khe không). Có bean riêng để
 * test đặt được giờ cố định thay vì chờ đồng hồ thật.
 * (The shared clock behind every time-based training rule (a Session has started, has ended, is there room today). A bean of its
 * own lets tests fix the time instead of waiting for the real clock.)
 */
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
