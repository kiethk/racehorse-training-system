package com.rtms.backend.service;

import com.rtms.backend.dto.CreateGroomDailyTaskRequest;
import com.rtms.backend.entity.GroomDailyTask;
import com.rtms.backend.repository.GroomDailyTaskRepository;
import com.rtms.backend.repository.HorseRepository;
import com.rtms.backend.repository.UserRepository;
import com.rtms.backend.security.AuthenticatedUser;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class GroomDailyTaskService {

    private final GroomDailyTaskRepository taskRepository;
    private final HorseRepository horseRepository;
    private final UserRepository userRepository;

    public GroomDailyTaskService(GroomDailyTaskRepository taskRepository,
                                 HorseRepository horseRepository,
                                 UserRepository userRepository) {
        this.taskRepository = taskRepository;
        this.horseRepository = horseRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public GroomDailyTask createTask(CreateGroomDailyTaskRequest request, AuthenticatedUser currentUser) {
        if (request.getHorseId() == null) {
            throw new RuntimeException("Horse ID is required");
        }
        horseRepository.findById(request.getHorseId())
                .orElseThrow(() -> new RuntimeException("Horse not found with id: " + request.getHorseId()));

        if (request.getTaskType() == null) {
            throw new RuntimeException("Task type is required");
        }

        Long targetGroomId;
        if ("GROOM".equalsIgnoreCase(currentUser.getRole())) {
            targetGroomId = currentUser.getUserId();
        } else {
            if (request.getGroomId() == null) {
                throw new RuntimeException("Groom ID is required when assigning a task");
            }
            userRepository.findById(request.getGroomId())
                    .orElseThrow(() -> new RuntimeException("Groom not found with id: " + request.getGroomId()));
            targetGroomId = request.getGroomId();
        }

        LocalDateTime scheduledTime = request.getScheduledTime() != null
                ? request.getScheduledTime()
                : LocalDateTime.now();

        GroomDailyTask task = new GroomDailyTask(
                targetGroomId,
                request.getHorseId(),
                request.getTaskType(),
                scheduledTime,
                request.getNotes()
        );

        return taskRepository.save(task);
    }

    public List<GroomDailyTask> getDailyTasks(Long groomId, LocalDate date, AuthenticatedUser currentUser) {
        LocalDate targetDate = (date != null) ? date : LocalDate.now();
        LocalDateTime start = targetDate.atStartOfDay();
        LocalDateTime end = targetDate.atTime(23, 59, 59, 999999999);

        if ("GROOM".equalsIgnoreCase(currentUser.getRole())) {
            return taskRepository.findByGroomIdAndScheduledTimeBetween(currentUser.getUserId(), start, end);
        }

        if (groomId != null) {
            return taskRepository.findByGroomIdAndScheduledTimeBetween(groomId, start, end);
        }

        return taskRepository.findByScheduledTimeBetween(start, end);
    }

    @Transactional
    public GroomDailyTask completeTask(Long taskId, AuthenticatedUser currentUser) {
        GroomDailyTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found with id: " + taskId));

        if ("GROOM".equalsIgnoreCase(currentUser.getRole()) && !task.getGroomId().equals(currentUser.getUserId())) {
            throw new AccessDeniedException("You are not authorized to complete a task assigned to another groom");
        }

        task.setIsCompleted(true);
        task.setCompletedAt(LocalDateTime.now());
        return taskRepository.save(task);
    }
}
