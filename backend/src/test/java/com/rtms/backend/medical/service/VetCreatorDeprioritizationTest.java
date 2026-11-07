package com.rtms.backend.medical.service;

import com.rtms.backend.audit.repository.AuditLogRepository;
import com.rtms.backend.horse.entity.Horse;
import com.rtms.backend.horse.repository.HorseRepository;
import com.rtms.backend.identity.entity.Role;
import com.rtms.backend.identity.entity.User;
import com.rtms.backend.identity.entity.VeterinarianProfile;
import com.rtms.backend.identity.repository.UserRepository;
import com.rtms.backend.identity.repository.VeterinarianProfileRepository;
import com.rtms.backend.medical.entity.CareSchedule;
import com.rtms.backend.medical.enums.CareScheduleStatus;
import com.rtms.backend.medical.enums.CareType;
import com.rtms.backend.medical.repository.CareScheduleRepository;
import com.rtms.backend.notification.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VetCreatorDeprioritizationTest {

    @Mock private CareScheduleRepository careScheduleRepository;
    @Mock private HorseRepository horseRepository;
    @Mock private UserRepository userRepository;
    @Mock private VeterinarianProfileRepository veterinarianProfileRepository;
    @Mock private AuditLogRepository auditLogRepository;
    @Mock private NotificationService notificationService;

    @InjectMocks
    private CareScheduleService careScheduleService;

    private User vet1;
    private User vet2;
    private Horse horse;

    @BeforeEach
    void setUp() {
        vet1 = eligibleVet(10L);
        vet2 = eligibleVet(20L);

        horse = new Horse();
        horse.setId(100L);
        horse.setName("Thunder");

        lenient().when(careScheduleRepository.findByHorseIdAndStatusIn(anyLong(), any()))
                .thenReturn(Collections.emptyList());
        lenient().when(careScheduleRepository.findByVeterinarianIdAndStatusIn(anyLong(), any()))
                .thenReturn(Collections.emptyList());
        lenient().when(careScheduleRepository.existsByHorseIdAndStatus(anyLong(), any()))
                .thenReturn(false);
        lenient().when(careScheduleRepository.findAssignedInDay(any(), any(), any()))
                .thenReturn(Collections.emptyList());
        lenient().when(careScheduleRepository.findByVeterinarianIdAndStatus(anyLong(), any()))
                .thenReturn(Collections.emptyList());
        lenient().when(careScheduleRepository.findScheduledForVet(anyLong(), any()))
                .thenReturn(Collections.emptyList());
        lenient().when(careScheduleRepository.findByStatusInAndVeterinarianIdIsNotNull(any()))
                .thenReturn(Collections.emptyList());
        lenient().when(careScheduleRepository.existsByHorseIdAndStatusIn(anyLong(), any()))
                .thenReturn(false);
        lenient().when(careScheduleRepository.existsByVeterinarianIdAndStatus(anyLong(), any()))
                .thenReturn(false);
        lenient().when(careScheduleRepository.save(any(CareSchedule.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private User eligibleVet(Long id) {
        Role role = new Role();
        role.setName("VETERINARIAN");
        User user = new User();
        user.setId(id);
        user.setRole(role);
        user.setActive(true);
        return user;
    }

    private void lockEligible(User... vets) {
        for (User vet : vets) {
            lenient().when(userRepository.findByIdForUpdate(vet.getId())).thenReturn(Optional.of(vet));
            VeterinarianProfile profile = new VeterinarianProfile();
            profile.setUserId(vet.getId());
            profile.setLicenseNumber("VET-" + vet.getId());
            lenient().when(veterinarianProfileRepository.findById(vet.getId())).thenReturn(Optional.of(profile));
        }
    }

    @Test
    @DisplayName("When Vet1 creates next routine schedule, Vet2 is prioritized and assigned ahead of creator Vet1")
    void testCreatorVetIsDeprioritizedInFavorOfOtherActiveVet() {
        LocalDateTime futureSlot = LocalDate.now().plusDays(3).atTime(LocalTime.of(14, 0));

        CareSchedule requestedSchedule = new CareSchedule();
        requestedSchedule.setId(1001L);
        requestedSchedule.setHorseId(horse.getId());
        requestedSchedule.setCareType(CareType.ROUTINE);
        requestedSchedule.setStatus(CareScheduleStatus.REQUESTED);
        requestedSchedule.setRequestedAt(futureSlot);
        requestedSchedule.setDurationMinutes(30);
        requestedSchedule.setRequestedById(vet1.getId()); // Creator is Vet1

        when(horseRepository.findByIdForUpdate(horse.getId())).thenReturn(Optional.of(horse));
        when(userRepository.findActiveVeterinarians()).thenReturn(List.of(vet1, vet2));
        lockEligible(vet1, vet2);

        careScheduleService.assignRequestedSchedule(requestedSchedule, vet1.getId());

        assertEquals(CareScheduleStatus.SCHEDULED, requestedSchedule.getStatus());
        assertEquals(vet2.getId(), requestedSchedule.getVeterinarianId(), "Vet2 must be assigned instead of creator Vet1");
    }

    @Test
    @DisplayName("When other vet is not available, creator Vet1 is still assigned as fallback")
    void testCreatorVetIsAssignedWhenNoOtherVetAvailable() {
        LocalDateTime futureSlot = LocalDate.now().plusDays(3).atTime(LocalTime.of(14, 0));

        CareSchedule requestedSchedule = new CareSchedule();
        requestedSchedule.setId(1002L);
        requestedSchedule.setHorseId(horse.getId());
        requestedSchedule.setCareType(CareType.ROUTINE);
        requestedSchedule.setStatus(CareScheduleStatus.REQUESTED);
        requestedSchedule.setRequestedAt(futureSlot);
        requestedSchedule.setDurationMinutes(30);
        requestedSchedule.setRequestedById(vet1.getId());

        when(horseRepository.findByIdForUpdate(horse.getId())).thenReturn(Optional.of(horse));
        // Only Vet1 is active in the farm
        when(userRepository.findActiveVeterinarians()).thenReturn(List.of(vet1));
        lockEligible(vet1);

        careScheduleService.assignRequestedSchedule(requestedSchedule, vet1.getId());

        assertEquals(CareScheduleStatus.SCHEDULED, requestedSchedule.getStatus());
        assertEquals(vet1.getId(), requestedSchedule.getVeterinarianId(), "Fallback to creator Vet1 when no other vet exists");
    }
}
