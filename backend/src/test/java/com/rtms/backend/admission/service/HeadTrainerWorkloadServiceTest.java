package com.rtms.backend.admission.service;

import com.rtms.backend.identity.entity.Role;
import com.rtms.backend.identity.entity.TrainerProfile;
import com.rtms.backend.identity.entity.User;
import com.rtms.backend.admission.enums.AdmissionStatus;
import com.rtms.backend.admission.repository.AdmissionApplicationRepository;
import com.rtms.backend.horse.repository.HorseRepository;
import com.rtms.backend.identity.repository.TrainerProfileRepository;
import com.rtms.backend.identity.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HeadTrainerWorkloadServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private TrainerProfileRepository trainerProfileRepository;

    @Mock
    private HorseRepository horseRepository;

    @Mock
    private AdmissionApplicationRepository admissionRepository;

    private HeadTrainerWorkloadService service;

    @BeforeEach
    void setUp() {
        service = new HeadTrainerWorkloadService(
                userRepository,
                trainerProfileRepository,
                horseRepository,
                admissionRepository
        );
    }

    private List<Object[]> rows(Object[]... items) {
        return Arrays.asList(items);
    }

    @Test
    @DisplayName("1. Chọn Trainer có ít ngựa khác nhau nhất (ngựa trong khu ∪ ngựa của đơn TRAINER_REVIEW đã giao)")
    void selectLeastLoadedHeadTrainer_selectsLowestDistinctHorseWorkload() {
        User trainer1 = createHeadTrainer(1L, "Trainer 1", true);
        User trainer2 = createHeadTrainer(2L, "Trainer 2", true);
        User trainer3 = createHeadTrainer(3L, "Trainer 3", true);

        when(userRepository.findActiveHeadTrainers()).thenReturn(List.of(trainer1, trainer2, trainer3));
        mockValidProfiles(trainer1, trainer2, trainer3);

        // Trainer 1: khu có ngựa 10..13 (4), đơn chờ duyệt ngựa 14 -> 5
        // Trainer 2: khu có ngựa 10, đơn chờ duyệt cũng là ngựa 10 -> 1 (thấp nhất)
        // Trainer 3: khu có ngựa 20, 21, đơn chờ duyệt ngựa 22 -> 3
        when(horseRepository.findManagedHorsePairs(anyCollection())).thenReturn(rows(
                new Object[]{1L, 10L},
                new Object[]{1L, 11L},
                new Object[]{1L, 12L},
                new Object[]{1L, 13L},
                new Object[]{2L, 10L},
                new Object[]{3L, 20L},
                new Object[]{3L, 21L}
        ));
        when(admissionRepository.findAssignedHorsePairs(anyCollection(), eq(AdmissionStatus.TRAINER_REVIEW)))
                .thenReturn(rows(
                        new Object[]{1L, 14L},
                        new Object[]{2L, 10L}, // trùng ngựa trong khu -> chỉ đếm 1 lần
                        new Object[]{3L, 22L}
                ));

        Optional<User> selected = service.selectLeastLoadedHeadTrainer();

        assertTrue(selected.isPresent());
        assertEquals(2L, selected.get().getId());
    }

    @Test
    @DisplayName("2. Đơn đang chờ duyệt cũng tính vào tải: Trainer khu trống nhưng đã nhận 2 đơn thì không được ưu tiên")
    void selectLeastLoadedHeadTrainer_countsPendingAdmissions() {
        User trainer1 = createHeadTrainer(1L, "Trainer 1", true);
        User trainer2 = createHeadTrainer(2L, "Trainer 2", true);

        when(userRepository.findActiveHeadTrainers()).thenReturn(List.of(trainer1, trainer2));
        mockValidProfiles(trainer1, trainer2);

        // Trainer 1: khu trống, nhưng đang giữ 2 đơn chờ duyệt -> 2
        // Trainer 2: khu có 1 ngựa -> 1
        when(horseRepository.findManagedHorsePairs(anyCollection())).thenReturn(rows(
                new Object[]{2L, 50L}
        ));
        when(admissionRepository.findAssignedHorsePairs(anyCollection(), eq(AdmissionStatus.TRAINER_REVIEW)))
                .thenReturn(rows(
                        new Object[]{1L, 60L},
                        new Object[]{1L, 61L}
                ));

        Optional<User> selected = service.selectLeastLoadedHeadTrainer();

        assertTrue(selected.isPresent());
        assertEquals(2L, selected.get().getId());
    }

    @Test
    @DisplayName("3. Hai Trainer bằng tải thì chọn trainerId nhỏ hơn (tie-break)")
    void selectLeastLoadedHeadTrainer_tieBreakSmallerTrainerId() {
        User trainerA = createHeadTrainer(10L, "Trainer A", true);
        User trainerB = createHeadTrainer(5L, "Trainer B", true);
        User trainerC = createHeadTrainer(20L, "Trainer C", true);

        when(userRepository.findActiveHeadTrainers()).thenReturn(List.of(trainerA, trainerB, trainerC));
        mockValidProfiles(trainerA, trainerB, trainerC);

        when(horseRepository.findManagedHorsePairs(anyCollection())).thenReturn(rows(
                new Object[]{10L, 100L},
                new Object[]{5L, 101L},
                new Object[]{20L, 102L}
        ));
        when(admissionRepository.findAssignedHorsePairs(anyCollection(), eq(AdmissionStatus.TRAINER_REVIEW)))
                .thenReturn(Collections.emptyList());

        Optional<User> selected = service.selectLeastLoadedHeadTrainer();

        assertTrue(selected.isPresent());
        assertEquals(5L, selected.get().getId(), "Trainer 5 có id nhỏ nhất phải thắng tie-break");
    }

    @Test
    @DisplayName("4. Khóa TrainerProfile của ứng viên theo thứ tự id trước khi tính tải")
    void selectLeastLoadedHeadTrainer_locksProfilesInIdOrder() {
        User trainerA = createHeadTrainer(9L, "Trainer A", true);
        User trainerB = createHeadTrainer(3L, "Trainer B", true);

        when(userRepository.findActiveHeadTrainers()).thenReturn(List.of(trainerA, trainerB));
        mockValidProfiles(trainerA, trainerB);
        when(horseRepository.findManagedHorsePairs(anyCollection())).thenReturn(Collections.emptyList());
        when(admissionRepository.findAssignedHorsePairs(anyCollection(), any()))
                .thenReturn(Collections.emptyList());

        service.selectLeastLoadedHeadTrainer();

        verify(trainerProfileRepository).findByUserIdsForUpdate(List.of(3L, 9L));
    }

    @Test
    @DisplayName("5. Không có Head Trainer active -> Optional.empty() (đơn để trống trainerId, chờ gán lại)")
    void selectLeastLoadedHeadTrainer_noActiveTrainers_returnsEmpty() {
        when(userRepository.findActiveHeadTrainers()).thenReturn(Collections.emptyList());

        Optional<User> selected = service.selectLeastLoadedHeadTrainer();

        assertTrue(selected.isEmpty());
        verifyNoInteractions(admissionRepository);
    }

    @Test
    @DisplayName("6. Head Trainer không có TrainerProfile -> bị bỏ qua")
    void selectLeastLoadedHeadTrainer_skipsTrainerWithoutProfile() {
        User trainerWithProfile = createHeadTrainer(1L, "Trainer Valid", true);
        User trainerWithoutProfile = createHeadTrainer(2L, "Trainer Missing Profile", true);

        when(userRepository.findActiveHeadTrainers()).thenReturn(List.of(trainerWithProfile, trainerWithoutProfile));
        mockValidProfiles(trainerWithProfile);
        when(horseRepository.findManagedHorsePairs(anyCollection())).thenReturn(Collections.emptyList());
        when(admissionRepository.findAssignedHorsePairs(anyCollection(), any()))
                .thenReturn(Collections.emptyList());

        Optional<User> selected = service.selectLeastLoadedHeadTrainer();

        assertTrue(selected.isPresent());
        assertEquals(1L, selected.get().getId());
    }

    @Test
    @DisplayName("7. calculateDistinctHorseWorkloads tính đúng hợp tập ngựa riêng biệt")
    void calculateDistinctHorseWorkloads_returnsDistinctUnionCount() {
        when(horseRepository.findManagedHorsePairs(List.of(1L))).thenReturn(rows(
                new Object[]{1L, 10L},
                new Object[]{1L, 20L}
        ));
        when(admissionRepository.findAssignedHorsePairs(List.of(1L), AdmissionStatus.TRAINER_REVIEW))
                .thenReturn(rows(
                        new Object[]{1L, 20L},
                        new Object[]{1L, 30L}
                ));

        Map<Long, Long> workloads = service.calculateDistinctHorseWorkloads(List.of(1L));

        // Hợp: {10, 20, 30} -> 3
        assertEquals(3L, workloads.get(1L));
    }

    @Test
    @DisplayName("8. calculateDistinctHorseWorkloads với danh sách rỗng trả về map rỗng")
    void calculateDistinctHorseWorkloads_empty_returnsEmptyMap() {
        assertTrue(service.calculateDistinctHorseWorkloads(List.of()).isEmpty());
        verifyNoInteractions(horseRepository, admissionRepository);
    }

    private User createHeadTrainer(Long id, String name, boolean active) {
        Role role = new Role();
        role.setName("HEAD_TRAINER");

        User user = new User();
        user.setId(id);
        user.setFullName(name);
        user.setEmail(name.toLowerCase().replace(" ", "") + "@rtms.com");
        user.setActive(active);
        user.setRole(role);
        return user;
    }

    private void mockValidProfiles(User... users) {
        List<TrainerProfile> profiles = new ArrayList<>();
        for (User user : users) {
            TrainerProfile profile = new TrainerProfile();
            profile.setUserId(user.getId());
            profile.setCertificationNumber("CERT-" + user.getId());
            profiles.add(profile);
        }
        when(trainerProfileRepository.findAllById(anyCollection())).thenReturn(profiles);
        when(trainerProfileRepository.findByUserIdsForUpdate(anyCollection())).thenReturn(profiles);
    }
}
