package com.rtms.backend.repository;
import com.rtms.backend.entity.AdmissionApplication;
import com.rtms.backend.entity.CandidateHorseProfile;
import com.rtms.backend.enums.AdmissionStatus;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AdmissionApplicationRepository
        extends JpaRepository<AdmissionApplication, Long> {

    List<AdmissionApplication> findByOwnerId(Long ownerId);

    List<AdmissionApplication> findByOwnerIdOrderBySubmittedAtDesc(Long ownerId);

    List<AdmissionApplication> findByStatus(AdmissionStatus status);

    Optional<AdmissionApplication> findFirstByStatusOrderBySubmittedAtAscIdAsc(AdmissionStatus status);

    @Query("""
            SELECT a FROM AdmissionApplication a
            JOIN CandidateHorseProfile c ON c.admissionId = a.id
            WHERE (:filterStatus = false OR a.status = :status)
              AND (:filterName = false OR LOWER(c.name) LIKE LOWER(CONCAT('%', :candidateName, '%')))
              AND (:filterFrom = false OR a.submittedAt >= :submittedFrom)
              AND (:filterTo = false OR a.submittedAt < :submittedToExclusive)
            """)
    Page<AdmissionApplication> findGroomQueue(
            @Param("filterStatus") boolean filterStatus,
            @Param("status") AdmissionStatus status,
            @Param("filterName") boolean filterName,
            @Param("candidateName") String candidateName,
            @Param("filterFrom") boolean filterFrom,
            @Param("submittedFrom") java.time.LocalDateTime submittedFrom,
            @Param("filterTo") boolean filterTo,
            @Param("submittedToExclusive") java.time.LocalDateTime submittedToExclusive,
            Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT a
            FROM AdmissionApplication a
            WHERE a.id = :id
            """)
    Optional<AdmissionApplication> findByIdForUpdate(@Param("id") Long id);

    /**
     * Hàng chờ đánh giá của MỘT Trainer.
     *
     * Điều kiện "trainerId IS NULL" là cầu nối tạm thời: bước Thú y sẽ chạy
     * thuật toán chọn Trainer và ghi sẵn trainer_id, nhưng phần đó chưa làm.
     * Nếu lọc chặt ngay (chỉ trainer_id = :trainerId) thì toàn bộ đơn đang có
     * trong cơ sở dữ liệu — vốn đều mang trainer_id = NULL — biến mất khỏi mọi
     * hàng chờ, không ai đánh giá được gì nữa.
     *
     * Đơn chưa phân công vẫn hiện cho mọi Trainer (đúng như hành vi cũ), còn
     * đơn ĐÃ phân công thì chỉ người được chỉ định thấy. Khi bước Thú y hoàn
     * thành, mọi đơn mới đều có trainer_id nên nhánh NULL tự khô cạn mà không
     * cần sửa gì ở đây.
     *
     * TODO(sau khi Thú y gán trainer_id tự động): bỏ "OR a.trainerId IS NULL".
     *
     * Sắp xếp tăng dần theo ngày nộp — đơn chờ lâu nhất lên đầu, tránh để hồ
     * sơ cũ bị chôn dưới đáy danh sách.
     */
    @Query("""
            SELECT a FROM AdmissionApplication a
            WHERE a.status = :status
              AND (a.trainerId = :trainerId OR a.trainerId IS NULL)
            ORDER BY a.submittedAt ASC, a.id ASC
            """)
    List<AdmissionApplication> findTrainerPendingQueue(@Param("trainerId") Long trainerId,
                                                       @Param("status") AdmissionStatus status);

    /**
     * Hồ sơ Trainer này ĐÃ đánh giá — không lọc theo status.
     *
     * Lý do không dùng status: đánh giá xong là đơn chuyển MANAGER_REVIEW, rồi
     * có thể thành APPROVED hoặc REJECTED. Không trạng thái nào mang nghĩa
     * "Trainer đã duyệt", nên dấu vết duy nhất là trainer_reviewed_at.
     */
    List<AdmissionApplication> findByTrainerIdAndTrainerReviewedAtIsNotNullOrderByTrainerReviewedAtDesc(
            Long trainerId);
}
