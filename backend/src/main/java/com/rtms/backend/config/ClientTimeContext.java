package com.rtms.backend.config;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;

public final class ClientTimeContext {

    public static final ZoneId DEFAULT_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private static final ThreadLocal<LocalDateTime> CLIENT_TIME = new ThreadLocal<>();

    private ClientTimeContext() {}

    public static void set(LocalDateTime time) {
        CLIENT_TIME.set(time);
    }

    public static void clear() {
        CLIENT_TIME.remove();
    }

    public static LocalDateTime now() {
        LocalDateTime t = CLIENT_TIME.get();
        return t != null ? t : LocalDateTime.now(DEFAULT_ZONE);
    }

    public static LocalDate today() {
        return now().toLocalDate();
    }

    public static LocalTime time() {
        return now().toLocalTime();
    }
}
