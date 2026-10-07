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
 * Chọn Head Trainer ít việc nhất để giao đơn nhập học.
 *
 * Quy tắc:
 * 1. Chỉ xét User đang active, role HEAD_TRAINER, có TrainerProfile hợp lệ
 *    (certificationNumber không rỗng).
 * 2. Tải = số ngựa KHÁC NHAU gồm: ngựa đang ở trong khu Trainer phụ trách
 *    CỘNG ngựa của các đơn TRAINER_REVIEW đã giao cho Trainer đó (việc đã nhận
 *    nhưng chưa xong). Thiếu phần sau thì nhiều đơn hoàn tất cùng lúc sẽ dồn
 *    hết về người đang có 0 ngựa trong khu.
 * 3. Hòa thì chọn trainerId nhỏ hơn — kết quả tất định, test lặp lại được.
 * 4. Khóa hàng TrainerProfile của các ứng viên theo thứ tự id trước khi tính,
 *    để hai lượt gán chạy song song không cùng đọc một mức tải rồi cùng chọn
 *    một người.
 *
 * Không có khung giờ: đánh giá ngựa trong khu cách ly không phải cuộc hẹn có
 * giờ cố định, Trainer xem đơn lúc nào cũng được.
 */
@Service
public class HeadTrainerWorkloadService {

    private final UserRepository userRepository;
    private final TrainerProfileRepository trainerProfileRepository;
    private final HorseRepository horseRepository;
    private final AdmissionApplicationRepository admissionRepository;

    public HeadTrainerWorkloadService(
            UserRepository userRepository,
            TrainerProfileRepository trainerProfileRepository,
            HorseRepository horseRepository,
            AdmissionApplicationRepository admissionRepository) {
        this.userRepository = userRepository;
        this.trainerProfileRepository = trainerProfileRepository;
        this.horseRepository = horseRepository;
        this.admissionRepository = admissionRepository;
    }

    /**
     * Phải chạy trong transaction (khóa FOR UPDATE trên TrainerProfile giữ tới
     * khi transaction của lượt gán commit).
     */
    @Transactional
    public Optional<User> selectLeastLoadedHeadTrainer() {
        List<User> eligibleTrainers = getEligibleHeadTrainers();
        if (eligibleTrainers.isEmpty()) {
            return Optional.empty();
        }

        List<Long> candidateIds = eligibleTrainers.stream()
                .map(User::getId)
                .sorted()
                .toList();

        Set<Long> lockedUserIds = trainerProfileRepository.findByUserIdsForUpdate(candidateIds).stream()
                .map(TrainerProfile::getUserId)
                .collect(Collectors.toSet());
        if (lockedUserIds.isEmpty()) {
            return Optional.empty();
        }

        List<User> lockedCandidates = eligibleTrainers.stream()
                .filter(trainer -> lockedUserIds.contains(trainer.getId()))
                .toList();

        Map<Long, Long> workloads = calculateDistinctHorseWorkloads(
                lockedCandidates.stream().map(User::getId).toList());

        return lockedCandidates.stream()
                .min(Comparator
                        .comparingLong((User u) -> workloads.getOrDefault(u.getId(), 0L))
                        .thenComparing(User::getId));
    }

    /** HEAD_TRAINER đang active và có TrainerProfile hợp lệ, xếp theo id. */
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

        Map<Long, TrainerProfile> profiles = trainerProfileRepository.findAllById(candidateIds).stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(TrainerProfile::getUserId, p -> p, (a, b) -> a));

        return candidates.stream()
                .filter(user -> isEligibleHeadTrainer(user, profiles.get(user.getId())))
                .sorted(Comparator.comparing(User::getId))
                .toList();
    }

    /** Số ngựa khác nhau mỗi Trainer đang lo: ngựa trong khu ∪ ngựa của đơn đang chờ họ duyệt. */
    @Transactional(readOnly = true)
    public Map<Long, Long> calculateDistinctHorseWorkloads(Collection<Long> trainerIds) {
        if (trainerIds == null || trainerIds.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<Long, Set<Long>> horsesByTrainer = new HashMap<>();
        trainerIds.forEach(id -> horsesByTrainer.put(id, new HashSet<>()));

        collectPairs(horseRepository.findManagedHorsePairs(trainerIds), horsesByTrainer);
        collectPairs(admissionRepository.findAssignedHorsePairs(trainerIds, AdmissionStatus.TRAINER_REVIEW),
                horsesByTrainer);

        Map<Long, Long> workloads = new HashMap<>();
        horsesByTrainer.forEach((trainerId, horses) -> workloads.put(trainerId, (long) horses.size()));
        return workloads;
    }

    private static void collectPairs(List<Object[]> rows, Map<Long, Set<Long>> horsesByTrainer) {
        if (rows == null) {
            return;
        }
        for (Object[] row : rows) {
            if (row == null || row.length < 2 || row[0] == null || row[1] == null) {
                continue;
            }
            Set<Long> horses = horsesByTrainer.get(((Number) row[0]).longValue());
            if (horses != null) {
                horses.add(((Number) row[1]).longValue());
            }
        }
    }

    public boolean isEligibleHeadTrainer(User user, TrainerProfile profile) {
        if (user == null || !user.isActive()) {
            return false;
        }
        if (user.getRole() == null || !"HEAD_TRAINER".equals(user.getRole().getName())) {
            return false;
        }
        return profile != null
                && profile.getCertificationNumber() != null
                && !profile.getCertificationNumber().trim().isEmpty();
    }
}
