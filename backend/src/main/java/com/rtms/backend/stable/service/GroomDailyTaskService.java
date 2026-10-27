package com.rtms.backend.stable.service;
import com.rtms.backend.identity.entity.Role;
import com.rtms.backend.medical.enums.CareScheduleStatus;


import com.rtms.backend.stable.dto.CreateGroomDailyTaskRequest;
import com.rtms.backend.stable.dto.TodayTaskItemResponse;
import com.rtms.backend.stable.entity.GroomDailyTask;
import com.rtms.backend.stable.entity.GroomTaskCompletion;
import com.rtms.backend.horse.entity.Horse;
import com.rtms.backend.medical.entity.CareSchedule;
import com.rtms.backend.stable.entity.StableStall;
import com.rtms.backend.training.entity.Subject;
import com.rtms.backend.training.dto.GroomWorkoutRow;
import com.rtms.backend.stable.enums.GroomTaskType;
import com.rtms.backend.stable.enums.SopSlot;
import com.rtms.backend.stable.enums.TaskSource;
import com.rtms.backend.stable.repository.GroomDailyTaskRepository;
import com.rtms.backend.stable.repository.GroomTaskCompletionRepository;
import com.rtms.backend.horse.repository.HorseRepository;
import com.rtms.backend.medical.repository.CareScheduleRepository;
import com.rtms.backend.stable.repository.StableStallRepository;
import com.rtms.backend.training.repository.TrainingWorkoutRepository;
import com.rtms.backend.identity.repository.UserRepository;
import com.rtms.backend.security.AuthenticatedUser;
import com.rtms.backend.common.storage.FileStorageService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
public class GroomDailyTaskService {

    private final GroomDailyTaskRepository taskRepository;
    private final HorseRepository horseRepository;
    private final UserRepository userRepository;
    private final StableStallRepository stableStallRepository;
    private final TrainingWorkoutRepository workoutRepository;
    private final CareScheduleRepository careScheduleRepository;
    private final FileStorageService fileStorage;
    private final GroomTaskCompletionRepository completionRepository;

    private static final int MAX_COMPLETION_NOTE_LENGTH = 1000;
    private static final int MAX_COMPLETION_PHOTOS = 5;
    private static final int MAX_TASKS_PER_COMPLETION = 20;

    public GroomDailyTaskService(GroomDailyTaskRepository taskRepository,
                                 HorseRepository horseRepository,
                                 UserRepository userRepository,
                                 StableStallRepository stableStallRepository,
                                 TrainingWorkoutRepository workoutRepository,
                                 CareScheduleRepository careScheduleRepository,
                                 FileStorageService fileStorage,
                                 GroomTaskCompletionRepository completionRepository) {
        this.fileStorage = fileStorage;
        this.completionRepository = completionRepository;
        this.taskRepository = taskRepository;
        this.horseRepository = horseRepository;
        this.userRepository = userRepository;
        this.stableStallRepository = stableStallRepository;
        this.workoutRepository = workoutRepository;
        this.careScheduleRepository = careScheduleRepository;
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

    /**
     * Completes one task with what the Groom recorded. Kept for callers that complete a single task;
     * the checklist completes a whole block through {@link #completeTasks}.
     */
    @Transactional
    public GroomDailyTask completeTask(Long taskId, String note, MultipartFile image, AuthenticatedUser currentUser) {
        completeTasks(List.of(taskId), note, image == null ? List.of() : List.of(image), currentUser);
        return taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found with id: " + taskId));
    }

    /**
     * Completes several routine tasks in one action (the same duty at the same time for several horses) and
     * records one optional note and up to five optional photos for all of them. A task is completed once;
     * its evidence cannot be replaced afterwards.
     */
    @Transactional
    public GroomTaskCompletion completeTasks(List<Long> taskIds, String note, List<MultipartFile> photos,
                                             AuthenticatedUser currentUser) {
        List<Long> ids = taskIds == null ? List.of()
                : taskIds.stream().filter(Objects::nonNull).distinct().toList();
        if (ids.isEmpty() || ids.size() > MAX_TASKS_PER_COMPLETION) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Select between 1 and " + MAX_TASKS_PER_COMPLETION + " tasks to complete");
        }

        List<GroomDailyTask> tasks = taskRepository.findAllById(ids);
        if (tasks.size() != ids.size()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "One or more tasks were not found");
        }
        boolean isGroom = "GROOM".equalsIgnoreCase(currentUser.getRole());
        Long groomId = tasks.get(0).getGroomId();
        for (GroomDailyTask task : tasks) {
            if (isGroom && !task.getGroomId().equals(currentUser.getUserId())) {
                throw new AccessDeniedException("You are not authorized to complete a task assigned to another groom");
            }
            if (!task.getGroomId().equals(groomId)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Tasks completed together must belong to the same groom");
            }
            if (Boolean.TRUE.equals(task.getIsCompleted())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Task #" + task.getId() + " is already completed");
            }
        }

        String cleanNote = note == null || note.isBlank() ? null : note.trim();
        if (cleanNote != null && cleanNote.length() > MAX_COMPLETION_NOTE_LENGTH) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "The note is too long. The limit is " + MAX_COMPLETION_NOTE_LENGTH + " characters.");
        }

        List<MultipartFile> files = photos == null ? List.of()
                : photos.stream().filter(file -> file != null && !file.isEmpty()).toList();
        if (files.size() > MAX_COMPLETION_PHOTOS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Attach at most " + MAX_COMPLETION_PHOTOS + " photos");
        }
        for (MultipartFile file : files) {
            String mediaType = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
            if (!mediaType.startsWith("image/")) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Task photos must be images (JPEG, PNG, WebP)");
            }
        }

        LocalDateTime now = LocalDateTime.now();
        GroomTaskCompletion completion = completionRepository.save(new GroomTaskCompletion(groomId, cleanNote, now));
        List<String> storedKeys = new ArrayList<>();
        try {
            for (MultipartFile file : files) {
                // validates size and real content
                storedKeys.add(fileStorage.store(file, "rtms/task-completions/" + completion.getId()));
            }
            completion.getPhotoKeys().addAll(storedKeys);
            GroomTaskCompletion saved = completionRepository.save(completion);
            for (GroomDailyTask task : tasks) {
                task.setIsCompleted(true);
                task.setCompletedAt(now);
                task.setCompletionId(saved.getId());
            }
            taskRepository.saveAll(tasks);
            return saved;
        } catch (RuntimeException ex) {
            storedKeys.forEach(fileStorage::delete);   // nothing was saved -> do not leave orphan files
            throw ex;
        }
    }

    /** Storage key of one completion photo. A Groom may only open photos of their own completions. */
    private String completionPhotoKey(Long completionId, int index, AuthenticatedUser currentUser) {
        GroomTaskCompletion completion = completionRepository.findById(completionId)
                .orElseThrow(() -> new RuntimeException("Task completion not found with id: " + completionId));
        if ("GROOM".equalsIgnoreCase(currentUser.getRole()) && !completion.getGroomId().equals(currentUser.getUserId())) {
            throw new AccessDeniedException("You can only view photos of your own tasks");
        }
        if (index < 0 || index >= completion.getPhotoKeys().size()) {
            throw new RuntimeException("Photo " + index + " of task completion #" + completionId + " not found");
        }
        return completion.getPhotoKeys().get(index);
    }

    public Resource loadCompletionPhoto(Long completionId, int index, AuthenticatedUser currentUser) {
        return fileStorage.load(completionPhotoKey(completionId, index, currentUser));
    }

    public MediaType completionPhotoMediaType(Long completionId, int index, AuthenticatedUser currentUser) {
        return fileStorage.mediaType(completionPhotoKey(completionId, index, currentUser));
    }

    @Transactional
    public List<GroomDailyTask> generateDailyRoutineTasks(LocalDate date) {
        LocalDate targetDate = (date != null) ? date : LocalDate.now();

        List<Horse> horsesInStalls = horseRepository.findByCurrentStallIdIsNotNull();
        List<GroomDailyTask> generatedTasks = new ArrayList<>();

        for (Horse horse : horsesInStalls) {
            StableStall stall = stableStallRepository.findById(horse.getCurrentStallId())
                    .orElse(null);
            if (stall == null || stall.getGroomId() == null) {
                continue;   // chuồng chưa gán Groom -> bỏ qua
            }

            Long groomId = stall.getGroomId();

            // Duyệt enum thay vì 5 khối lặp gần giống nhau.
            // Thêm việc SOP mới = thêm một dòng trong SopSlot, không đụng file này.
            for (SopSlot slot : SopSlot.values()) {
                createTaskIfNotExist(
                        generatedTasks,
                        groomId,
                        horse.getId(),
                        slot.getTaskType(),
                        targetDate.atTime(slot.getTime()),
                        null);    // the task type and time say it all; notes are for ad-hoc tasks
            }
        }

        if (generatedTasks.isEmpty()) {
            return new ArrayList<>();
        }
        return taskRepository.saveAll(generatedTasks);
    }

    /**
     * MÀN HÌNH "TODAY CHECKLIST" — gom việc động từ 3 nguồn độc lập.
     *
     *   Nguồn 1: groom_daily_tasks        (SOP thường nhật)
     *   Nguồn 2: training_workouts JOIN training_lots  (buổi tập được gán)
     *   Nguồn 3: care_schedule (lịch thú y của ngựa trong chuồng mình)
     *
     * Lợi ích: Groom nhìn một màn hình là nắm trọn ngày — lúc nào cho ăn, lúc
     * nào phải có mặt ở sân, lúc nào Vet tới tiêm.
     */
    public List<TodayTaskItemResponse> getTodayAggregatedTasks(Long groomId,
                                                                LocalDate date,
                                                                AuthenticatedUser currentUser) {
        LocalDate targetDate = (date != null) ? date : LocalDate.now();

        // Role GROOM luôn bị ép về chính mình; Trainer/Manager xem được của người khác.
        // Giữ đúng quy ước đang dùng ở getDailyTasks().
        Long targetGroomId;
        if ("GROOM".equalsIgnoreCase(currentUser.getRole())) {
            targetGroomId = currentUser.getUserId();
        } else {
            targetGroomId = (groomId != null) ? groomId : currentUser.getUserId();
        }

        // =============================================================
        // B1 — NẠP DỮ LIỆU THÔ TỪ 3 NGUỒN
        //
        // Nạp hết trước, KHÔNG dựng item vội. Nhờ vậy bước B2 biết được
        // toàn bộ id cần tra và gom vào một lần query duy nhất.
        // =============================================================
        LocalDateTime dayStart = targetDate.atStartOfDay();
        LocalDateTime dayEnd = targetDate.atTime(23, 59, 59, 999999999);

        // Nguồn 1 — SOP thường nhật
        List<GroomDailyTask> sopTasks = taskRepository
                .findByGroomIdAndScheduledTimeBetween(targetGroomId, dayStart, dayEnd);

        // Nguồn 2 — Buổi tập, đã JOIN sẵn giờ lot, tên ngựa và tên bài tập trong MỘT câu query
        // (Source 2 — Workouts, with lot times, horse name and subject name already joined in ONE query)
        List<GroomWorkoutRow> workouts = workoutRepository.findGroomWorkoutRows(targetGroomId, targetDate);

        // Nguồn 3 — Lịch thú y của ngựa trong chuồng mình
        List<Long> stallIds = stableStallRepository.findByGroomId(targetGroomId).stream()
                .map(StableStall::getId)
                .toList();

        List<Horse> myHorses = stallIds.isEmpty() ? List.of()
                : horseRepository.findByCurrentStallIdIn(stallIds);

        List<CareSchedule> schedules = myHorses.isEmpty() ? List.of()
                : careScheduleRepository.findByHorseIdInAndScheduledAtBetweenAndStatusIn(
                        myHorses.stream().map(Horse::getId).toList(), dayStart, dayEnd,
                        List.of(com.rtms.backend.medical.enums.CareScheduleStatus.SCHEDULED,
                                com.rtms.backend.medical.enums.CareScheduleStatus.IN_PROGRESS,
                                com.rtms.backend.medical.enums.CareScheduleStatus.COMPLETED));

        // =============================================================
        // B2 — GOM ID RỒI NẠP MỘT LẦN
        //
        // Trước đây mỗi task/workout gọi findById riêng -> N+1 query.
        // Giờ số query là HẰNG SỐ, không phụ thuộc số ngựa.
        // =============================================================

        // Ngựa trong chuồng mình đã có sẵn tên từ B1 -> không cần query lại
        Map<Long, String> horseNameById = new HashMap<>();
        myHorses.forEach(h -> horseNameById.put(h.getId(), h.getName()));

        // Chỉ những con CHƯA có tên mới phải tra thêm (ví dụ Trainer nhờ Groom
        // này dắt hộ một con không thuộc chuồng của họ). Thường là rỗng. Buổi tập đã mang sẵn tên ngựa.
        // (Only horses WITHOUT a name yet need a lookup. Usually empty. Workout rows already carry the horse name.)
        Set<Long> neededHorseIds = new HashSet<>();
        sopTasks.forEach(t -> neededHorseIds.add(t.getHorseId()));
        neededHorseIds.removeAll(horseNameById.keySet());

        if (!neededHorseIds.isEmpty()) {
            horseRepository.findAllById(neededHorseIds)
                    .forEach(h -> horseNameById.put(h.getId(), h.getName()));
        }

        // =============================================================
        // B3 — DỰNG ITEM (không còn query nào nữa)
        // =============================================================
        List<TodayTaskItemResponse> items = new ArrayList<>();

        // Completions of the day's tasks, loaded once: tasks completed together share one.
        Set<Long> completionIds = new HashSet<>();
        sopTasks.forEach(t -> { if (t.getCompletionId() != null) completionIds.add(t.getCompletionId()); });
        Map<Long, GroomTaskCompletion> completionById = new HashMap<>();
        if (!completionIds.isEmpty()) {
            completionRepository.findAllById(completionIds).forEach(c -> completionById.put(c.getId(), c));
        }

        for (GroomDailyTask task : sopTasks) {
            GroomTaskCompletion completion = completionById.get(task.getCompletionId());
            items.add(new TodayTaskItemResponse(
                    TaskSource.SOP,
                    task.getId(),
                    task.getScheduledTime().toLocalTime(),
                    null,
                    task.getHorseId(),
                    nameOf(horseNameById, task.getHorseId()),
                    toVietnamese(task.getTaskType()),
                    task.getNotes(),
                    Boolean.TRUE.equals(task.getIsCompleted()) ? "COMPLETED" : "PENDING",
                    true)                         // chỉ SOP mới tick được
                    .withCompletion(task.getCompletionId(),
                            completion == null ? null : completion.getNote(),
                            completion == null ? List.of() : completionPhotoUrls(completion)));
        }

        for (GroomWorkoutRow w : workouts) {
            items.add(new TodayTaskItemResponse(
                    TaskSource.WORKOUT,
                    w.workoutId(),
                    w.startTime(),
                    w.endTime(),
                    w.horseId(),
                    w.horseName(),
                    "Training session · " + w.subjectName(),
                    "Lead the horse to the track, assist the trainer, and bring the horse back after the session",
                    w.status().name(),
                    false));                      // Trainer mới là người đóng buổi tập
        }

        for (CareSchedule s : schedules) {
            items.add(new TodayTaskItemResponse(
                    TaskSource.PREVENTIVE_CARE,
                    s.getId(),
                    s.getScheduledAt().toLocalTime(),
                    s.getScheduledAt().plusMinutes(s.getDurationMinutes()).toLocalTime(),
                    s.getHorseId(),
                    nameOf(horseNameById, s.getHorseId()),
                    "Veterinary · " + s.getCareType(),
                    s.getDescription(),
                    s.getStatus().name(),
                    false));              // Vet mới là người đóng
        }

        // =============================================================
        // SẮP XẾP TUẦN TỰ THEO GIỜ
        // =============================================================
        items.sort(Comparator.comparing(TodayTaskItemResponse::getStartTime));
        return items;
    }

    /** Tra tên từ Map đã nạp sẵn; không khớp thì hiện mã số như cũ. */
    /** API paths of a completion's photos: the endpoint that checks the viewer, never the storage key. */
    private static List<String> completionPhotoUrls(GroomTaskCompletion completion) {
        List<String> urls = new ArrayList<>();
        for (int index = 0; index < completion.getPhotoKeys().size(); index++) {
            urls.add("/api/groom-daily-tasks/completions/" + completion.getId() + "/photos/" + index);
        }
        return urls;
    }

    private String nameOf(Map<Long, String> nameById, Long id) {
        return nameById.getOrDefault(id, "#" + id);
    }


    /** Nhãn tiếng Việt cho loại việc SOP. */
    private String toVietnamese(GroomTaskType type) {
        return switch (type) {
            case FEEDING        -> "Feeding";
            case MUCKING_OUT    -> "Mucking out";
            case GROOMING       -> "Washing & grooming";
            case HOOF_CARE      -> "Hoof care";
            case HEALTH_CHECK   -> "Health check";
            case WORKOUT_ASSIST -> "Training assistance";
            case VET_ASSIST     -> "Veterinary assistance";
            case SPECIAL_CARE   -> "Special care";
        };
    }

    private void createTaskIfNotExist(List<GroomDailyTask> list, Long groomId, Long horseId,
                                      GroomTaskType taskType, LocalDateTime scheduledTime,
                                      String notes) {
        LocalDateTime checkStart = scheduledTime.minusMinutes(30);
        LocalDateTime checkEnd = scheduledTime.plusMinutes(30);
        boolean exists = taskRepository.existsByHorseIdAndTaskTypeAndScheduledTimeBetween(
                horseId, taskType, checkStart, checkEnd);
        if (!exists) {
            list.add(new GroomDailyTask(groomId, horseId, taskType, scheduledTime, notes));
        }
    }
}
