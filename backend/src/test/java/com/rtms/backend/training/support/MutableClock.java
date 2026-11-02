package com.rtms.backend.training.support;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Đồng hồ test đặt được giờ tùy ý, để kiểm tra các luật theo giờ (buổi đã bắt đầu, đã kết thúc) mà không phải chờ đồng hồ thật.
 * (A test clock that can be set to any time, to check the time-based rules (a Session has started, has ended) without waiting for
 * the real clock.)
 */
public class MutableClock extends Clock {

    private final ZoneId zone = ZoneId.systemDefault();
    private volatile Instant instant = Instant.now();

    /** Quay về giờ thật, dùng khi test kết thúc. (Back to real time, used when a test ends.) */
    public void reset() {
        instant = Instant.now();
    }

    public void set(LocalDateTime moment) {
        instant = moment.atZone(zone).toInstant();
    }

    @Override
    public ZoneId getZone() {
        return zone;
    }

    @Override
    public Clock withZone(ZoneId newZone) {
        return Clock.fixed(instant, newZone);
    }

    @Override
    public Instant instant() {
        return instant;
    }
}
