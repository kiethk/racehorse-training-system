package com.rtms.backend.config;

import com.rtms.backend.enums.TrainingDay;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Chuyển Set<TrainingDay> thành chuỗi "MONDAY,WEDNESDAY,FRIDAY" và ngược lại.
 *
 * Vì sao dùng 1 cột VARCHAR chứ không phải bảng con (@ElementCollection):
 *   - Toàn bộ entity trong dự án dùng FK kiểu Long thuần, không có một quan hệ
 *     JPA nào. Nhét @ElementCollection vào sẽ là ngoại lệ duy nhất.
 *   - Tập giá trị tối đa 7 phần tử, luôn đọc/ghi trọn gói, không query lẻ
 *     -> lợi ích chuẩn hoá bằng 0.
 * Đánh đổi: không query được "tìm mọi plan có tập thứ Hai" bằng SQL.
 * Nghiệp vụ hiện tại không cần truy vấn đó.
 */
@Converter
public class TrainingDaySetConverter implements AttributeConverter<Set<TrainingDay>, String> {

    private static final String DELIMITER = ",";

    @Override
    public String convertToDatabaseColumn(Set<TrainingDay> days) {
        if (days == null || days.isEmpty()) {
            return null;
        }
        return days.stream()
                .map(Enum::name)
                .sorted()
                .collect(Collectors.joining(DELIMITER));
    }

    @Override
    public Set<TrainingDay> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return new LinkedHashSet<>();
        }
        return Arrays.stream(dbData.split(DELIMITER))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(TrainingDay::valueOf)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
