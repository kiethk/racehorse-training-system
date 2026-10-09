package com.rtms.backend.medical.service;
import com.rtms.backend.horse.repository.HorseRepository;
import com.rtms.backend.medical.dto.CareScheduleResponse;
import com.rtms.backend.medical.dto.CompletePreventiveCareScheduleRequest;
import com.rtms.backend.medical.dto.CreatePreventiveCareScheduleRequest;
import com.rtms.backend.medical.enums.CareScheduleStatus;
import com.rtms.backend.medical.enums.CareType;
import com.rtms.backend.medical.repository.CareScheduleRepository;


import com.rtms.backend.config.ApiException;
import com.rtms.backend.medical.entity.CareSchedule;
import com.rtms.backend.security.AuthenticatedUser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PreventiveCareServiceTest {
    @Mock CareScheduleRepository schedules;
    @Mock HorseRepository horses;
    @Mock CareScheduleService care;
    private final AuthenticatedUser vet = new AuthenticatedUser(7L, "vet@example.com", "VETERINARIAN");

    @Test
    void legacyCompletionCannotBypassUnifiedStateMachine() {
        ApiException error = assertThrows(ApiException.class, () ->
                new PreventiveCareService(schedules, horses, care)
                    .recordCompletion(1L, new CompletePreventiveCareScheduleRequest(), vet));
        assertEquals(HttpStatus.GONE, error.getStatus());
        verifyNoInteractions(schedules, horses, care);
    }

    @Test
    void rejectsInitialAndUnknownCareTypes() {
        for (String type : new String[]{"INITIAL_EXAM", "UNKNOWN"}) {
            CreatePreventiveCareScheduleRequest request = new CreatePreventiveCareScheduleRequest();
            request.setCareType(type);
            assertThrows(ApiException.class, () ->
                    new PreventiveCareService(schedules, horses, care).createSchedule(request, vet));
        }
        verifyNoInteractions(care, schedules);
    }

    @Test
    void preventiveCreationUsesUnifiedOfferFlow() {
        CareSchedule schedule = new CareSchedule();
        schedule.setId(1L);
        schedule.setHorseId(2L);
        schedule.setCareType(CareType.ROUTINE);
        CareSchedule source = new CareSchedule();
        source.setId(99L);
        source.setHorseId(2L);
        source.setVeterinarianId(7L);
        source.setStatus(CareScheduleStatus.COMPLETED);
        when(schedules.findFirstByVeterinarianIdAndHorseIdAndStatusOrderByCompletedAtDesc(
                7L, 2L, CareScheduleStatus.COMPLETED)).thenReturn(Optional.of(source));
        when(care.createNextSchedule(any(), eq(7L))).thenReturn(CareScheduleResponse.from(schedule));
        when(schedules.findById(1L)).thenReturn(Optional.of(schedule));
        CreatePreventiveCareScheduleRequest request = new CreatePreventiveCareScheduleRequest();
        request.setHorseId(2L);
        request.setCareType("VACCINATION");
        assertSame(schedule, new PreventiveCareService(schedules, horses, care).createSchedule(request, vet));
        verify(care).createNextSchedule(argThat(r -> r.getCareType() == CareType.ROUTINE
                && Long.valueOf(99L).equals(r.getSourceScheduleId())
                && r.getIdempotencyKey() != null
                && r.getDescription().startsWith("VACCINATION:")), eq(7L));
        verify(schedules, never()).save(any());
    }
}
