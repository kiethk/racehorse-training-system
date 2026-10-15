package com.rtms.backend.service;

import com.rtms.backend.entity.CareSchedule;
import com.rtms.backend.entity.Horse;
import com.rtms.backend.entity.Role;
import com.rtms.backend.entity.User;
import com.rtms.backend.entity.VeterinarianProfile;
import com.rtms.backend.enums.CareScheduleStatus;
import com.rtms.backend.enums.CareType;
import com.rtms.backend.repository.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

/**
 * AD-002: thứ tự ưu tiên gán Vet phải xét (1) số ca đã để quá hạn và
 * (2) tổng số phút khám đã tích lũy TRƯỚC tải trong ngày.
 * Hai test dưới đây chọn cặp Vet mà thứ tự id sẽ cho kết quả NGƯỢC LẠI nếu thiếu bậc tương ứng.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CareScheduleAssignmentRankingTest {

    @Mock private CareScheduleRepository careScheduleRepository;
    @Mock private HorseRepository horseRepository;
    @Mock private HealthRecordRepository healthRecordRepository;
    @Mock private HorseHealthMetricRepository metricRepository;
    @Mock private AdmissionApplicationRepository admissionRepository;
    @Mock private StableStallRepository stallRepository;
    @Mock private UserRepository userRepository;
    @Mock private GroomIncidentReportRepository incidentReportRepository;
    @Mock private VeterinarianProfileRepository veterinarianProfileRepository;
    @Mock private AuditLogRepository auditLogRepository;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private EntityManager entityManager;
    @Mock private NotificationService notificationService;
    @Mock private HorseTrainingPlanService trainingPlanService;

    private CareScheduleService service;
    private User fewOverdueVet;   // id 1 — id nhỏ hơn nhưng nhiều ca quá hạn
    private User cleanVet;        // id 2

    @BeforeEach
    void setUp() {
        service = new CareScheduleService(
                careScheduleRepository, horseRepository, healthRecordRepository, metricRepository,
                admissionRepository, stallRepository, userRepository, incidentReportRepository,
                veterinarianProfileRepository, auditLogRepository, eventPublisher, entityManager,
                notificationService,
                new TrainingDecisionService(horseRepository, careScheduleRepository, trainingPlanService));

        fewOverdueVet = vet(1L, "Vet One");
        cleanVet = vet(2L, "Vet Two");

        Horse horse = new Horse();
        horse.setId(10L);
        horse.setName("Lightning");

        when(horseRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(horse));
        when(userRepository.findActiveVeterinarians()).thenReturn(List.of(fewOverdueVet, cleanVet));
        when(careScheduleRepository.findAssignedInDay(any(), any(), anyList())).thenReturn(List.of());
        when(careScheduleRepository.findScheduledForVet(anyLong(), any())).thenReturn(List.of());
        when(careScheduleRepository.findScheduledForHorse(anyLong(), any())).thenReturn(List.of());
        when(careScheduleRepository.existsByVeterinarianIdAndStatus(anyLong(), any())).thenReturn(false);
        when(careScheduleRepository.existsByHorseIdAndStatus(anyLong(), any())).thenReturn(false);
        when(careScheduleRepository.existsByVeterinarianIdAndHorseIdAndStatusIn(anyLong(), anyLong(), anyList()))
                .thenReturn(false);
        when(careScheduleRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(userRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(fewOverdueVet));
        when(userRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(cleanVet));
        when(veterinarianProfileRepository.findById(anyLong())).thenAnswer(i -> {
            VeterinarianProfile profile = new VeterinarianProfile();
            profile.setUserId(i.getArgument(0));
            profile.setLicenseNumber("LIC-" + i.getArgument(0));
            return Optional.of(profile);
        });
    }

    @Test
    @DisplayName("AD-002 bậc 1: Vet ít ca quá hạn hơn được gán trước, dù id lớn hơn")
    void fewerOverdueCasesWins() {
        CareSchedule schedule = requestedRoutine();
        when(careScheduleRepository.countByVeterinarianIdAndStatusInAndScheduledAtBefore(eq(1L), anyList(), any()))
                .thenReturn(5L);
        when(careScheduleRepository.countByVeterinarianIdAndStatusInAndScheduledAtBefore(eq(2L), anyList(), any()))
                .thenReturn(0L);
        when(careScheduleRepository.sumAssignedMinutesByVeterinarian(anyLong())).thenReturn(0L);

        service.assignRequestedSchedule(schedule);

        assertEquals(2L, schedule.getVeterinarianId(),
                "Vet 0 ca qua han phai thang Vet 5 ca qua han (AD-002 bac 1)");
    }

    @Test
    @DisplayName("AD-002 bậc 2: hoà số ca quá hạn thì Vet ít phút tích lũy hơn được gán trước")
    void lowerCumulativeMinutesWins() {
        CareSchedule schedule = requestedRoutine();
        when(careScheduleRepository.countByVeterinarianIdAndStatusInAndScheduledAtBefore(anyLong(), anyList(), any()))
                .thenReturn(0L);
        when(careScheduleRepository.sumAssignedMinutesByVeterinarian(1L)).thenReturn(900L);
        when(careScheduleRepository.sumAssignedMinutesByVeterinarian(2L)).thenReturn(300L);

        service.assignRequestedSchedule(schedule);

        assertEquals(2L, schedule.getVeterinarianId(),
                "Vet 300 phut tich luy phai thang Vet 900 phut (AD-002 bac 2)");
    }

    private CareSchedule requestedRoutine() {
        CareSchedule schedule = new CareSchedule();
        schedule.setId(101L);
        schedule.setHorseId(10L);
        schedule.setCareType(CareType.ROUTINE);
        schedule.setStatus(CareScheduleStatus.REQUESTED);
        schedule.setRequestedAt(LocalDateTime.now());
        schedule.setDurationMinutes(5);
        return schedule;
    }

    private User vet(Long id, String fullName) {
        User user = new User();
        user.setId(id);
        user.setFullName(fullName);
        user.setActive(true);
        Role role = new Role();
        role.setName("VETERINARIAN");
        user.setRole(role);
        return user;
    }
}
