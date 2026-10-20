package com.rtms.backend.medical.service;

import com.rtms.backend.horse.entity.Horse;
import com.rtms.backend.horse.enums.HorseStatus;
import com.rtms.backend.horse.repository.HorseRepository;
import com.rtms.backend.medical.dto.PreventiveProtocolStatus;
import com.rtms.backend.medical.entity.CareSchedule;
import com.rtms.backend.medical.entity.PreventiveProtocol;
import com.rtms.backend.medical.enums.CareSchedulePurpose;
import com.rtms.backend.medical.enums.CareScheduleStatus;
import com.rtms.backend.medical.enums.CareType;
import com.rtms.backend.medical.enums.PreventiveDueStatus;
import com.rtms.backend.medical.enums.ProtocolCategory;
import com.rtms.backend.medical.repository.CareScheduleRepository;
import com.rtms.backend.medical.repository.PrescriptionRepository;
import com.rtms.backend.medical.repository.PrescriptionRepository.LastPreventiveDose;
import com.rtms.backend.medical.repository.PreventiveProtocolRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PreventiveStatusServiceTest {
    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 18);
    private static final long HORSE = 2L;
    private static final long ACTOR = 7L;

    @Mock PreventiveProtocolRepository protocols;
    @Mock PrescriptionRepository prescriptions;
    @Mock CareScheduleRepository schedules;
    @Mock HorseRepository horses;
    @Mock com.rtms.backend.admission.repository.AdmissionApplicationRepository admissions;

    private final PreventiveProtocol ei = protocol(1L, "EI", 180, 14, true);
    private final PreventiveProtocol tetanus = protocol(2L, "TETANUS", 365, 30, true);
    private final PreventiveProtocol rabies = protocol(3L, "RABIES", 365, 30, false);
    private final List<LastPreventiveDose> doses = new ArrayList<>();
    private PreventiveStatusService service;

    @BeforeEach
    void setUp() {
        service = new PreventiveStatusService(protocols, prescriptions, schedules, horses, admissions,
                Clock.fixed(TODAY.atTime(9, 0).atZone(ZONE).toInstant(), ZONE));
        when(protocols.findByActiveTrueOrderByCategoryAscIdAsc()).thenReturn(List.of(ei, tetanus, rabies));
        when(prescriptions.findLastPreventiveDoses(anyCollection())).thenReturn(doses);
        Horse horse = new Horse();
        horse.setId(HORSE);
        horse.setCurrentStatus(HorseStatus.ELIGIBLE);
        when(horses.findByIdForUpdate(HORSE)).thenReturn(Optional.of(horse));
        when(schedules.findFirstByHorseIdAndPurposeAndStatusIn(eq(HORSE), eq(CareSchedulePurpose.PREVENTIVE), any()))
                .thenReturn(Optional.empty());
        when(schedules.findByRequestedByIdAndIdempotencyKey(any(), any())).thenReturn(Optional.empty());
        when(schedules.save(any())).thenAnswer(call -> call.getArgument(0));
        when(schedules.saveAndFlush(any())).thenAnswer(call -> call.getArgument(0));
    }

    // ---- statusFor / classify ----

    @Test
    void classifiesEachProtocolFromItsLastDose() {
        dose(1L, TODAY.minusDays(100));   // EI hạn +80 ngày        -> OK
        dose(2L, TODAY.minusDays(340));   // Tetanus hạn +25 ngày   -> DUE_SOON (≤ 30)
        dose(3L, TODAY.minusDays(400));   // Rabies hạn -35 ngày    -> OVERDUE

        Map<Long, PreventiveDueStatus> byProtocol = byProtocol(service.statusFor(HORSE));

        assertEquals(PreventiveDueStatus.OK, byProtocol.get(1L));
        assertEquals(PreventiveDueStatus.DUE_SOON, byProtocol.get(2L));
        assertEquals(PreventiveDueStatus.OVERDUE, byProtocol.get(3L));
    }

    @Test
    void neverVaccinatedIsMissingOnlyForMandatoryProtocols() {
        Map<Long, PreventiveDueStatus> byProtocol = byProtocol(service.statusFor(HORSE));

        assertEquals(PreventiveDueStatus.MISSING, byProtocol.get(1L));
        assertEquals(PreventiveDueStatus.MISSING, byProtocol.get(2L));
        assertEquals(PreventiveDueStatus.NONE, byProtocol.get(3L));
    }

    @Test
    void boundariesOfWarningWindowAndDueDay() {
        assertEquals(PreventiveDueStatus.DUE_SOON, PreventiveStatusService.classify(ei, TODAY, TODAY));
        assertEquals(PreventiveDueStatus.DUE_SOON, PreventiveStatusService.classify(ei, TODAY.plusDays(14), TODAY));
        assertEquals(PreventiveDueStatus.OK, PreventiveStatusService.classify(ei, TODAY.plusDays(15), TODAY));
        assertEquals(PreventiveDueStatus.OVERDUE, PreventiveStatusService.classify(ei, TODAY.minusDays(1), TODAY));
    }

    @Test
    void reportsLastDoseAndNextDue() {
        dose(1L, LocalDate.of(2026, 10, 1));

        PreventiveProtocolStatus eiStatus = service.statusFor(HORSE).get(0);

        assertEquals(LocalDate.of(2026, 10, 1), eiStatus.lastDoseOn());
        assertEquals(LocalDate.of(2027, 3, 30), eiStatus.nextDueOn());
    }

    // ---- upsertMilestone ----

    @Test
    void createsRequestedMilestoneAtEarliestMandatoryDueDate() {
        dose(1L, TODAY);                       // EI hạn 16/04/2027
        dose(2L, TODAY.minusDays(200));        // Tetanus hạn 18/04/2027 (+165)
        dose(3L, TODAY.minusDays(360));        // Rabies không bắt buộc, bỏ qua dù sắp tới hạn

        CareSchedule milestone = service.upsertMilestone(HORSE, "preventive:10", ACTOR, null).orElseThrow();

        assertEquals(CareType.ROUTINE, milestone.getCareType());
        assertEquals(CareSchedulePurpose.PREVENTIVE, milestone.getPurpose());
        assertEquals(CareScheduleStatus.REQUESTED, milestone.getStatus());
        assertEquals(TODAY.plusDays(165).atTime(14, 0), milestone.getRequestedAt());
        assertNull(milestone.getVeterinarianId());
        assertEquals(ACTOR, milestone.getRequestedById());
        assertEquals("preventive:10", milestone.getIdempotencyKey());
        assertTrue(milestone.getDescription().contains("TETANUS"));
        assertFalse(milestone.getDescription().contains("RABIES"));
    }

    @Test
    void overdueTargetIsBookedForTomorrow() {
        dose(1L, TODAY.minusDays(200));
        dose(2L, TODAY.minusDays(10));

        CareSchedule milestone = service.upsertMilestone(HORSE, "preventive:11", ACTOR, null).orElseThrow();

        assertEquals(TODAY.plusDays(1).atTime(14, 0), milestone.getRequestedAt());
    }

    @Test
    void missingProtocolsAreIgnoredUntilACallerGivesThemADueDate() {
        dose(1L, TODAY);                       // Tetanus MISSING

        CareSchedule duringAdmission = service.upsertMilestone(HORSE, "preventive:12", ACTOR, null).orElseThrow();
        assertEquals(TODAY.plusDays(180).atTime(14, 0), duringAdmission.getRequestedAt());

        CareSchedule afterApproval = service.upsertMilestone(HORSE, "preventive:approval:3", ACTOR,
                TODAY.plusDays(1)).orElseThrow();
        assertEquals(TODAY.plusDays(1).atTime(14, 0), afterApproval.getRequestedAt());
        assertTrue(afterApproval.getDescription().contains("no record"));
    }

    @Test
    void nothingToBookWhenEveryMandatoryProtocolIsMissingAndIgnored() {
        assertTrue(service.upsertMilestone(HORSE, "preventive:13", ACTOR, null).isEmpty());
        verify(schedules, never()).save(any());
    }

    @Test
    void movesAnExistingRequestedMilestoneInsteadOfCreatingAnother() {
        CareSchedule existing = milestone(CareScheduleStatus.REQUESTED, TODAY.plusDays(3).atTime(14, 0), null);
        when(schedules.findFirstByHorseIdAndPurposeAndStatusIn(eq(HORSE), eq(CareSchedulePurpose.PREVENTIVE), any()))
                .thenReturn(Optional.of(existing));
        dose(1L, TODAY);
        dose(2L, TODAY);

        CareSchedule result = service.upsertMilestone(HORSE, "preventive:14", ACTOR, null).orElseThrow();

        assertSame(existing, result);
        assertEquals(TODAY.plusDays(180).atTime(14, 0), existing.getRequestedAt());
        verify(schedules, times(1)).save(existing);
        verify(schedules, never()).saveAndFlush(any());
    }

    @Test
    void cancelsScheduledMilestoneWhenNothingIsDueAnymoreThenBooksTheNextOne() {
        CareSchedule booked = milestone(CareScheduleStatus.SCHEDULED, TODAY.plusDays(4).atTime(9, 0), 21L);
        when(schedules.findFirstByHorseIdAndPurposeAndStatusIn(eq(HORSE), eq(CareSchedulePurpose.PREVENTIVE), any()))
                .thenReturn(Optional.of(booked));
        dose(1L, TODAY);                       // vừa tiêm gộp trong ca ROUTINE hôm nay
        dose(2L, TODAY);

        CareSchedule next = service.upsertMilestone(HORSE, "preventive:15", ACTOR, null).orElseThrow();

        assertEquals(CareScheduleStatus.CANCELLED, booked.getStatus());
        assertEquals("Vaccinated early in a combined visit on 18/10", booked.getCancelReason());
        assertNotSame(booked, next);
        assertEquals(CareScheduleStatus.REQUESTED, next.getStatus());
        assertEquals(TODAY.plusDays(180).atTime(14, 0), next.getRequestedAt());
        // Mốc cũ phải được flush trước khi INSERT mốc mới, nếu không partial unique index sẽ chặn.
        InOrder order = inOrder(schedules);
        order.verify(schedules).saveAndFlush(booked);
        order.verify(schedules).save(next);
    }

    @Test
    void keepsScheduledMilestoneThatStillHasWorkInsideTheWindow() {
        CareSchedule booked = milestone(CareScheduleStatus.SCHEDULED, TODAY.plusDays(4).atTime(9, 0), 21L);
        when(schedules.findFirstByHorseIdAndPurposeAndStatusIn(eq(HORSE), eq(CareSchedulePurpose.PREVENTIVE), any()))
                .thenReturn(Optional.of(booked));
        dose(1L, TODAY);
        dose(2L, TODAY.minusDays(355));        // Tetanus vẫn đến hạn sau 10 ngày, trong cửa sổ 4 + 7

        CareSchedule result = service.upsertMilestone(HORSE, "preventive:16", ACTOR, null).orElseThrow();

        assertSame(booked, result);
        assertEquals(CareScheduleStatus.SCHEDULED, booked.getStatus());
        assertEquals(21L, booked.getVeterinarianId());
        assertEquals(TODAY.plusDays(4).atTime(9, 0), booked.getScheduledAt());
        // The slot stays; only the list of what is still due is refreshed (EI was just given).
        assertTrue(booked.getDescription().contains("TETANUS"));
        assertFalse(booked.getDescription().contains("EI"));
        verify(schedules).save(booked);
        verify(schedules, never()).saveAndFlush(any());
    }

    @Test
    void leavesInProgressMilestoneToTheExamThatOwnsIt() {
        CareSchedule running = milestone(CareScheduleStatus.IN_PROGRESS, TODAY.atTime(8, 0), 21L);
        when(schedules.findFirstByHorseIdAndPurposeAndStatusIn(eq(HORSE), eq(CareSchedulePurpose.PREVENTIVE), any()))
                .thenReturn(Optional.of(running));
        dose(1L, TODAY);
        dose(2L, TODAY);

        assertSame(running, service.upsertMilestone(HORSE, "preventive:17", ACTOR, null).orElseThrow());
        verify(schedules, never()).save(any());
    }

    @Test
    void replayedOperationReturnsTheMilestoneItAlreadyCreated() {
        CareSchedule earlier = milestone(CareScheduleStatus.COMPLETED, TODAY.atTime(14, 0), 21L);
        when(schedules.findByRequestedByIdAndIdempotencyKey(ACTOR, "preventive:18")).thenReturn(Optional.of(earlier));
        dose(1L, TODAY);
        dose(2L, TODAY);

        assertSame(earlier, service.upsertMilestone(HORSE, "preventive:18", ACTOR, null).orElseThrow());
        verify(schedules, never()).save(any());
    }

    @Test
    void rejectedHorseNeverGetsAMilestone() {
        Horse rejected = new Horse();
        rejected.setId(HORSE);
        rejected.setCurrentStatus(HorseStatus.REJECTED);
        when(horses.findByIdForUpdate(HORSE)).thenReturn(Optional.of(rejected));
        dose(1L, TODAY.minusDays(200));

        assertTrue(service.upsertMilestone(HORSE, "preventive:19", ACTOR, TODAY).isEmpty());
        verify(schedules, never()).save(any());
    }

    @Test
    void blockedFollowUpAndPreventiveMilestoneUseDifferentIdempotencyKeys() {
        // Ca BLOCKED tạo nextSchedule với key "completion:<scheduleId>"; mốc tiêm dùng key riêng nên không đụng nhau.
        dose(1L, TODAY);
        dose(2L, TODAY);

        service.upsertMilestone(HORSE, "preventive:20", ACTOR, null);

        ArgumentCaptor<CareSchedule> saved = ArgumentCaptor.forClass(CareSchedule.class);
        verify(schedules).save(saved.capture());
        assertFalse(saved.getValue().getIdempotencyKey().startsWith("completion:"));
        verify(schedules).findByRequestedByIdAndIdempotencyKey(ACTOR, "preventive:20");
    }

    @Test
    void newMilestoneIsLinkedToTheHorsesLatestLiveAdmissionSoTheVetQueueCanOpenIt() {
        var rejected = new com.rtms.backend.admission.entity.AdmissionApplication();
        rejected.setId(8L);
        rejected.setStatus(com.rtms.backend.admission.enums.AdmissionStatus.REJECTED);
        var approved = new com.rtms.backend.admission.entity.AdmissionApplication();
        approved.setId(5L);
        approved.setStatus(com.rtms.backend.admission.enums.AdmissionStatus.APPROVED);
        when(admissions.findByHorseIdOrderBySubmittedAtDesc(HORSE)).thenReturn(List.of(rejected, approved));
        dose(1L, TODAY);
        dose(2L, TODAY);

        CareSchedule milestone = service.upsertMilestone(HORSE, "preventive:40", ACTOR, null).orElseThrow();

        assertEquals(5L, milestone.getAdmissionId());
    }

    // ---- deferMilestone ----

    @Test
    void deferralBooksTheNewMilestoneOnTheDeferredDayWithAReason() {
        dose(1L, TODAY.minusDays(200));        // EI quá hạn
        dose(2L, TODAY);

        CareSchedule milestone = service.deferMilestone(HORSE, "preventive:30", ACTOR, TODAY.plusDays(8)).orElseThrow();

        assertEquals(TODAY.plusDays(8).atTime(14, 0), milestone.getRequestedAt());
        assertEquals(com.rtms.backend.medical.enums.RescheduleReason.UNDER_TREATMENT, milestone.getRescheduleReason());
        assertEquals(CareScheduleStatus.REQUESTED, milestone.getStatus());
    }

    @Test
    void deferralLeavesAnAlreadyBookedMilestoneAlone() {
        CareSchedule booked = milestone(CareScheduleStatus.SCHEDULED, TODAY.plusDays(2).atTime(9, 0), 21L);
        when(schedules.findFirstByHorseIdAndPurposeAndStatusIn(eq(HORSE), eq(CareSchedulePurpose.PREVENTIVE), any()))
                .thenReturn(Optional.of(booked));
        dose(1L, TODAY.minusDays(200));
        dose(2L, TODAY);

        service.deferMilestone(HORSE, "preventive:31", ACTOR, TODAY.plusDays(8));

        assertEquals(TODAY.plusDays(2).atTime(9, 0), booked.getScheduledAt());
        assertNull(booked.getRescheduleReason());
    }

    // ---- helpers ----

    private void dose(long protocolId, LocalDate on) {
        doses.add(new LastPreventiveDose() {
            public Long getHorseId() { return HORSE; }
            public Long getProtocolId() { return protocolId; }
            public LocalDate getLastDoseOn() { return on; }
        });
    }

    private static Map<Long, PreventiveDueStatus> byProtocol(List<PreventiveProtocolStatus> statuses) {
        Map<Long, PreventiveDueStatus> map = new java.util.HashMap<>();
        statuses.forEach(status -> map.put(status.protocolId(), status.status()));
        return map;
    }

    private static CareSchedule milestone(CareScheduleStatus status, LocalDateTime at, Long vetId) {
        CareSchedule schedule = new CareSchedule();
        schedule.setId(99L);
        schedule.setHorseId(HORSE);
        schedule.setCareType(CareType.ROUTINE);
        schedule.setPurpose(CareSchedulePurpose.PREVENTIVE);
        schedule.setStatus(status);
        schedule.setRequestedAt(at);
        schedule.setScheduledAt(vetId == null ? null : at);
        schedule.setVeterinarianId(vetId);
        return schedule;
    }

    private static PreventiveProtocol protocol(long id, String code, int interval, int warning, boolean mandatory) {
        PreventiveProtocol protocol = new PreventiveProtocol();
        protocol.setId(id);
        protocol.setCode(code);
        protocol.setName(code);
        protocol.setCategory(ProtocolCategory.VACCINE);
        protocol.setIntervalDays(interval);
        protocol.setWarningDays(warning);
        protocol.setMandatory(mandatory);
        return protocol;
    }
}
