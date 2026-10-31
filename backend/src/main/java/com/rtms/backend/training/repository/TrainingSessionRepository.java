package com.rtms.backend.training.repository;

import com.rtms.backend.training.entity.TrainingSession;
import com.rtms.backend.training.enums.SessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

@Repository
public interface TrainingSessionRepository extends JpaRepository<TrainingSession, Long> {

    /**
     * Khóa lịch của một Trainer trong suốt transaction hiện tại (advisory lock), dùng chung khóa với mọi thao tác xếp giờ.
     * (Locks the schedule of one trainer for the current transaction (advisory lock), shared by every operation that places
     * a Session.)
     */
    @Query(value = "SELECT 1 FROM (SELECT pg_advisory_xact_lock(:key)) locked", nativeQuery = true)
    int lockSchedule(@Param("key") long key);

    List<TrainingSession> findByGroupPlanIdOrderBySessionDateAscStartTimeAsc(Long groupPlanId);

    List<TrainingSession> findByGroupPlanIdInOrderBySessionDateAscStartTimeAsc(Collection<Long> groupPlanIds);

    /** Mọi buổi chưa hủy của Trainer trong một ngày, theo giờ tăng dần. (A trainer's non-cancelled Sessions of a day, ascending.) */
    List<TrainingSession> findByTrainerIdAndSessionDateAndStatusNotOrderByStartTimeAsc(
            Long trainerId, LocalDate sessionDate, SessionStatus status);

    long countByGroupPlanIdAndStatus(Long groupPlanId, SessionStatus status);
}
