package com.rtms.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

/**
 * Đồng hồ dùng chung cho toàn bộ hệ thống.
 * Tự động đồng bộ theo giờ từ Client (qua header X-Client-Time) nếu có,
 * hoặc fallback về giờ hệ thống (Asia/Ho_Chi_Minh).
 */
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock() {
        return new DynamicClientClock(ClientTimeContext.DEFAULT_ZONE);
    }

    public static class DynamicClientClock extends Clock {
        private final ZoneId zone;

        public DynamicClientClock(ZoneId zone) {
            this.zone = zone != null ? zone : ClientTimeContext.DEFAULT_ZONE;
        }

        @Override
        public ZoneId getZone() {
            return zone;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return new DynamicClientClock(zone);
        }

        @Override
        public Instant instant() {
            // Client time is captured as a wall-clock value in the farm's zone. Always
            // convert from that zone so withZone() changes the display zone, not the instant.
            return ClientTimeContext.now().atZone(ClientTimeContext.DEFAULT_ZONE).toInstant();
        }
    }
}
