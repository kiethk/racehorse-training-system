package com.rtms.backend.service;

import com.rtms.backend.entity.Role;
import com.rtms.backend.entity.TrainerProfile;
import com.rtms.backend.entity.User;
import com.rtms.backend.repository.HorseRepository;
import com.rtms.backend.repository.TrainerProfileRepository;
import com.rtms.backend.repository.TrainerScheduleRepository;
import com.rtms.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
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
    private TrainerScheduleRepository trainerScheduleRepository;

    private HeadTrainerWorkloadService service;

    @BeforeEach
    void setUp() {
        service = new HeadTrainerWorkloadService(
                userRepository,
                trainerProfileRepository,
                horseRepository,
                trainerScheduleRepository
        );
    }

    private List<Object[]> rows(Object[]... items) {
        return Arrays.asList(items);
    }

    @Test
    @DisplayName("1. Chọn Trainer có workload ngựa riêng biệt thấp nhất (distinct union giữa Area horses và TrainerSchedule horses)")
    void selectLeastLoadedHeadTrainer_selectsLowestDistinctHorseWorkload() {
        User trainer1 = createHeadTrainer(1L, "Trainer 1", true);
        User trainer2 = createHeadTrainer(2L, "Trainer 2", true);
        User trainer3 = createHeadTrainer(3L, "Trainer 3", true);

        when(userRepository.findActiveHeadTrainers()).thenReturn(List.of(trainer1, trainer2, trainer3));
        mockValidProfiles(trainer1, trainer2, trainer3);
        when(trainerScheduleRepository.findByTrainerIdAndStatusIn(anyLong(), anyCollection()))
                .thenReturn(Collections.emptyList());

        // Trainer 1: manages horse 10, 11, 12, 13 (4 horses), active schedule for horse 14 -> distinct union = 5
        // Trainer 2: manages horse 10, active schedule for horse 10 -> distinct union = 1 (lowest!)
        // Trainer 3: manages horse 20, 21, active schedule for horse 22 -> distinct union = 3
        when(horseRepository.findManagedHorsePairs(anyCollection())).thenReturn(rows(
                new Object[]{1L, 10L},
                new Object[]{1L, 11L},
                new Object[]{1L, 12L},
                new Object[]{1L, 13L},
                new Object[]{2L, 10L},
                new Object[]{3L, 20L},
                new Object[]{3L, 21L}
        ));
        when(trainerScheduleRepository.findActiveTrainerHorsePairs(anyCollection(), anyCollection())).thenReturn(rows(
                new Object[]{1L, 14L},
                new Object[]{2L, 10L}, // Same horse as managed horse -> distinct count must be 1, not 2
                new Object[]{3L, 22L}
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
        when(trainerScheduleRepository.findByTrainerIdAndStatusIn(anyLong(), anyCollection()))
                .thenReturn(Collections.emptyList());

        // All have same workload = 1
        when(horseRepository.findManagedHorsePairs(anyCollection())).thenReturn(rows(
                new Object[]{10L, 100L},
                new Object[]{5L, 101L},
                new Object[]{20L, 102L}
        ));
        when(trainerScheduleRepository.findActiveTrainerHorsePairs(anyCollection(), anyCollection()))
                .thenReturn(Collections.emptyList());

        Optional<User> selected = service.selectLeastLoadedHeadTrainer();

        assertTrue(selected.isPresent());
        assertEquals(5L, selected.get().getId(), "Trainer 5 có id nhỏ nhất phải thắng tie-break");
    }

    @Test
    @DisplayName("3. Lọc bỏ Trainer có lịch đánh giá trùng giờ (overlapping schedule)")
    void selectLeastLoadedHeadTrainer_filtersOutOverlappingTrainers() {
        User trainer1 = createHeadTrainer(1L, "Trainer 1", true);
        User trainer2 = createHeadTrainer(2L, "Trainer 2", true);

        when(userRepository.findActiveHeadTrainers()).thenReturn(List.of(trainer1, trainer2));
        mockValidProfiles(trainer1, trainer2);

        // Trainer 1 is overlapping in the proposed slot!
        com.rtms.backend.entity.TrainerSchedule overlapping = new com.rtms.backend.entity.TrainerSchedule();
        overlapping.setScheduledAt(LocalDateTime.now().minusMinutes(5));
        overlapping.setDurationMinutes(30);
        when(trainerScheduleRepository.findByTrainerIdAndStatusIn(eq(1L), anyCollection()))
                .thenReturn(List.of(overlapping));
        when(trainerScheduleRepository.findByTrainerIdAndStatusIn(eq(2L), anyCollection()))
                .thenReturn(Collections.emptyList());

        when(horseRepository.findManagedHorsePairs(anyCollection())).thenReturn(Collections.emptyList());
        when(trainerScheduleRepository.findActiveTrainerHorsePairs(anyCollection(), anyCollection()))
                .thenReturn(Collections.emptyList());

        Optional<User> selected = service.selectLeastLoadedHeadTrainer();

        assertTrue(selected.isPresent());
        assertEquals(2L, selected.get().getId(), "Trainer 1 bị trùng lịch nên Trainer 2 được chọn");
    }

    @Test
    @DisplayName("4. Tất cả Trainer bị trùng lịch -> trả về Optional.empty()")
    void selectLeastLoadedHeadTrainer_allOverlapping_returnsEmpty() {
        User trainer1 = createHeadTrainer(1L, "Trainer 1", true);

        when(userRepository.findActiveHeadTrainers()).thenReturn(List.of(trainer1));
        mockValidProfiles(trainer1);

        com.rtms.backend.entity.TrainerSchedule overlapping = new com.rtms.backend.entity.TrainerSchedule();
        overlapping.setScheduledAt(LocalDateTime.now().minusMinutes(5));
        overlapping.setDurationMinutes(30);
        when(trainerScheduleRepository.findByTrainerIdAndStatusIn(eq(1L), anyCollection()))
                .thenReturn(List.of(overlapping));

        Optional<User> selected = service.selectLeastLoadedHeadTrainer();

        assertTrue(selected.isEmpty());
    }

    @Test
    @DisplayName("5. Không có Head Trainer active -> trả về Optional.empty()")
    void selectLeastLoadedHeadTrainer_noActiveTrainers_returnsEmpty() {
        when(userRepository.findActiveHeadTrainers()).thenReturn(Collections.emptyList());

        Optional<User> selected = service.selectLeastLoadedHeadTrainer();

        assertTrue(selected.isEmpty());
    }

    @Test
    @DisplayName("6. Head Trainer không có TrainerProfile -> bị bỏ qua")
    void selectLeastLoadedHeadTrainer_skipsTrainerWithoutProfile() {
        User trainerWithProfile = createHeadTrainer(1L, "Trainer Valid", true);
        User trainerWithoutProfile = createHeadTrainer(2L, "Trainer Missing Profile", true);

        when(userRepository.findActiveHeadTrainers()).thenReturn(List.of(trainerWithProfile, trainerWithoutProfile));
        mockValidProfiles(trainerWithProfile); // Only 1L has profile
        when(trainerScheduleRepository.findByTrainerIdAndStatusIn(anyLong(), anyCollection()))
                .thenReturn(Collections.emptyList());
        when(horseRepository.findManagedHorsePairs(anyCollection())).thenReturn(Collections.emptyList());
        when(trainerScheduleRepository.findActiveTrainerHorsePairs(anyCollection(), anyCollection()))
                .thenReturn(Collections.emptyList());

        Optional<User> selected = service.selectLeastLoadedHeadTrainer();

        assertTrue(selected.isPresent());
        assertEquals(1L, selected.get().getId());
    }

    @Test
    @DisplayName("7. getDistinctHorseWorkload tính đúng hợp tập ngựa riêng biệt")
    void getDistinctHorseWorkload_returnsDistinctUnionCount() {
        when(horseRepository.findManagedHorsePairs(List.of(1L))).thenReturn(rows(
                new Object[]{1L, 10L},
                new Object[]{1L, 20L}
        ));
        when(trainerScheduleRepository.findActiveTrainerHorsePairs(eq(List.of(1L)), anyCollection())).thenReturn(rows(
                new Object[]{1L, 20L},
                new Object[]{1L, 30L}
        ));

        long workload = service.getDistinctHorseWorkload(1L);

        // Union: {10, 20, 30} -> size 3
        assertEquals(3L, workload);
    }

    @Test
    @DisplayName("8. getDistinctHorseWorkload với null trainerId trả về 0")
    void getDistinctHorseWorkload_nullTrainer_returnsZero() {
        assertEquals(0L, service.getDistinctHorseWorkload(null));
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
