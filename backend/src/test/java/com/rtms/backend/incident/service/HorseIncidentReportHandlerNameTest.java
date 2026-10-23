package com.rtms.backend.incident.service;

import com.rtms.backend.common.storage.FileStorageService;
import com.rtms.backend.horse.repository.HorseRepository;
import com.rtms.backend.identity.entity.User;
import com.rtms.backend.identity.repository.UserRepository;
import com.rtms.backend.incident.dto.HandleIncidentRequest;
import com.rtms.backend.incident.entity.HorseIncidentReport;
import com.rtms.backend.incident.enums.IncidentStatus;
import com.rtms.backend.incident.repository.HorseIncidentReportRepository;
import com.rtms.backend.medical.repository.CareScheduleRepository;
import com.rtms.backend.medical.service.CareScheduleService;
import com.rtms.backend.security.AuthenticatedUser;
import com.rtms.backend.training.service.TrainingDecisionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.*;

/** Người báo sự cố thấy tên vet được giao (handledById) trên mỗi báo cáo. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class HorseIncidentReportHandlerNameTest {

    @Mock HorseIncidentReportRepository reports;
    @Mock HorseRepository horses;
    @Mock FileStorageService files;
    @Mock CareScheduleRepository schedules;
    @Mock CareScheduleService careScheduleService;
    @Mock TrainingDecisionService trainingDecisions;
    @Mock UserRepository users;

    private HorseIncidentReportService service;
    private final AuthenticatedUser groom = new AuthenticatedUser(10L, "groom1@rtms.com", "GROOM");

    @BeforeEach
    void setUp() {
        service = new HorseIncidentReportService(reports, horses, files, schedules, careScheduleService,
                trainingDecisions, users);
        when(users.findAllById(anyCollection())).thenAnswer(call -> {
            Collection<Long> ids = call.getArgument(0);
            return ids.stream().map(id -> user(id, id == 6L ? "James Carter" : "Hannah Reed")).toList();
        });
    }

    @Test
    void listCarriesTheAssignedVetNameAndLooksUpNamesOnce() {
        HorseIncidentReport assigned = report(1L, IncidentStatus.IN_REVIEW, 6L);
        HorseIncidentReport alsoAssigned = report(2L, IncidentStatus.RESOLVED, 6L);
        HorseIncidentReport waiting = report(3L, IncidentStatus.REPORTED, null);
        when(reports.findByReportedById(10L)).thenReturn(List.of(assigned, alsoAssigned, waiting));

        List<HorseIncidentReport> result = service.getReports(null, null, groom);

        assertEquals("James Carter", assigned.getHandlerName());
        assertEquals("James Carter", alsoAssigned.getHandlerName());
        assertNull(waiting.getHandlerName());
        assertEquals(3, result.size());
        verify(users, times(1)).findAllById(List.of(6L));
    }

    @Test
    void reportJoinedToAnOpenUrgentCaseShowsThatCasesVet() {
        // Second report on the same horse: linked to the open urgent case, no handledById of its own.
        HorseIncidentReport joined = report(6L, IncidentStatus.REPORTED, null);
        joined.setCareScheduleId(94L);
        HorseIncidentReport unassignedCase = report(7L, IncidentStatus.REPORTED, null);
        unassignedCase.setCareScheduleId(95L);
        when(reports.findByReportedById(10L)).thenReturn(List.of(joined, unassignedCase));
        when(schedules.findAllById(anyCollection())).thenReturn(List.of(schedule(94L, 3L), schedule(95L, null)));

        service.getReports(null, null, groom);

        assertEquals("Hannah Reed", joined.getHandlerName());
        assertNull(unassignedCase.getHandlerName(), "the urgent case has no vet yet");
        verify(schedules, times(1)).findAllById(argThat(ids -> java.util.Set.copyOf((Collection<Long>) ids).equals(java.util.Set.of(94L, 95L))));
    }

    @Test
    void handledByWinsOverTheLinkedCase() {
        HorseIncidentReport handled = report(8L, IncidentStatus.IN_REVIEW, 6L);
        handled.setCareScheduleId(94L);
        when(reports.findByReportedById(10L)).thenReturn(List.of(handled));

        service.getReports(null, null, groom);

        assertEquals("James Carter", handled.getHandlerName());
        verify(schedules, never()).findAllById(any());
    }

    @Test
    void reportsWithoutAnyVetSkipTheLookup() {
        when(reports.findByReportedById(10L)).thenReturn(List.of(report(3L, IncidentStatus.REPORTED, null)));

        service.getReports(null, null, groom);

        verify(users, never()).findAllById(any());
    }

    @Test
    void singleReportAndHandledReportCarryTheName() {
        HorseIncidentReport own = report(1L, IncidentStatus.IN_REVIEW, 6L);
        own.setReportedById(10L);
        when(reports.findById(1L)).thenReturn(Optional.of(own));
        assertEquals("James Carter", service.getReportById(1L, groom).getHandlerName());

        HorseIncidentReport pending = report(4L, IncidentStatus.REPORTED, null);
        when(reports.findById(4L)).thenReturn(Optional.of(pending));
        when(reports.save(any())).thenAnswer(call -> call.getArgument(0));
        HandleIncidentRequest request = new HandleIncidentRequest();
        request.setStatus(IncidentStatus.IN_REVIEW);

        HorseIncidentReport handled = service.handleReport(4L, request, new AuthenticatedUser(3L, "vet3@rtms.com", "VETERINARIAN"));

        assertEquals("Hannah Reed", handled.getHandlerName());
    }

    private static HorseIncidentReport report(long id, IncidentStatus status, Long vetId) {
        HorseIncidentReport report = new HorseIncidentReport();
        report.setId(id);
        report.setStatus(status);
        report.setHandledById(vetId);
        report.setReportedAt(LocalDateTime.of(2026, 10, 20, 8, 0).plusHours(id));
        return report;
    }

    private static com.rtms.backend.medical.entity.CareSchedule schedule(long id, Long vetId) {
        com.rtms.backend.medical.entity.CareSchedule schedule = new com.rtms.backend.medical.entity.CareSchedule();
        schedule.setId(id);
        schedule.setVeterinarianId(vetId);
        return schedule;
    }

    private static User user(long id, String name) {
        User user = new User();
        user.setId(id);
        user.setFullName(name);
        return user;
    }
}
