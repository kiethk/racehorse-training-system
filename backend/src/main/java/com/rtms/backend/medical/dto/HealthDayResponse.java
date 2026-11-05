package com.rtms.backend.medical.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * Một ngày trên biểu đồ sức khỏe: số lần đo và những con có lần đo bất thường trong ngày.
 * (One day on the health chart: the number of readings and the horses with an abnormal reading that day.)
 *
 * @param readings        số lần đo trong ngày (readings taken that day)
 * @param abnormalHorses  mỗi con chỉ xuất hiện một lần dù có nhiều lần đo bất thường, theo tên (each horse once even
 *                        with several abnormal readings, by name)
 */
public record HealthDayResponse(LocalDate date,
                                int readings,
                                List<AbnormalHorse> abnormalHorses) {

    /** @param issues câu giải thích hiển thị thẳng, ví dụ "Temperature 38.7 °C" (ready-to-show sentences) */
    public record AbnormalHorse(Long horseId, String horseName, List<String> issues) {
    }
}
