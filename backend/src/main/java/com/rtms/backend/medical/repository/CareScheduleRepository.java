package com.rtms.backend.medical.repository;
import com.rtms.backend.admission.entity.AdmissionApplication;
import com.rtms.backend.admission.entity.CandidateHorseProfile;

import com.rtms.backend.medical.entity.CareSchedule;
import com.rtms.backend.medical.enums.CareSchedulePurpose;
import com.rtms.backend.medical.enums.CareScheduleStatus;
import com.rtms.backend.medical.enums.CareType;
import com.rtms.backend.admission.enums.AdmissionStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface CareScheduleRepository extends JpaRepository<CareSchedule, Long> {

    List<CareSchedule> findByHorseIdOrderByScheduledAtAsc(Long horseId);

    List<CareSchedule> findByHorseIdInAndScheduledAtBetweenAndStatusIn(
            Collection<Long> horseIds, LocalDateTime start, LocalDateTime end,
            Collection<CareScheduleStatus> statuses);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT cs FROM CareSchedule cs WHERE cs.id = :id")
    Optional<CareSchedule> findByIdForUpdate(@Param("id") Long id);

    Optional<CareSchedule> findFirstByAdmissionIdAndCareTypeOrderByCreatedAtDesc(Long admissionId, CareType careType);

    Optional<CareSchedule> findFirstByAdmissionIdAndStatusOrderByCreatedAtDesc(Long admissionId, CareScheduleStatus status);

    List<CareSchedule> findByHorseIdAndStatusIn(Long horseId, Collection<CareScheduleStatus> statuses);

    boolean existsByHorseIdAndCareTypeAndStatusIn(Long horseId, CareType careType, Collection<CareScheduleStatus> statuses);

    Optional<CareSchedule> findFirstByHorseIdAndCareTypeAndStatusInOrderByCreatedAtDesc(Long horseId, CareType careType, Collection<CareScheduleStatus> statuses);

    Optional<CareSchedule> findByRequestedByIdAndIdempotencyKey(Long requestedById, String idempotencyKey);

    Optional<CareSchedule> findBySourceScheduleId(Long sourceScheduleId);

    Optional<CareSchedule> findFirstByVeterinarianIdAndHorseIdAndStatusOrderByCompletedAtDesc(
            Long veterinarianId, Long horseId, CareScheduleStatus status);

    boolean existsByHorseIdAndCareTypeAndStatusInAndIdNot(Long horseId, CareType careType, Collection<CareScheduleStatus> statuses, Long id);


    @Query(value = """
            SELECT cs.* FROM care_schedule cs
            LEFT JOIN horse_incident_reports gir ON gir.id = cs.source_incident_id
            WHERE cs.status = 'REQUESTED'
              AND (cs.care_type IN ('URGENT', 'INITIAL') OR cs.requested_at IS NULL OR cs.requested_at < :horizon)
            ORDER BY CASE cs.care_type WHEN 'URGENT' THEN 0 WHEN 'INITIAL' THEN 1 ELSE 2 END,
                     CASE WHEN cs.care_type = 'URGENT' THEN
                       CASE gir.severity WHEN 'CRITICAL' THEN 0 WHEN 'HIGH' THEN 1 WHEN 'MEDIUM' THEN 2 ELSE 3 END
                     ELSE 0 END,
                     cs.created_at ASC, cs.id ASC
            LIMIT :batchSize
            FOR UPDATE OF cs SKIP LOCKED
            """, nativeQuery = true)
    List<CareSchedule> lockNextRequestedBatch(@Param("batchSize") int batchSize,
                                              @Param("horizon") LocalDateTime horizon);

    @Query("SELECT cs FROM CareSchedule cs WHERE cs.veterinarianId IS NOT NULL "
            + "AND cs.status IN :statuses AND cs.scheduledAt >= :dayStart AND cs.scheduledAt < :dayEnd")
    List<CareSchedule> findAssignedInDay(@Param("dayStart") LocalDateTime dayStart,
            @Param("dayEnd") LocalDateTime dayEnd,
            @Param("statuses") Collection<CareScheduleStatus> statuses);

    boolean existsByVeterinarianIdAndHorseIdAndStatusIn(Long veterinarianId, Long horseId,
            Collection<CareScheduleStatus> statuses);

    List<CareSchedule> findByVeterinarianIdAndCareTypeAndStatus(
            Long veterinarianId, CareType careType, CareScheduleStatus status);

    List<CareSchedule> findByVeterinarianIdAndCareTypeAndStatusIn(
            Long veterinarianId, CareType careType, Collection<CareScheduleStatus> statuses);

    boolean existsByVeterinarianIdAndStatus(Long veterinarianId, CareScheduleStatus status);

    boolean existsByHorseIdAndStatus(Long horseId, CareScheduleStatus status);

    @Query("SELECT cs FROM CareSchedule cs WHERE cs.horseId = :horseId AND cs.status = :status AND cs.scheduledAt IS NOT NULL")
    List<CareSchedule> findScheduledForHorse(@Param("horseId") Long horseId, @Param("status") CareScheduleStatus status);

    @Query("SELECT cs FROM CareSchedule cs WHERE cs.veterinarianId = :vetId AND cs.status = :status AND cs.scheduledAt IS NOT NULL")
    List<CareSchedule> findScheduledForVet(@Param("vetId") Long vetId, @Param("status") CareScheduleStatus status);

    List<CareSchedule> findByStatusInAndScheduledAtIsNotNull(Collection<CareScheduleStatus> statuses);

    List<CareSchedule> findByStatusInAndVeterinarianIdIsNotNull(Collection<CareScheduleStatus> statuses);

    List<CareSchedule> findByVeterinarianIdAndStatus(Long veterinarianId, CareScheduleStatus status);

    @Query("SELECT cs FROM CareSchedule cs WHERE (:status IS NULL OR cs.status = :status) "
            + "AND (:careType IS NULL OR cs.careType = :careType) "
            + "AND (:horseId IS NULL OR cs.horseId = :horseId) "
            + "AND (:vetId IS NULL OR cs.veterinarianId = :vetId) "
            + "AND (:admissionId IS NULL OR cs.admissionId = :admissionId) "
            + "AND (cast(:dayStart as timestamp) IS NULL OR cs.scheduledAt >= :dayStart) "
            + "AND (cast(:dayEnd as timestamp) IS NULL OR cs.scheduledAt < :dayEnd)")
    Page<CareSchedule> findFiltered(@Param("status") CareScheduleStatus status,
            @Param("careType") CareType careType, @Param("horseId") Long horseId,
            @Param("vetId") Long vetId, @Param("admissionId") Long admissionId,
            @Param("dayStart") LocalDateTime dayStart,
            @Param("dayEnd") LocalDateTime dayEnd, Pageable pageable);

    @Query("SELECT cs FROM CareSchedule cs, AdmissionApplication a WHERE cs.admissionId = a.id "
            + "AND (:status IS NULL OR cs.status = :status) AND (:careType IS NULL OR cs.careType = :careType) "
            + "AND (:horseId IS NULL OR cs.horseId = :horseId) AND (:vetId IS NULL OR cs.veterinarianId = :vetId) "
            + "AND (:admissionId IS NULL OR cs.admissionId = :admissionId) "
            + "AND a.groomId = :userId")
    Page<CareSchedule> findFilteredForGroom(@Param("status") CareScheduleStatus status,
            @Param("careType") CareType careType, @Param("horseId") Long horseId,
            @Param("vetId") Long vetId, @Param("admissionId") Long admissionId,
            @Param("userId") Long userId, Pageable pageable);

    boolean existsByAdmissionIdAndVeterinarianId(Long admissionId, Long veterinarianId);

    @Query("""
        SELECT cs FROM CareSchedule cs
        WHERE cs.admissionId IS NOT NULL
          AND cs.veterinarianId = :vetId
          AND cs.status != 'CANCELLED'
        ORDER BY
          CASE cs.careType WHEN 'URGENT' THEN 0 WHEN 'INITIAL' THEN 1 ELSE 2 END,
          cs.scheduledAt ASC, cs.id DESC
    """)
    List<CareSchedule> findVetAdmissionSchedules(@Param("vetId") Long vetId);

    /**
     * Hàng đợi khám của bác sĩ thú y. Mặc định (scheduleStatus = null) ẩn các ca
     * COMPLETED và CANCELLED; khi người dùng chọn một trạng thái cụ thể thì bộ lọc
     * đó được ưu tiên, nên ca đã hoàn thành vẫn xem được qua bộ lọc Completed.
     * Bộ lọc pill (Awaiting / In exam) chỉ mô tả ca đang chờ nên bị bỏ qua khi
     * đã chọn trạng thái cụ thể.
     */
    @Query(value = """
        SELECT cs FROM CareSchedule cs, AdmissionApplication a, CandidateHorseProfile c
        WHERE cs.admissionId = a.id AND c.admissionId = a.id
          AND cs.veterinarianId = :vetId
          AND ((:scheduleStatus IS NOT NULL AND cs.status = :scheduleStatus)
               OR (:scheduleStatus IS NULL AND cs.status NOT IN ('COMPLETED','CANCELLED')))
          AND (:searching = false OR LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(COALESCE(c.breed, '')) LIKE LOWER(CONCAT('%', :search, '%'))
               OR CAST(a.id AS string) LIKE CONCAT('%', :search, '%'))
          AND (:admissionStatus IS NULL OR a.status = :admissionStatus)
          AND (:careType IS NULL OR cs.careType = :careType)
          AND (:scheduleStatus IS NOT NULL
               OR :pill = '' OR :pill = 'ALL'
               OR (:pill = 'ACTIVE' AND a.status <> com.rtms.backend.admission.enums.AdmissionStatus.APPROVED)
               OR (:pill = 'AWAITING' AND cs.status IN ('REQUESTED','SCHEDULED') AND a.status <> com.rtms.backend.admission.enums.AdmissionStatus.APPROVED)
               OR (:pill = 'IN_PROGRESS' AND cs.status = 'IN_PROGRESS' AND a.status <> com.rtms.backend.admission.enums.AdmissionStatus.APPROVED)
               OR (:pill = 'APPROVED' AND a.status = com.rtms.backend.admission.enums.AdmissionStatus.APPROVED))
          AND (:priority = ''
               OR (:priority = 'URGENT' AND cs.careType = 'URGENT')
               OR (:priority = 'NORMAL' AND cs.careType <> 'URGENT'))
        ORDER BY CASE cs.careType WHEN 'URGENT' THEN 0 WHEN 'INITIAL' THEN 1 ELSE 2 END,
                 COALESCE(cs.scheduledAt, cs.createdAt) ASC, cs.id ASC
        """,
        countQuery = """
        SELECT COUNT(cs.id) FROM CareSchedule cs, AdmissionApplication a, CandidateHorseProfile c
        WHERE cs.admissionId = a.id AND c.admissionId = a.id
          AND cs.veterinarianId = :vetId
          AND ((:scheduleStatus IS NOT NULL AND cs.status = :scheduleStatus)
               OR (:scheduleStatus IS NULL AND cs.status NOT IN ('COMPLETED','CANCELLED')))
          AND (:searching = false OR LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(COALESCE(c.breed, '')) LIKE LOWER(CONCAT('%', :search, '%'))
               OR CAST(a.id AS string) LIKE CONCAT('%', :search, '%'))
          AND (:admissionStatus IS NULL OR a.status = :admissionStatus)
          AND (:careType IS NULL OR cs.careType = :careType)
          AND (:scheduleStatus IS NOT NULL
               OR :pill = '' OR :pill = 'ALL'
               OR (:pill = 'ACTIVE' AND a.status <> com.rtms.backend.admission.enums.AdmissionStatus.APPROVED)
               OR (:pill = 'AWAITING' AND cs.status IN ('REQUESTED','SCHEDULED') AND a.status <> com.rtms.backend.admission.enums.AdmissionStatus.APPROVED)
               OR (:pill = 'IN_PROGRESS' AND cs.status = 'IN_PROGRESS' AND a.status <> com.rtms.backend.admission.enums.AdmissionStatus.APPROVED)
               OR (:pill = 'APPROVED' AND a.status = com.rtms.backend.admission.enums.AdmissionStatus.APPROVED))
          AND (:priority = ''
               OR (:priority = 'URGENT' AND cs.careType = 'URGENT')
               OR (:priority = 'NORMAL' AND cs.careType <> 'URGENT'))
        """)
    Page<CareSchedule> findVetQueuePage(
            @Param("vetId") Long vetId, @Param("searching") boolean searching,
            @Param("search") String search, @Param("pill") String pill,
            @Param("admissionStatus") AdmissionStatus admissionStatus,
            @Param("scheduleStatus") CareScheduleStatus scheduleStatus,
            @Param("careType") CareType careType, @Param("priority") String priority,
            Pageable pageable);

    long countByVeterinarianIdAndAdmissionIdIsNotNullAndStatusIn(Long vetId, Collection<CareScheduleStatus> statuses);

    @Query("""
        SELECT COUNT(cs.id) FROM CareSchedule cs, AdmissionApplication a
        WHERE cs.admissionId = a.id AND cs.veterinarianId = :vetId
          AND cs.status IN :statuses
          AND a.status <> com.rtms.backend.admission.enums.AdmissionStatus.APPROVED
    """)
    long countActiveVetQueueItems(@Param("vetId") Long vetId, @Param("statuses") Collection<CareScheduleStatus> statuses);

    @Query("""
        SELECT COUNT(cs.id) FROM CareSchedule cs, AdmissionApplication a
        WHERE cs.admissionId = a.id AND cs.veterinarianId = :vetId
          AND cs.status IN :statuses
          AND a.status = com.rtms.backend.admission.enums.AdmissionStatus.APPROVED
    """)
    long countApprovedVetQueueItems(@Param("vetId") Long vetId, @Param("statuses") Collection<CareScheduleStatus> statuses);


    boolean existsByHorseIdAndStatusInAndIdNot(Long horseId, Collection<CareScheduleStatus> statuses, Long id);

    boolean existsByHorseIdAndPurposeAndStatusInAndIdNot(Long horseId, CareSchedulePurpose purpose,
            Collection<CareScheduleStatus> statuses, Long id);

    /** Mốc tiêm phòng đang chờ của ngựa; partial unique index đảm bảo tối đa một. */
    Optional<CareSchedule> findFirstByHorseIdAndPurposeAndStatusIn(Long horseId, CareSchedulePurpose purpose,
            Collection<CareScheduleStatus> statuses);

    /**
     * Lần khám sắp tới của ngựa (đã xếp giờ thì lấy giờ xếp, chưa thì lấy giờ
     * yêu cầu). Ngựa đang tạm nghỉ thì đây chính là mốc "tạm nghỉ đến".
     * Mốc tiêm phòng không tính: nó không phải lần tái khám để mở khóa tập.
     * null nếu không còn lần khám nào đang chờ.
     */
    @Query("""
            SELECT MIN(COALESCE(cs.scheduledAt, cs.requestedAt))
            FROM CareSchedule cs
            WHERE cs.horseId = :horseId AND cs.status IN :statuses
              AND cs.purpose = com.rtms.backend.medical.enums.CareSchedulePurpose.FOLLOW_UP
            """)
    LocalDateTime findNextExamTime(@Param("horseId") Long horseId,
                                   @Param("statuses") Collection<CareScheduleStatus> statuses);
}
