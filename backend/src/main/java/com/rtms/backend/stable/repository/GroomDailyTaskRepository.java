package com.rtms.backend.stable.repository;

import com.rtms.backend.stable.entity.GroomDailyTask;
import com.rtms.backend.stable.enums.GroomTaskType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface GroomDailyTaskRepository extends JpaRepository<GroomDailyTask, Long> {

    List<GroomDailyTask> findByGroomId(Long groomId);

    List<GroomDailyTask> findByGroomIdAndScheduledTimeBetween(Long groomId, LocalDateTime start, LocalDateTime end);

    List<GroomDailyTask> findByScheduledTimeBetween(LocalDateTime start, LocalDateTime end);

    List<GroomDailyTask> findByHorseId(Long horseId);

    boolean existsByHorseIdAndTaskTypeAndScheduledTimeBetween(
            Long horseId,
            GroomTaskType taskType,
            LocalDateTime start,
            LocalDateTime end
    );
}
