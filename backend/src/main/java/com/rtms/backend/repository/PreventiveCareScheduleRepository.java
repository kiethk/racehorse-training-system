package com.rtms.backend.repository;

import com.rtms.backend.entity.PreventiveCareSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface PreventiveCareScheduleRepository extends JpaRepository<PreventiveCareSchedule, Long> {
    List<PreventiveCareSchedule> findByHorseIdOrderByScheduledDateAsc(Long horseId);
    List<PreventiveCareSchedule> findByStatusOrderByScheduledDateAsc(String status);
    List<PreventiveCareSchedule> findByHorseIdInAndScheduledDate(List<Long> horseIds,
                                                                  LocalDate scheduledDate);
    List<PreventiveCareSchedule> findByHorseIdAndStatusIn(Long horseId, List<String> statuses);
}