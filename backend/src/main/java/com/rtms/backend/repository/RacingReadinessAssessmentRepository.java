package com.rtms.backend.repository;

import com.rtms.backend.entity.RacingReadinessAssessment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RacingReadinessAssessmentRepository
        extends JpaRepository<RacingReadinessAssessment, Long> {

    Optional<RacingReadinessAssessment> findByAdmissionId(Long admissionId);

    /** Toàn bộ lịch sử của một chiến mã, mới nhất trước. */
    List<RacingReadinessAssessment> findByHorseIdOrderByAssessmentDateDesc(Long horseId);

    /**
     * CHỈ đánh giá định kỳ — dùng cho biểu đồ tiến bộ.
     * Bỏ mốc nhập học vì fitness lúc đó là ước lượng bằng mắt, không cùng
     * thang với điểm đo được sau khi tập.
     */
    List<RacingReadinessAssessment>
        findByHorseIdAndAdmissionIdIsNullOrderByAssessmentDateAsc(Long horseId);
}