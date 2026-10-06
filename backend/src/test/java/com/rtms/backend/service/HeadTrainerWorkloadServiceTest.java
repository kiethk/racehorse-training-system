package com.rtms.backend.service;

import com.rtms.backend.entity.Role;
import com.rtms.backend.entity.TrainerProfile;
import com.rtms.backend.entity.User;
import com.rtms.backend.enums.AdmissionStatus;
import com.rtms.backend.enums.CareScheduleStatus;
import com.rtms.backend.repository.AdmissionApplicationRepository;
import com.rtms.backend.repository.CareScheduleRepository;
import com.rtms.backend.repository.HorseRepository;
import com.rtms.backend.repository.TrainerProfileRepository;
import com.rtms.backend.repository.UserRepository;
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
    private AdmissionApplicationRepository admissionApplicationRepository;

    @Mock
    private CareScheduleRepository careScheduleRepository;

    private HeadTrainerWorkloadService service;

    @BeforeEach
    void setUp() {
        service = new HeadTrainerWorkloadService(
                userRepository,
                trainerProfileRepository,
                horseRepository,
                admissionApplicationRepository,
                careScheduleRepository
        );
        lenient().when(careScheduleRepository.countActiveCareSchedulesByTrainerIds(anyCollection(), anyCollection()))
                .thenReturn(Collections.emptyList());
    }

    private List<Object[]> rows(Object[]... items) {
        return Arrays.asList(items);
    }

    @Test
    @DisplayName("1. Chọn Trainer có workload thấp nhất (ngựa + đơn active)")
    void selectLeastLoadedHeadTrainer_selectsTrainerWithLowestWorkload() {
        User trainer1 = createHeadTrainer(1L, "Trainer 1", true);
        User trainer2 = createHeadTrainer(2L, "Trainer 2", true);
        User trainer3 = createHeadTrainer(3L, "Trainer 3", true);

        when(userRepository.findActiveHeadTrainers()).thenReturn(List.of(trainer1, trainer2, trainer3));
        mockValidProfiles(trainer1, trainer2, trainer3);

        // Trainer 1: 5 horses, 1 admission = 6
        // Trainer 2: 1 horse, 1 admission = 2  <- lowest
        // Trainer 3: 2 horses, 3 admissions = 5
        when(horseRepository.countHorsesByTrainerIds(anyCollection())).thenReturn(rows(
                new Object[]{1L, 5L},
                new Object[]{2L, 1L},
                new Object[]{3L, 2L}
        ));
        when(admissionApplicationRepository.countActiveAdmissionsByTrainerIds(anyCollection(), anyCollection())).thenReturn(rows(
                new Object[]{1L, 1L},
                new Object[]{2L, 1L},
                new Object[]{3L, 3L}
        ));

        Optional<User> selected = service.selectLeastLoadedHeadTrainer();

        assertTrue(selected.isPresent());
        assertEquals(2L, selected.get().getId());
        assertEquals("Trainer 2", selected.get().getFullName());
    }

    @Test
    @DisplayName("2. Hai Trainer bằng workload thì chọn trainerId nhỏ hơn (tie-break)")
    void selectLeastLoadedHeadTrainer_tieBreakSmallerTrainerId() {
        User trainerA = createHeadTrainer(10L, "Trainer A", true);
        User trainerB = createHeadTrainer(5L, "Trainer B", true);
        User trainerC = createHeadTrainer(20L, "Trainer C", true);

        when(userRepository.findActiveHeadTrainers()).thenReturn(List.of(trainerA, trainerB, trainerC));
        mockValidProfiles(trainerA, trainerB, trainerC);

        // Both trainerA (10) and trainerB (5) have total workload = 3
        // trainerC (20) has workload = 8
        when(horseRepository.countHorsesByTrainerIds(anyCollection())).thenReturn(rows(
                new Object[]{10L, 2L},
                new Object[]{5L, 3L},
                new Object[]{20L, 5L}
        ));
        when(admissionApplicationRepository.countActiveAdmissionsByTrainerIds(anyCollection(), anyCollection())).thenReturn(rows(
                new Object[]{10L, 1L},
                new Object[]{5L, 0L},
                new Object[]{20L, 3L}
        ));

        Optional<User> selected = service.selectLeastLoadedHeadTrainer();

        assertTrue(selected.isPresent());
        // Both have workload 3, 5L < 10L -> selects trainerB (5L)
        assertEquals(5L, selected.get().getId());
        assertEquals("Trainer B", selected.get().getFullName());
    }

    @Test
    @DisplayName("3. Bỏ qua User inactive")
    void selectLeastLoadedHeadTrainer_ignoresInactiveUser() {
        User activeTrainer = createHeadTrainer(1L, "Active Trainer", true);
        User inactiveTrainer = createHeadTrainer(2L, "Inactive Trainer", false);

        when(userRepository.findActiveHeadTrainers()).thenReturn(List.of(activeTrainer, inactiveTrainer));
        mockValidProfiles(activeTrainer, inactiveTrainer);

        // Inactive trainer has 0 workload, but should be filtered out
        when(horseRepository.countHorsesByTrainerIds(anyCollection())).thenReturn(rows(
                new Object[]{1L, 4L}
        ));
        when(admissionApplicationRepository.countActiveAdmissionsByTrainerIds(anyCollection(), anyCollection())).thenReturn(rows(
                new Object[]{1L, 2L}
        ));

        Optional<User> selected = service.selectLeastLoadedHeadTrainer();

        assertTrue(selected.isPresent());
        assertEquals(1L, selected.get().getId());
    }

    @Test
    @DisplayName("4. Bỏ qua User không có role HEAD_TRAINER")
    void selectLeastLoadedHeadTrainer_ignoresNonHeadTrainerRole() {
        User headTrainer = createHeadTrainer(1L, "Head Trainer", true);

        User regularTrainer = createHeadTrainer(2L, "Regular Trainer", true);
        Role trainerRole = new Role();
        trainerRole.setName("TRAINER");
        regularTrainer.setRole(trainerRole);

        User groomUser = createHeadTrainer(3L, "Groom User", true);
        Role groomRole = new Role();
        groomRole.setName("GROOM");
        groomUser.setRole(groomRole);

        when(userRepository.findActiveHeadTrainers()).thenReturn(List.of(headTrainer, regularTrainer, groomUser));
        mockValidProfiles(headTrainer, regularTrainer, groomUser);

        when(horseRepository.countHorsesByTrainerIds(anyCollection())).thenReturn(rows(
                new Object[]{1L, 10L}
        ));
        when(admissionApplicationRepository.countActiveAdmissionsByTrainerIds(anyCollection(), anyCollection())).thenReturn(rows(
                new Object[]{1L, 2L}
        ));

        Optional<User> selected = service.selectLeastLoadedHeadTrainer();

        assertTrue(selected.isPresent());
        assertEquals(1L, selected.get().getId());
    }

    @Test
    @DisplayName("5. Bỏ qua TrainerProfile không hợp lệ (null, thiếu certification number hoặc trống)")
    void selectLeastLoadedHeadTrainer_ignoresInvalidTrainerProfile() {
        User validTrainer = createHeadTrainer(1L, "Valid Trainer", true);
        User nullProfileTrainer = createHeadTrainer(2L, "Null Profile Trainer", true);
        User blankCertTrainer = createHeadTrainer(3L, "Blank Cert Trainer", true);
        User emptyCertTrainer = createHeadTrainer(4L, "Empty Cert Trainer", true);

        when(userRepository.findActiveHeadTrainers()).thenReturn(
                List.of(validTrainer, nullProfileTrainer, blankCertTrainer, emptyCertTrainer)
        );

        TrainerProfile validProfile = new TrainerProfile();
        validProfile.setUserId(1L);
        validProfile.setCertificationNumber("HT-CERT-001");

        TrainerProfile blankCertProfile = new TrainerProfile();
        blankCertProfile.setUserId(3L);
        blankCertProfile.setCertificationNumber("   ");

        TrainerProfile emptyCertProfile = new TrainerProfile();
        emptyCertProfile.setUserId(4L);
        emptyCertProfile.setCertificationNumber("");

        when(trainerProfileRepository.findAllById(anyCollection())).thenReturn(
                List.of(validProfile, blankCertProfile, emptyCertProfile)
        );

        when(horseRepository.countHorsesByTrainerIds(anyCollection())).thenReturn(rows(
                new Object[]{1L, 5L}
        ));
        when(admissionApplicationRepository.countActiveAdmissionsByTrainerIds(anyCollection(), anyCollection())).thenReturn(rows(
                new Object[]{1L, 2L}
        ));

        Optional<User> selected = service.selectLeastLoadedHeadTrainer();

        assertTrue(selected.isPresent());
        assertEquals(1L, selected.get().getId());
    }

    @Test
    @DisplayName("6. Không có Trainer hợp lệ thì trả về empty/null")
    void selectLeastLoadedHeadTrainer_noEligibleTrainers_returnsEmpty() {
        when(userRepository.findActiveHeadTrainers()).thenReturn(Collections.emptyList());

        Optional<User> selected = service.selectLeastLoadedHeadTrainer();

        assertTrue(selected.isEmpty());
        verifyNoInteractions(horseRepository, admissionApplicationRepository);
    }

    @Test
    @DisplayName("6b. Danh sách có User nhưng không ai đủ điều kiện -> trả về empty")
    void selectLeastLoadedHeadTrainer_allCandidatesIneligible_returnsEmpty() {
        User inactive = createHeadTrainer(1L, "Inactive", false);
        when(userRepository.findActiveHeadTrainers()).thenReturn(List.of(inactive));
        when(trainerProfileRepository.findAllById(anyCollection())).thenReturn(Collections.emptyList());

        Optional<User> selected = service.selectLeastLoadedHeadTrainer();

        assertTrue(selected.isEmpty());
        verifyNoInteractions(horseRepository, admissionApplicationRepository);
    }

    @Test
    @DisplayName("7. APPROVED và REJECTED không được tính vào Admission workload")
    void calculateWorkloads_excludesApprovedAndRejectedAdmissions() {
        // Verify that the query passes terminal statuses (APPROVED, REJECTED) to be excluded
        when(horseRepository.countHorsesByTrainerIds(anyCollection())).thenReturn(rows(
                new Object[]{1L, 2L}
        ));
        // Only non-terminal admissions are returned by the query
        when(admissionApplicationRepository.countActiveAdmissionsByTrainerIds(
                eq(List.of(1L)),
                argThat(statuses -> statuses.contains(AdmissionStatus.APPROVED) && statuses.contains(AdmissionStatus.REJECTED))
        )).thenReturn(rows(
                new Object[]{1L, 3L}
        ));

        Map<Long, Long> workloads = service.calculateWorkloads(List.of(1L));

        assertEquals(1, workloads.size());
        // 2 horses + 3 active admissions = 5
        assertEquals(5L, workloads.get(1L));

        // Verify terminal statuses are specifically excluded in query call
        verify(admissionApplicationRepository).countActiveAdmissionsByTrainerIds(
                eq(List.of(1L)),
                eq(HeadTrainerWorkloadService.TERMINAL_ADMISSION_STATUSES)
        );
    }

    @Test
    @DisplayName("8. Trainer có 0 ngựa và 0 admission tính đúng workload = 0")
    void calculateWorkloads_zeroWorkloadHandledGracefully() {
        User trainer = createHeadTrainer(1L, "Trainer 1", true);
        when(userRepository.findActiveHeadTrainers()).thenReturn(List.of(trainer));
        mockValidProfiles(trainer);

        when(horseRepository.countHorsesByTrainerIds(anyCollection())).thenReturn(Collections.emptyList());
        when(admissionApplicationRepository.countActiveAdmissionsByTrainerIds(anyCollection(), anyCollection())).thenReturn(Collections.emptyList());

        Optional<Long> selectedId = service.selectLeastLoadedHeadTrainerId();

        assertTrue(selectedId.isPresent());
        assertEquals(1L, selectedId.get());
        assertEquals(0L, service.getHorseWorkload(1L));
        assertEquals(0L, service.getAdmissionWorkload(1L));
        assertEquals(0L, service.getCareScheduleWorkload(1L));
    }

    @Test
    @DisplayName("9. CareSchedule active (SCHEDULED, IN_PROGRESS) được tính vào Trainer workload")
    void calculateWorkloads_includesActiveCareSchedules() {
        when(horseRepository.countHorsesByTrainerIds(anyCollection())).thenReturn(rows(
                new Object[]{1L, 2L}
        ));
        when(admissionApplicationRepository.countActiveAdmissionsByTrainerIds(anyCollection(), anyCollection())).thenReturn(rows(
                new Object[]{1L, 1L}
        ));
        when(careScheduleRepository.countActiveCareSchedulesByTrainerIds(
                eq(List.of(1L)),
                eq(HeadTrainerWorkloadService.ACTIVE_CARE_SCHEDULE_STATUSES)
        )).thenReturn(rows(
                new Object[]{1L, 4L}
        ));

        Map<Long, Long> workloads = service.calculateWorkloads(List.of(1L));

        assertEquals(1, workloads.size());
        // 2 horses + 1 active admission + 4 active care schedules = 7
        assertEquals(7L, workloads.get(1L));
    }

    @Test
    @DisplayName("10. getCareScheduleWorkload trả về đúng số lượng")
    void getCareScheduleWorkload_returnsCount() {
        when(careScheduleRepository.countActiveCareSchedulesByTrainerIds(
                eq(List.of(1L)),
                eq(HeadTrainerWorkloadService.ACTIVE_CARE_SCHEDULE_STATUSES)
        )).thenReturn(rows(
                new Object[]{1L, 3L}
        ));

        assertEquals(3L, service.getCareScheduleWorkload(1L));
        assertEquals(0L, service.getCareScheduleWorkload(null));
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
    }
}
