package com.rtms.backend.repository;

import com.rtms.backend.entity.GroomDailyTask;
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
}
