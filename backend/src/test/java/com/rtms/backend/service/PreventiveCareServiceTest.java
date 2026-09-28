package com.rtms.backend.service;

import com.rtms.backend.config.ApiException;
import com.rtms.backend.dto.CompletePreventiveCareScheduleRequest;
import com.rtms.backend.dto.CreatePreventiveCareScheduleRequest;
import com.rtms.backend.entity.HealthRecord;
import com.rtms.backend.entity.PreventiveCareSchedule;
import com.rtms.backend.repository.*;
import com.rtms.backend.security.AuthenticatedUser;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PreventiveCareServiceTest {
    @Mock PreventiveCareScheduleRepository schedules;
    @Mock HealthRecordRepository records;
    @Mock HorseRepository horses;
    private final AuthenticatedUser vet = new AuthenticatedUser(7L, "vet@example.com", "VETERINARIAN");

    @Test
    @DisplayName("Không hoàn tất lịch COMPLETED lần hai và không tạo hồ sơ mới")
    void completedScheduleCannotBeCompletedAgain() {
        PreventiveCareSchedule schedule = new PreventiveCareSchedule();
        schedule.setStatus("COMPLETED");
        when(schedules.findByIdForUpdate(1L)).thenReturn(Optional.of(schedule));
        PreventiveCareService service = new PreventiveCareService(schedules, records, horses);
        ApiException error = assertThrows(ApiException.class,
                () -> service.recordCompletion(1L, new CompletePreventiveCareScheduleRequest(), null));
        assertEquals(HttpStatus.CONFLICT, error.getStatus());
        assertEquals("INVALID_REVIEW_STATE", error.getErrorCode());
        verifyNoInteractions(records);
    }

    @Test
    void initialExamMustBeCompletedThroughAdmissionReview() {
        PreventiveCareSchedule schedule = new PreventiveCareSchedule();
        schedule.setId(1L);
        schedule.setCareType("INITIAL_EXAM");
        when(schedules.findByIdForUpdate(1L)).thenReturn(Optional.of(schedule));

        ApiException error = assertThrows(ApiException.class,
                () -> new PreventiveCareService(schedules, records, horses)
                        .recordCompletion(1L, new CompletePreventiveCareScheduleRequest(), vet));

        assertEquals(HttpStatus.CONFLICT, error.getStatus());
        assertEquals("INVALID_REVIEW_STATE", error.getErrorCode());
        assertTrue(error.getMessage().contains("admission vet review"));
        verifyNoInteractions(records);
        verify(schedules, never()).save(any());
        assertEquals("PENDING", schedule.getStatus());
    }

    @Test
    void nonInitialExamCanBeCompleted() {
        PreventiveCareSchedule schedule = new PreventiveCareSchedule();
        schedule.setId(1L);
        schedule.setHorseId(2L);
        schedule.setCareType("VACCINATION");
        when(schedules.findByIdForUpdate(1L)).thenReturn(Optional.of(schedule));
        HealthRecord saved = new HealthRecord();
        when(records.save(any(HealthRecord.class))).thenReturn(saved);

        HealthRecord result = new PreventiveCareService(schedules, records, horses)
                .recordCompletion(1L, new CompletePreventiveCareScheduleRequest(), vet);

        assertSame(saved, result);
        verify(records).save(argThat(record -> record.getHorseId().equals(2L)
                && record.getPreventiveCareScheduleId().equals(1L) && record.getVeterinarianId().equals(7L)));
        assertEquals("COMPLETED", schedule.getStatus());
        verify(schedules).save(schedule);
    }

    @Test
    void createScheduleRejectsInitialExamAndUnknownType() {
        PreventiveCareService service = new PreventiveCareService(schedules, records, horses);
        for (String type : new String[]{"INITIAL_EXAM", "UNKNOWN"}) {
            CreatePreventiveCareScheduleRequest request = new CreatePreventiveCareScheduleRequest();
            request.setCareType(type);
            ApiException error = assertThrows(ApiException.class, () -> service.createSchedule(request, vet));
            assertEquals(HttpStatus.BAD_REQUEST, error.getStatus());
            assertEquals("VALIDATION_ERROR", error.getErrorCode());
            assertTrue(error.getMessage().contains("INITIAL_EXAM is created by the Groom/admission flow"));
        }
        verifyNoInteractions(schedules);
    }

    @Test
    void createScheduleAcceptsVetCareTypes() {
        PreventiveCareService service = new PreventiveCareService(schedules, records, horses);
        when(schedules.save(any(PreventiveCareSchedule.class))).thenAnswer(invocation -> invocation.getArgument(0));
        for (String type : new String[]{"ROUTINE_EXAM", "VACCINATION", "DEWORMING"}) {
            CreatePreventiveCareScheduleRequest request = new CreatePreventiveCareScheduleRequest();
            request.setHorseId(2L);
            request.setCareType(type);
            PreventiveCareSchedule schedule = service.createSchedule(request, vet);
            assertEquals(type, schedule.getCareType());
            assertEquals(2L, schedule.getHorseId());
            assertEquals(7L, schedule.getVeterinarianId());
        }
        verify(schedules, times(3)).save(any(PreventiveCareSchedule.class));
    }
}
