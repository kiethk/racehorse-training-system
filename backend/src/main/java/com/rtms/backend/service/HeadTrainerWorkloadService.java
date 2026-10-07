package com.rtms.backend.service;

import com.rtms.backend.entity.TrainerProfile;
import com.rtms.backend.entity.TrainerSchedule;
import com.rtms.backend.entity.User;
import com.rtms.backend.enums.TrainerScheduleStatus;
import com.rtms.backend.repository.HorseRepository;
import com.rtms.backend.repository.TrainerProfileRepository;
import com.rtms.backend.repository.TrainerScheduleRepository;
import com.rtms.backend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service tính toán workload và lựa chọn Head Trainer cho TrainerSchedule.
 *
 * Quy tắc:
 * 1. Chỉ xét User đang active.
 * 2. Role phải là HEAD_TRAINER.
 * 3. Phải có TrainerProfile hợp lệ (certificationNumber không rỗng).
 * 4. Không có TrainerSchedule trùng khung giờ (SCHEDULED hoặc IN_PROGRESS).
 * 5. Workload = distinct Horses currently managed by the Trainer (via Area)
 *               UNION
 *               distinct horse_id values in that Trainer's SCHEDULED/IN_PROGRESS trainer schedules.
 * 6. Tie-break: chọn trainerId nhỏ hơn.
 * 7. Concurrency: khóa hàng các ứng viên theo thứ tự trainerId tăng dần trước khi tính toán.
 * 8. Nếu không có Trainer hợp lệ/khả dụng: trả về Optional.empty().
 */
@Service
public class HeadTrainerWorkloadService {

    public static final Set<TrainerScheduleStatus> ACTIVE_TRAINER_SCHEDULE_STATUSES = Set.of(
            TrainerScheduleStatus.SCHEDULED,
            TrainerScheduleStatus.IN_PROGRESS
    );

    private static final int DEFAULT_DURATION_MINUTES = 30;

    private final UserRepository userRepository;
    private final TrainerProfileRepository trainerProfileRepository;
    private final HorseRepository horseRepository;
    private final TrainerScheduleRepository trainerScheduleRepository;

    public HeadTrainerWorkloadService(
            UserRepository userRepository,
            TrainerProfileRepository trainerProfileRepository,
            HorseRepository horseRepository,
            TrainerScheduleRepository trainerScheduleRepository) {
        this.userRepository = userRepository;
        this.trainerProfileRepository = trainerProfileRepository;
        this.horseRepository = horseRepository;
        this.trainerScheduleRepository = trainerScheduleRepository;
    }

    /**
     * Lựa chọn Head Trainer có tải công việc thấp nhất cho thời điểm hiện tại.
     */
    @Transactional
    public Optional<User> selectLeastLoadedHeadTrainer() {
        return selectLeastLoadedHeadTrainer(LocalDateTime.now(), DEFAULT_DURATION_MINUTES);
    }

    /**
     * Lựa chọn Head Trainer có tải công việc thấp nhất cho một khung giờ cụ thể.
     */
    @Transactional
    public Optional<User> selectLeastLoadedHeadTrainer(LocalDateTime slotStart, int durationMinutes) {
        List<User> eligibleTrainers = getEligibleHeadTrainers();
        if (eligibleTrainers.isEmpty()) {
            return Optional.empty();
        }

        List<Long> candidateIds = eligibleTrainers.stream()
                .map(User::getId)
                .sorted()
                .toList();

        // Khóa các bản ghi TrainerProfile ứng viên theo thứ tự ID để chống race condition
        List<TrainerProfile> lockedProfiles = trainerProfileRepository.findByUserIdsForUpdate(candidateIds);
        if (lockedProfiles.isEmpty()) {
            return Optional.empty();
        }

        Set<Long> lockedUserIds = lockedProfiles.stream()
                .map(TrainerProfile::getUserId)
                .collect(Collectors.toSet());

        List<User> lockedCandidates = eligibleTrainers.stream()
                .filter(trainer -> lockedUserIds.contains(trainer.getId()))
                .toList();

        LocalDateTime slotEnd = slotStart.plusMinutes(durationMinutes);

        // Lọc các Trainer không bị trùng lịch trong khung giờ
        List<User> availableTrainers = lockedCandidates.stream()
                .filter(trainer -> !hasOverlappingSchedule(trainer.getId(), slotStart, slotEnd))
                .toList();

        if (availableTrainers.isEmpty()) {
            return Optional.empty();
        }

        List<Long> availableIds = availableTrainers.stream().map(User::getId).toList();
        Map<Long, Long> workloads = calculateDistinctHorseWorkloads(availableIds);

        return availableTrainers.stream()
                .min(Comparator
                        .comparingLong((User u) -> workloads.getOrDefault(u.getId(), 0L))
                        .thenComparing(User::getId));
    }

    @Transactional
    public Optional<Long> selectLeastLoadedHeadTrainerId() {
        return selectLeastLoadedHeadTrainer().map(User::getId);
    }

    @Transactional
    public Optional<Long> selectLeastLoadedHeadTrainerId(LocalDateTime slotStart, int durationMinutes) {
        return selectLeastLoadedHeadTrainer(slotStart, durationMinutes).map(User::getId);
    }

    /**
     * Kiểm tra xem Trainer có lịch nào bị trùng khung giờ không.
     */
    public boolean hasOverlappingSchedule(Long trainerId, LocalDateTime slotStart, LocalDateTime slotEnd) {
        List<TrainerSchedule> activeSchedules = trainerScheduleRepository.findByTrainerIdAndStatusIn(
                trainerId, ACTIVE_TRAINER_SCHEDULE_STATUSES);

        for (TrainerSchedule schedule : activeSchedules) {
            LocalDateTime schedStart = schedule.getScheduledAt();
            LocalDateTime schedEnd = schedStart.plusMinutes(
                    schedule.getDurationMinutes() > 0 ? schedule.getDurationMinutes() : DEFAULT_DURATION_MINUTES);
            // Hai khoảng [A, B] và [C, D] giao nhau khi A < D && C < B
            if (schedStart.isBefore(slotEnd) && slotStart.isBefore(schedEnd)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Lấy danh sách các User là HEAD_TRAINER đang active và có TrainerProfile hợp lệ.
     */
    @Transactional(readOnly = true)
    public List<User> getEligibleHeadTrainers() {
        List<User> candidates = userRepository.findActiveHeadTrainers();
        if (candidates == null || candidates.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> candidateIds = candidates.stream()
                .map(User::getId)
                .filter(Objects::nonNull)
                .toList();

        if (candidateIds.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, TrainerProfile> profileMap;
        try {
            profileMap = trainerProfileRepository.findAllById(candidateIds).stream()
                    .filter(Objects::nonNull)
                    .collect(Collectors.toMap(TrainerProfile::getUserId, p -> p, (a, b) -> a));
        } catch (Exception e) {
            profileMap = Collections.emptyMap();
        }

        final Map<Long, TrainerProfile> finalProfileMap = profileMap;
        return candidates.stream()
                .filter(user -> {
                    TrainerProfile profile = finalProfileMap.get(user.getId());
                    if (profile == null) {
                        profile = trainerProfileRepository.findById(user.getId()).orElse(null);
                    }
                    return isEligibleHeadTrainer(user, profile);
                })
                .sorted(Comparator.comparing(User::getId))
                .toList();
    }

    /**
     * Tính toán distinct horse workload:
     * distinct Horses managed by Trainer (via Area) UNION distinct horse_id in active TrainerSchedule.
     */
    @Transactional(readOnly = true)
    public Map<Long, Long> calculateDistinctHorseWorkloads(Collection<Long> trainerIds) {
        if (trainerIds == null || trainerIds.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<Long, Set<Long>> trainerToHorses = new HashMap<>();
        for (Long trainerId : trainerIds) {
            trainerToHorses.put(trainerId, new HashSet<>());
        }

        // 1. Ngựa quản lý qua Area
        List<Object[]> areaRows = horseRepository.findManagedHorsePairs(trainerIds);
        if (areaRows != null) {
            for (Object[] row : areaRows) {
                if (row != null && row.length >= 2 && row[0] != null && row[1] != null) {
                    Long tId = ((Number) row[0]).longValue();
                    Long hId = ((Number) row[1]).longValue();
                    if (trainerToHorses.containsKey(tId)) {
                        trainerToHorses.get(tId).add(hId);
                    }
                }
            }
        }

        // 2. Ngựa trong TrainerSchedule đang active
        List<Object[]> scheduleRows = trainerScheduleRepository.findActiveTrainerHorsePairs(
                trainerIds, ACTIVE_TRAINER_SCHEDULE_STATUSES);
        if (scheduleRows != null) {
            for (Object[] row : scheduleRows) {
                if (row != null && row.length >= 2 && row[0] != null && row[1] != null) {
                    Long tId = ((Number) row[0]).longValue();
                    Long hId = ((Number) row[1]).longValue();
                    if (trainerToHorses.containsKey(tId)) {
                        trainerToHorses.get(tId).add(hId);
                    }
                }
            }
        }

        Map<Long, Long> workloads = new HashMap<>();
        for (Map.Entry<Long, Set<Long>> entry : trainerToHorses.entrySet()) {
            workloads.put(entry.getKey(), (long) entry.getValue().size());
        }
        return workloads;
    }

    /**
     * Helper cho một Trainer đơn lẻ.
     */
    @Transactional(readOnly = true)
    public long getDistinctHorseWorkload(Long trainerId) {
        if (trainerId == null) {
            return 0L;
        }
        Map<Long, Long> map = calculateDistinctHorseWorkloads(List.of(trainerId));
        return map.getOrDefault(trainerId, 0L);
    }

    public boolean isEligibleHeadTrainer(User user, TrainerProfile profile) {
        if (user == null || !user.isActive()) {
            return false;
        }
        if (user.getRole() == null || !"HEAD_TRAINER".equals(user.getRole().getName())) {
            return false;
        }
        return isValidProfile(profile);
    }

    public boolean isValidProfile(TrainerProfile profile) {
        return profile != null
                && profile.getCertificationNumber() != null
                && !profile.getCertificationNumber().trim().isEmpty();
    }
}
