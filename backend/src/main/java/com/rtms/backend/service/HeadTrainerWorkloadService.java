package com.rtms.backend.service;

import com.rtms.backend.entity.TrainerProfile;
import com.rtms.backend.entity.User;
import com.rtms.backend.enums.AdmissionStatus;
import com.rtms.backend.repository.AdmissionApplicationRepository;
import com.rtms.backend.repository.HorseRepository;
import com.rtms.backend.repository.TrainerProfileRepository;
import com.rtms.backend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service tính toán workload và lựa chọn Head Trainer có tải công việc thấp nhất.
 *
 * Quy tắc:
 * 1. Chỉ xét User đang active.
 * 2. Role phải là HEAD_TRAINER.
 * 3. Phải có TrainerProfile hợp lệ (certificationNumber không rỗng).
 * 4. Workload = (Số Horse trong các Area do Trainer phụ trách) + (Số Admission đã gán trainerId chưa terminal).
 * 5. Trạng thái terminal: APPROVED hoặc REJECTED.
 * 6. Tie-break: chọn trainerId nhỏ hơn.
 * 7. Nếu không có Trainer hợp lệ: trả về Optional.empty().
 */
@Service
public class HeadTrainerWorkloadService {

    public static final Set<AdmissionStatus> TERMINAL_ADMISSION_STATUSES = Set.of(
            AdmissionStatus.APPROVED,
            AdmissionStatus.REJECTED
    );

    private final UserRepository userRepository;
    private final TrainerProfileRepository trainerProfileRepository;
    private final HorseRepository horseRepository;
    private final AdmissionApplicationRepository admissionApplicationRepository;

    public HeadTrainerWorkloadService(
            UserRepository userRepository,
            TrainerProfileRepository trainerProfileRepository,
            HorseRepository horseRepository,
            AdmissionApplicationRepository admissionApplicationRepository) {
        this.userRepository = userRepository;
        this.trainerProfileRepository = trainerProfileRepository;
        this.horseRepository = horseRepository;
        this.admissionApplicationRepository = admissionApplicationRepository;
    }

    /**
     * Lựa chọn Head Trainer có tổng workload thấp nhất.
     *
     * @return Optional chứa User Head Trainer được chọn, hoặc Optional.empty() nếu không có ai hợp lệ.
     */
    @Transactional(readOnly = true)
    public Optional<User> selectLeastLoadedHeadTrainer() {
        List<User> eligibleTrainers = getEligibleHeadTrainers();
        if (eligibleTrainers.isEmpty()) {
            return Optional.empty();
        }

        List<Long> trainerIds = eligibleTrainers.stream()
                .map(User::getId)
                .toList();

        Map<Long, Long> workloads = calculateWorkloads(trainerIds);

        return eligibleTrainers.stream()
                .min(Comparator
                        .comparingLong((User u) -> workloads.getOrDefault(u.getId(), 0L))
                        .thenComparing(User::getId));
    }

    /**
     * Tiện ích trả về trainerId của Head Trainer có workload thấp nhất.
     */
    @Transactional(readOnly = true)
    public Optional<Long> selectLeastLoadedHeadTrainerId() {
        return selectLeastLoadedHeadTrainer().map(User::getId);
    }

    /**
     * Lấy danh sách các User là HEAD_TRAINER đang active và có TrainerProfile hợp lệ.
     * Truy vấn hàng loạt tránh N+1.
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
     * Tính toán bảng workload cho danh sách trainerIds:
     * workload = horseCount + activeAdmissionCount.
     * Sử dụng 2 câu query tổng hợp tránh N+1.
     */
    @Transactional(readOnly = true)
    public Map<Long, Long> calculateWorkloads(Collection<Long> trainerIds) {
        if (trainerIds == null || trainerIds.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<Long, Long> horseCounts = parseCountResults(horseRepository.countHorsesByTrainerIds(trainerIds));
        Map<Long, Long> admissionCounts = parseCountResults(
                admissionApplicationRepository.countActiveAdmissionsByTrainerIds(trainerIds, TERMINAL_ADMISSION_STATUSES));

        Map<Long, Long> workloads = new HashMap<>();
        for (Long trainerId : trainerIds) {
            long horses = horseCounts.getOrDefault(trainerId, 0L);
            long admissions = admissionCounts.getOrDefault(trainerId, 0L);
            workloads.put(trainerId, horses + admissions);
        }
        return workloads;
    }

    /**
     * Lấy riêng số lượng ngựa hiện tại do Trainer quản lý.
     */
    @Transactional(readOnly = true)
    public long getHorseWorkload(Long trainerId) {
        if (trainerId == null) {
            return 0L;
        }
        Map<Long, Long> counts = parseCountResults(horseRepository.countHorsesByTrainerIds(List.of(trainerId)));
        return counts.getOrDefault(trainerId, 0L);
    }

    /**
     * Lấy riêng số lượng đơn nhập học active (chưa terminal) đã gán cho Trainer.
     */
    @Transactional(readOnly = true)
    public long getAdmissionWorkload(Long trainerId) {
        if (trainerId == null) {
            return 0L;
        }
        Map<Long, Long> counts = parseCountResults(
                admissionApplicationRepository.countActiveAdmissionsByTrainerIds(List.of(trainerId), TERMINAL_ADMISSION_STATUSES));
        return counts.getOrDefault(trainerId, 0L);
    }

    /**
     * Kiểm tra tính hợp lệ của User ứng viên Head Trainer.
     */
    public boolean isEligibleHeadTrainer(User user, TrainerProfile profile) {
        if (user == null || !user.isActive()) {
            return false;
        }
        if (user.getRole() == null || !"HEAD_TRAINER".equals(user.getRole().getName())) {
            return false;
        }
        return isValidProfile(profile);
    }

    /**
     * Kiểm tra tính hợp lệ của TrainerProfile.
     */
    public boolean isValidProfile(TrainerProfile profile) {
        return profile != null
                && profile.getCertificationNumber() != null
                && !profile.getCertificationNumber().trim().isEmpty();
    }

    private Map<Long, Long> parseCountResults(List<Object[]> results) {
        if (results == null || results.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, Long> map = new HashMap<>();
        for (Object[] row : results) {
            if (row != null && row.length >= 2 && row[0] != null && row[1] != null) {
                Long id = ((Number) row[0]).longValue();
                Long count = ((Number) row[1]).longValue();
                map.put(id, count);
            }
        }
        return map;
    }
}
