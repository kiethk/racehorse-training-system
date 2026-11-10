package com.rtms.backend.training.repository;

import com.rtms.backend.training.entity.CourseRun;
import com.rtms.backend.training.enums.CourseRunStatus;
import com.rtms.backend.training.dto.DashboardHorseRow;
import com.rtms.backend.training.dto.DashboardCourseRunRow;
import com.rtms.backend.training.dto.HorseCourseRunResponse;
import com.rtms.backend.training.enums.SessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

@Repository
public interface CourseRunRepository extends JpaRepository<CourseRun, Long> {

    List<CourseRun> findByGroupIdAndStatusIn(Long groupId, Collection<CourseRunStatus> statuses);

    boolean existsByGroupId(Long groupId);

    boolean existsByGroupIdAndStatusIn(Long groupId, Collection<CourseRunStatus> statuses);

    List<CourseRun> findByGroupIdInAndStatusIn(Collection<Long> groupIds, Collection<CourseRunStatus> statuses);

    List<CourseRun> findByTrainerIdOrderByStartDateDesc(Long trainerId);

    List<CourseRun> findAllByOrderByStartDateDesc();

    List<CourseRun> findByStatusAndStartDateLessThanEqual(CourseRunStatus status, LocalDate date);

    List<CourseRun> findByStatusIn(Collection<CourseRunStatus> statuses);

    /** Kế hoạch chưa đóng không còn buổi nào ở trạng thái đã cho. (Unfinished course runs with no Session in the given status.) */
    @Query("""
           SELECT p FROM CourseRun p
           WHERE p.status IN :statuses
             AND NOT EXISTS (SELECT 1 FROM TrainingSession s WHERE s.courseRunId = p.id AND s.status = :scheduled)
           """)
    List<CourseRun> findWithoutScheduledSessions(@Param("statuses") Collection<CourseRunStatus> statuses,
                                                 @Param("scheduled") SessionStatus scheduled);

    long countByStatus(CourseRunStatus status);

    /**
     * Các kế hoạch có trạng thái cho trước mà chiến mã còn Workout chưa hủy, kế hoạch kết thúc muộn nhất trước. Ngựa đã rời nhóm thì
     * Workout còn lại đã bị hủy, nên course run của nhóm cũ không còn tính là của nó.
     * (The course runs in the given statuses where the horse still has a Workout that is not cancelled, latest end first. A horse that left
     * its group had its remaining Workouts cancelled, so the old group's course run no longer counts as its own.)
     */
    @Query("""
           SELECT DISTINCT p FROM CourseRun p
           JOIN TrainingSession s ON s.courseRunId = p.id
           JOIN TrainingWorkout w ON w.sessionId = s.id
           WHERE w.horseId = :horseId AND p.status IN :statuses
             AND w.status <> com.rtms.backend.training.enums.WorkoutStatus.CANCELLED
           ORDER BY p.endDate DESC
           """)
    List<CourseRun> findCourseRunsOfHorse(@Param("horseId") Long horseId, @Param("statuses") Collection<CourseRunStatus> statuses);

    /**
     * Danh sách kế hoạch theo từng chiến mã kèm tên ngựa, tên khóa và ba con số tiến độ. totalSessions là số buổi của chính kế
     * hoạch, cancelledSessions là số buổi đã hủy CẢ BUỔI, completedSessions là số Workout đã RAN của chính con ngựa đó.
     * trainerId = null nghĩa là mọi Trainer; horseId = null nghĩa là mọi ngựa. Kế hoạch mới nhất lên đầu.
     * (Course run list per horse with horse name, course name and the three progress counters. totalSessions is the course run's own number of
     * Sessions, cancelledSessions the Sessions cancelled as a whole, completedSessions the horse's own Workouts that Ran.)
     */
    @Query("""
           SELECT new com.rtms.backend.training.dto.HorseCourseRunResponse(
                      p.id, w.horseId, h.name, p.courseId, c.name, p.startDate, p.endDate, p.status,
                      (SELECT COUNT(a) FROM TrainingSession a WHERE a.courseRunId = p.id),
                      COUNT(CASE WHEN w.status = com.rtms.backend.training.enums.WorkoutStatus.RAN THEN 1 END),
                      (SELECT COUNT(b) FROM TrainingSession b
                       WHERE b.courseRunId = p.id AND b.status = com.rtms.backend.training.enums.SessionStatus.CANCELLED))
           FROM TrainingWorkout w
           JOIN TrainingSession s ON w.sessionId = s.id
           JOIN CourseRun p ON s.courseRunId = p.id
           JOIN Horse h ON w.horseId = h.id
           JOIN Course c ON p.courseId = c.id
           WHERE (:trainerId IS NULL OR p.trainerId = :trainerId)
             AND (:horseId IS NULL OR w.horseId = :horseId)
           GROUP BY p.id, w.horseId, h.name, p.courseId, c.name, p.startDate, p.endDate, p.status
           ORDER BY p.startDate DESC, p.id DESC
           """)
    List<HorseCourseRunResponse> findHorseCourseRuns(@Param("trainerId") Long trainerId, @Param("horseId") Long horseId);

    // =================================================================
    // DASHBOARD THEO PHẠM VI VAI TRÒ (Dashboard by role scope)
    // =================================================================

    /** Chiến mã trong các khu của Trainer, kèm mã chuồng. (Horses in the Trainer's areas, with stall code.) */
    @Query("""
           SELECT new com.rtms.backend.training.dto.DashboardHorseRow(h.id, h.name, h.breed, ss.stallCode)
           FROM Horse h
           JOIN StableStall ss ON h.currentStallId = ss.id
           JOIN Area a ON ss.areaId = a.id
           WHERE a.trainerId = :trainerId
           ORDER BY h.id ASC
           """)
    List<DashboardHorseRow> findDashboardHorses(@Param("trainerId") Long trainerId);

    /** Chiến mã đã nhập trại của một Owner; ngựa chưa có chuồng vẫn được liệt kê. (The admitted horses of one Owner, stall or not.) */
    @Query("""
           SELECT new com.rtms.backend.training.dto.DashboardHorseRow(h.id, h.name, h.breed, ss.stallCode)
           FROM Horse h
           LEFT JOIN StableStall ss ON h.currentStallId = ss.id
           WHERE h.ownerId = :ownerId
             AND h.currentStatus = com.rtms.backend.horse.enums.HorseStatus.ELIGIBLE
           ORDER BY h.id ASC
           """)
    List<DashboardHorseRow> findDashboardHorsesOfOwner(@Param("ownerId") Long ownerId);

    /** Mọi chiến mã đã nhập trại của trang trại. (Every admitted horse of the farm.) */
    @Query("""
           SELECT new com.rtms.backend.training.dto.DashboardHorseRow(h.id, h.name, h.breed, ss.stallCode)
           FROM Horse h
           LEFT JOIN StableStall ss ON h.currentStallId = ss.id
           WHERE h.currentStatus = com.rtms.backend.horse.enums.HorseStatus.ELIGIBLE
           ORDER BY h.id ASC
           """)
    List<DashboardHorseRow> findAllDashboardHorses();

    /**
     * Mọi kế hoạch của các chiến mã đã cho (qua Workout của chúng), kèm tên khóa, số buổi của kế hoạch, số buổi đã hủy cả buổi và số
     * Workout đã HELD của chính con ngựa đó.
     * (Every course run of the given horses (through their Workouts), with the course name, the course run's Session count, the Sessions
     * cancelled as a whole and the horse's own HELD Workouts.)
     */
    @Query("""
           SELECT new com.rtms.backend.training.dto.DashboardCourseRunRow(
                      p.id, w.horseId, p.status, p.startDate, p.endDate, c.name,
                      CAST((SELECT COUNT(a) FROM TrainingSession a WHERE a.courseRunId = p.id) AS integer),
                      COUNT(CASE WHEN w.status = com.rtms.backend.training.enums.WorkoutStatus.RAN THEN 1 END),
                      (SELECT COUNT(b) FROM TrainingSession b
                       WHERE b.courseRunId = p.id AND b.status = com.rtms.backend.training.enums.SessionStatus.CANCELLED))
           FROM TrainingWorkout w
           JOIN TrainingSession s ON w.sessionId = s.id
           JOIN CourseRun p ON s.courseRunId = p.id
           JOIN Course c ON p.courseId = c.id
           WHERE w.horseId IN :horseIds
           GROUP BY p.id, w.horseId, p.status, p.startDate, p.endDate, c.name
           ORDER BY p.id ASC
           """)
    List<DashboardCourseRunRow> findDashboardCourseRuns(@Param("horseIds") Collection<Long> horseIds);

    /** Ngựa có nằm trong một khu của Trainer không? (Is the horse in one of the Trainer's areas?) */
    @Query("""
           SELECT COUNT(h) > 0
           FROM Horse h
           JOIN StableStall ss ON h.currentStallId = ss.id
           JOIN Area a ON ss.areaId = a.id
           WHERE h.id = :horseId AND a.trainerId = :trainerId
           """)
    boolean existsHorseInTrainerArea(@Param("horseId") Long horseId, @Param("trainerId") Long trainerId);

    /** Số chiến mã đang tập: có Workout còn hiệu lực trong một kế hoạch ACTIVE. (Horses in training: with an effective Workout in an ACTIVE course run.) */
    @Query("""
           SELECT COUNT(DISTINCT w.horseId)
           FROM TrainingWorkout w
           JOIN TrainingSession s ON w.sessionId = s.id
           JOIN CourseRun p ON s.courseRunId = p.id
           WHERE p.status = com.rtms.backend.training.enums.CourseRunStatus.ACTIVE
             AND w.status <> com.rtms.backend.training.enums.WorkoutStatus.CANCELLED
           """)
    long countHorsesInTraining();
}
