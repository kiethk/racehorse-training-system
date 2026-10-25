package com.rtms.backend.medical.repository;

import com.rtms.backend.medical.dto.HealthReadingRow;
import com.rtms.backend.medical.entity.HorseHealthMetric;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface HorseHealthMetricRepository extends JpaRepository<HorseHealthMetric, Long> {
    List<HorseHealthMetric> findByHorseIdOrderByRecordedAtDesc(Long horseId);

    List<HorseHealthMetric> findByHealthRecordIdOrderByRecordedAtAsc(Long healthRecordId);

    /**
     * Các lần đo của một tập chiến mã trong khoảng ngày, kèm tên ngựa, một câu truy vấn cho cả tập. Việc nào bất
     * thường do HealthReadingPolicy quyết định, không nằm trong SQL.
     * (The readings of a set of horses in a date range with the horse name, one query for the whole set. What is
     * abnormal is decided by HealthReadingPolicy, not in SQL.)
     *
     * @param since   đầu ngày đầu tiên của khoảng, bao gồm (start of the first day of the range, inclusive)
     * @param before  đầu ngày SAU ngày cuối của khoảng, không bao gồm (start of the day AFTER the last day, exclusive)
     */
    @Query("""
           SELECT new com.rtms.backend.medical.dto.HealthReadingRow(
                      CAST(m.recordedAt AS LocalDate), m.horseId, h.name,
                      m.temperature, m.heartRate, m.bodyConditionScore)
           FROM HorseHealthMetric m
           JOIN Horse h ON h.id = m.horseId
           WHERE m.horseId IN :horseIds
             AND m.recordedAt >= :since AND m.recordedAt < :before
           ORDER BY m.recordedAt ASC, m.id ASC
           """)
    List<HealthReadingRow> findHealthReadings(@Param("horseIds") Collection<Long> horseIds,
                                              @Param("since") LocalDateTime since,
                                              @Param("before") LocalDateTime before);
}
