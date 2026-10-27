package com.rtms.backend.medical.service;

import com.rtms.backend.config.ApiException;
import com.rtms.backend.horse.entity.Horse;
import com.rtms.backend.horse.enums.HorseStatus;
import com.rtms.backend.horse.repository.HorseRepository;
import com.rtms.backend.medical.dto.PreventiveCoverageResponse;
import com.rtms.backend.medical.dto.PreventiveDueHorse;
import com.rtms.backend.medical.dto.PreventiveDueSummary;
import com.rtms.backend.medical.dto.PreventiveProtocolStatus;
import com.rtms.backend.medical.entity.PreventiveProtocol;
import com.rtms.backend.medical.enums.PreventiveDueStatus;
import com.rtms.backend.medical.enums.ProtocolCategory;
import com.rtms.backend.medical.repository.CareScheduleRepository;
import com.rtms.backend.medical.repository.PreventiveProtocolRepository;
import com.rtms.backend.security.AuthenticatedUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.data.domain.Page;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PreventiveCareQueryServiceTest {
    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 18);

    @Mock PreventiveProtocolRepository protocols;
    @Mock HorseRepository horses;
    @Mock CareScheduleRepository schedules;
    @Mock PreventiveStatusService preventiveStatus;

    private PreventiveCareQueryService service;

    @BeforeEach
    void setUp() {
        service = new PreventiveCareQueryService(protocols, horses, schedules, preventiveStatus,
                Clock.fixed(TODAY.atTime(9, 0).atZone(ZONE).toInstant(), ZONE));
        Horse alpha = horse(1L, "Alpha", HorseStatus.ELIGIBLE, 5L);
        Horse bravo = horse(2L, "Bravo", HorseStatus.CANDIDATE, 6L);
        when(horses.findByCurrentStatusInOrderByNameAsc(PreventiveCareQueryService.HORSES_IN_SCOPE))
                .thenReturn(List.of(alpha, bravo));
        when(horses.findById(1L)).thenReturn(Optional.of(alpha));
        when(preventiveStatus.statusFor(anyCollection())).thenReturn(Map.of(
                1L, List.of(
                        status(1L, 1L, TODAY.plusDays(100), PreventiveDueStatus.OK),
                        status(1L, 2L, TODAY.plusDays(20), PreventiveDueStatus.DUE_SOON),
                        status(1L, 3L, null, PreventiveDueStatus.NONE)),
                2L, List.of(
                        status(2L, 1L, null, PreventiveDueStatus.MISSING),
                        status(2L, 2L, TODAY.plusDays(5), PreventiveDueStatus.DUE_SOON),
                        status(2L, 3L, TODAY.minusDays(3), PreventiveDueStatus.OVERDUE))));
        when(protocols.findByActiveTrueOrderByCategoryAscIdAsc()).thenReturn(List.of(
                protocol(1L, "EI"), protocol(2L, "TETANUS"), protocol(3L, "RABIES")));
    }

    @Test
    void dueListHasOneRowPerHorseMostUrgentFirst() {
        var page = service.dueHorses(null, null, null, 0, 20);

        assertEquals(2, page.getTotalElements());
        PreventiveDueHorse bravo = page.getContent().get(0);
        assertEquals("Bravo", bravo.horseName());
        assertEquals(HorseStatus.CANDIDATE, bravo.horseStatus());
        assertEquals(PreventiveDueStatus.MISSING, bravo.worstStatus());
        assertNull(bravo.earliestDueOn());
        assertEquals(List.of(1L, 3L, 2L), bravo.protocols().stream().map(PreventiveProtocolStatus::protocolId).toList());
        PreventiveDueHorse alpha = page.getContent().get(1);
        assertEquals(List.of(2L), alpha.protocols().stream().map(PreventiveProtocolStatus::protocolId).toList());
        assertEquals(TODAY.plusDays(20), alpha.earliestDueOn());
    }

    @Test
    void dueListFiltersByStatusProtocolAndHorseName() {
        assertEquals(List.of("Bravo", "Alpha"), names(service.dueHorses(EnumSet.of(PreventiveDueStatus.DUE_SOON), null, null, 0, 20)));
        assertEquals(List.of("Bravo"), names(service.dueHorses(null, 1L, null, 0, 20)));
        assertEquals(List.of("Alpha"), names(service.dueHorses(EnumSet.of(PreventiveDueStatus.OK), null, null, 0, 20)));
        assertEquals(List.of("Alpha"), names(service.dueHorses(null, null, "  aLP ", 0, 20)));
        assertEquals(List.of(), names(service.dueHorses(null, null, "zulu", 0, 20)));
    }

    @Test
    void dueListIsPagedAfterSorting() {
        var first = service.dueHorses(null, null, null, 0, 1);
        var second = service.dueHorses(null, null, null, 1, 1);
        var beyond = service.dueHorses(null, null, null, 5, 1);

        assertEquals(List.of("Bravo"), names(first));
        assertEquals(List.of("Alpha"), names(second));
        assertEquals(2, first.getTotalElements());
        assertEquals(2, first.getTotalPages());
        assertTrue(beyond.getContent().isEmpty());
    }

    @Test
    void summaryCountsAcrossTheWholeStable() {
        PreventiveDueSummary summary = service.dueSummary();

        assertEquals(2, summary.overdue());
        assertEquals(2, summary.dueSoon());
        assertEquals(2, summary.horses());
    }

    @Test
    void coverageCountsEveryHorseInTheStablePerProtocol() {
        List<PreventiveCoverageResponse> coverage = service.coverage();

        assertEquals(3, coverage.size());
        PreventiveCoverageResponse ei = coverage.get(0);
        assertEquals("EI", ei.code());
        assertEquals(2, ei.totalHorses());
        assertEquals(1, ei.ok());
        assertEquals(1, ei.missing());
        PreventiveCoverageResponse rabies = coverage.get(2);
        assertEquals(1, rabies.overdue());
        assertEquals(1, rabies.none());
    }

    @Test
    void ownerSeesOnlyTheirOwnHorse() {
        when(preventiveStatus.statusFor(1L)).thenReturn(List.of());
        when(schedules.findFirstByHorseIdAndPurposeAndStatusIn(any(), any(), any())).thenReturn(Optional.empty());

        assertEquals("Alpha", service.horseStatus(1L, new AuthenticatedUser(5L, "o@x.test", "HORSE_OWNER")).horseName());
        assertNull(service.horseStatus(1L, new AuthenticatedUser(20L, "v@x.test", "VETERINARIAN")).nextMilestone());

        ApiException error = assertThrows(ApiException.class, () ->
                service.horseStatus(1L, new AuthenticatedUser(6L, "other@x.test", "HORSE_OWNER")));
        assertEquals(HttpStatus.FORBIDDEN, error.getStatus());
    }

    private static List<String> names(Page<PreventiveDueHorse> page) {
        return page.getContent().stream().map(PreventiveDueHorse::horseName).toList();
    }

    private static Horse horse(long id, String name, HorseStatus status, long ownerId) {
        Horse horse = new Horse();
        horse.setId(id);
        horse.setName(name);
        horse.setCurrentStatus(status);
        horse.setOwnerId(ownerId);
        return horse;
    }

    private static PreventiveProtocolStatus status(long horseId, long protocolId, LocalDate nextDue,
            PreventiveDueStatus status) {
        return new PreventiveProtocolStatus(horseId, protocolId, "P" + protocolId, "P" + protocolId,
                ProtocolCategory.VACCINE, true, nextDue == null ? null : nextDue.minusDays(180), nextDue, status);
    }

    private static PreventiveProtocol protocol(long id, String code) {
        PreventiveProtocol protocol = new PreventiveProtocol();
        protocol.setId(id);
        protocol.setCode(code);
        protocol.setName(code);
        protocol.setCategory(ProtocolCategory.VACCINE);
        protocol.setMandatory(true);
        protocol.setActive(true);
        return protocol;
    }
}
