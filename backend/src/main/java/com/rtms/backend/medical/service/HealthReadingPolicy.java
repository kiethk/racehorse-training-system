package com.rtms.backend.medical.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Khi nào một lần đo sức khỏe bị coi là bất thường, để dashboard chỉ ra con nào cần chú ý. Các ngưỡng là khoảng bình
 * thường thường dùng cho ngựa trưởng thành; Thú y là người quyết định cuối cùng, nên chỉnh ở đây khi cần.
 * (When a health reading counts as abnormal, so the dashboard can point at the horses that need attention. The limits
 * are the ranges commonly used for an adult horse; the veterinarian has the final word, so change them here when
 * needed.)
 */
public final class HealthReadingPolicy {

    /** Thân nhiệt bình thường, °C. (Normal temperature, °C.) */
    public static final BigDecimal MIN_TEMPERATURE = new BigDecimal("37.2");
    public static final BigDecimal MAX_TEMPERATURE = new BigDecimal("38.5");

    /** Nhịp tim lúc nghỉ bình thường, nhịp/phút. (Normal resting heart rate, beats per minute.) */
    public static final BigDecimal MIN_HEART_RATE = new BigDecimal("28");
    public static final BigDecimal MAX_HEART_RATE = new BigDecimal("44");

    /** Điểm thể trạng chấp nhận được trên thang 1–9: dưới mức này là gầy, trên là béo. (Acceptable body condition on the 1-9 scale: below is thin, above is fat.) */
    public static final BigDecimal MIN_BODY_CONDITION = new BigDecimal("4");
    public static final BigDecimal MAX_BODY_CONDITION = new BigDecimal("7");

    private HealthReadingPolicy() {
        // lớp tiện ích (utility class)
    }

    /**
     * Những chỉ số của một lần đo nằm ngoài khoảng bình thường, mỗi chỉ số một câu để hiển thị thẳng cho người dùng.
     * Chỉ số không đo (null) thì bỏ qua. Rỗng nghĩa là lần đo bình thường.
     * (The values of one reading outside the normal range, one sentence each, shown as they are. A value that was not
     * measured (null) is skipped. Empty means a normal reading.)
     */
    public static List<String> issuesOf(BigDecimal temperature, BigDecimal heartRate, BigDecimal bodyCondition) {
        List<String> issues = new ArrayList<>();
        if (outside(temperature, MIN_TEMPERATURE, MAX_TEMPERATURE)) {
            issues.add(String.format(Locale.ROOT, "Temperature %.1f °C", temperature));
        }
        if (outside(heartRate, MIN_HEART_RATE, MAX_HEART_RATE)) {
            issues.add(String.format(Locale.ROOT, "Heart rate %.0f bpm", heartRate));
        }
        if (outside(bodyCondition, MIN_BODY_CONDITION, MAX_BODY_CONDITION)) {
            issues.add(String.format(Locale.ROOT, "Body condition %.1f", bodyCondition));
        }
        return issues;
    }

    private static boolean outside(BigDecimal value, BigDecimal min, BigDecimal max) {
        return value != null && (value.compareTo(min) < 0 || value.compareTo(max) > 0);
    }
}
