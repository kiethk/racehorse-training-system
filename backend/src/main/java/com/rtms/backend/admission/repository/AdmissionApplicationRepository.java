package com.rtms.backend.admission.repository;
import com.rtms.backend.admission.entity.CandidateHorseProfile;

import com.rtms.backend.admission.entity.AdmissionApplication;
import com.rtms.backend.admission.enums.AdmissionStatus;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;

@Repository
public interface AdmissionApplicationRepository
        extends JpaRepository<AdmissionApplication, Long> {

    List<AdmissionApplication> findByOwnerId(Long ownerId);

    List<AdmissionApplication> findByOwnerIdOrderBySubmittedAtDesc(Long ownerId);

    List<AdmissionApplication> findByOwnerIdOrderByCreatedAtDesc(Long ownerId);

    List<AdmissionApplication> findByStatus(AdmissionStatus status);

    @Query("""
            SELECT COUNT(a) > 0
            FROM AdmissionApplication a
            JOIN CandidateHorseProfile c ON c.admissionId = a.id
            WHERE c.registrationNumber = :ueln
              AND a.status NOT IN :completedStatuses
            """)
    boolean existsOpenAdmissionByUeln(@Param("ueln") String ueln,
                                     @Param("completedStatuses") Collection<AdmissionStatus> completedStatuses);

    List<AdmissionApplication> findByGroomId(Long groomId);

    Optional<AdmissionApplication> findFirstByStatusOrderBySubmittedAtAscIdAsc(AdmissionStatus status);

    List<AdmissionApplication> findByStatusAndArrivalDeadlineAtLessThanEqualOrderByArrivalDeadlineAtAscIdAsc(
            AdmissionStatus status, LocalDateTime cutoff);

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
     * Đơn đã sang bước Trainer nhưng chưa được gán ai (chưa có Trainer phù hợp
     * lúc Vet khám xong). Job định kỳ dùng để gán lại; đơn chờ lâu nhất trước.
     */
    List<AdmissionApplication> findByStatusAndTrainerIdIsNullOrderBySubmittedAtAscIdAsc(AdmissionStatus status);

    /** Hàng chờ của MỘT Trainer — đơn chờ lâu nhất lên đầu. */
    List<AdmissionApplication> findByTrainerIdAndStatusOrderBySubmittedAtAscIdAsc(Long trainerId,
                                                                               AdmissionStatus status);

    /**
     * Hồ sơ Trainer này ĐÃ đánh giá — không lọc theo status.
     *
     * Đánh giá xong là đơn chuyển MANAGER_REVIEW, rồi có thể thành APPROVED
     * hoặc REJECTED. Không trạng thái nào mang nghĩa "Trainer đã duyệt", nên
     * dấu vết duy nhất là trainer_reviewed_at.
     */
    List<AdmissionApplication> findByTrainerIdAndTrainerReviewedAtIsNotNullOrderByTrainerReviewedAtDesc(
            Long trainerId);

    List<AdmissionApplication> findByTrainerId(Long trainerId);

    /**
     * (trainerId, horseId) của các đơn đã giao cho Trainer mà chưa xong — phần
     * "việc đã giao nhưng chưa xong" trong tải của Trainer. Không tính phần này
     * thì nhiều đơn hoàn tất cùng lúc sẽ dồn hết về một người.
     */
    @Query("""
            SELECT a.trainerId, a.horseId
            FROM AdmissionApplication a
            WHERE a.status IN :statuses
              AND a.trainerId IN :trainerIds
              AND a.horseId IS NOT NULL
            """)
    List<Object[]> findAssignedHorsePairs(@Param("trainerIds") java.util.Collection<Long> trainerIds,
                                          @Param("statuses") Collection<AdmissionStatus> statuses);

    /**
     * Batch: the latest (by id desc) application that has a horseId in the given set.
     * Used to resolve HORSE_PHOTO for a list of horses without N+1 queries.
     */
    List<AdmissionApplication> findByHorseIdIn(Collection<Long> horseIds);

    /**
     * Finds all admission applications linked to a specific horse ID, newest first.
     */
    List<AdmissionApplication> findByHorseIdOrderBySubmittedAtDesc(Long horseId);

    List<AdmissionApplication> findAllByOrderBySubmittedAtDesc();
}

