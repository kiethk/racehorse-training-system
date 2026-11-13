package com.rtms.backend.incident.service;

import com.rtms.backend.common.storage.FileStorageService;
import com.rtms.backend.config.ApiException;
import com.rtms.backend.horse.entity.Horse;
import com.rtms.backend.horse.repository.HorseRepository;
import com.rtms.backend.identity.repository.UserRepository;
import com.rtms.backend.incident.dto.CreateIncidentReportRequest;
import com.rtms.backend.incident.entity.HorseIncidentReport;
import com.rtms.backend.incident.enums.IncidentType;
import com.rtms.backend.incident.enums.IncidentStatus;
import com.rtms.backend.incident.repository.HorseIncidentReportRepository;
import com.rtms.backend.medical.dto.CareScheduleResponse;
import com.rtms.backend.medical.entity.CareSchedule;
import com.rtms.backend.medical.enums.CareScheduleStatus;
import com.rtms.backend.medical.enums.CareType;
import com.rtms.backend.medical.repository.CareScheduleRepository;
import com.rtms.backend.medical.service.CareScheduleService;
import com.rtms.backend.security.AuthenticatedUser;
import com.rtms.backend.stable.repository.StableStallRepository;
import com.rtms.backend.training.enums.TrainingDecision;
import com.rtms.backend.training.service.TrainingDecisionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Luật báo cáo sự cố: mức độ quyết định ca khám, mỗi ngựa một ca sự cố, và ai được báo hoặc xem gì. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class HorseIncidentReportRoutingTest {

    private static final long HORSE_ID = 5L;
    private static final long STALL_ID = 50L;
    private static final long GROOM_ID = 10L;
    private static final long TRAINER_ID = 11L;
    private static final long VET_ID = 20L;

    @Mock HorseIncidentReportRepository reports;
    @Mock HorseRepository horses;
    @Mock FileStorageService files;
    @Mock CareScheduleRepository schedules;
    @Mock CareScheduleService careScheduleService;
    @Mock TrainingDecisionService trainingDecisions;
    @Mock UserRepository users;
    @Mock StableStallRepository stalls;

    private final AuthenticatedUser groom = new AuthenticatedUser(GROOM_ID, "groom@rtms.com", "GROOM");
    private final AuthenticatedUser trainer = new AuthenticatedUser(TRAINER_ID, "trainer@rtms.com", "HEAD_TRAINER");

    private HorseIncidentReportService service;
    private Horse horse;
    /** Mô phỏng bảng báo cáo: báo cáo đã lưu, tìm lại theo ca bằng careScheduleId. */
    private final List<HorseIncidentReport> stored = new ArrayList<>();

    @BeforeEach
    void setUp() {
        service = new HorseIncidentReportService(reports, horses, files, schedules, careScheduleService,
                trainingDecisions, users, stalls);

        horse = new Horse();
        horse.setId(HORSE_ID);
        horse.setCurrentStallId(STALL_ID);
        horse.setTrainingDecision(TrainingDecision.ALLOWED);
        when(horses.findByIdForUpdate(HORSE_ID)).thenReturn(Optional.of(horse));
        when(stalls.existsByIdAndGroomId(STALL_ID, GROOM_ID)).thenReturn(true);
        when(horses.findTrainerIdByHorseId(HORSE_ID)).thenReturn(Optional.of(TRAINER_ID));
        when(careScheduleService.findOpenIncidentCase(HORSE_ID)).thenReturn(Optional.empty());

        when(reports.save(any(HorseIncidentReport.class))).thenAnswer(call -> {
            HorseIncidentReport report = call.getArgument(0);
            if (report.getId() == null) {
                report.setId((long) stored.size() + 1);
                stored.add(report);
            }
            return report;
        });
        when(reports.findByCareScheduleIdOrderByReportedAtAsc(anyLong())).thenAnswer(call -> stored.stream()
                .filter(report -> call.getArgument(0).equals(report.getCareScheduleId()))
                .toList());
    }

    @Test
    @DisplayName("LOW và MEDIUM tạo ca ROUTINE, ngựa vẫn tập")
    void minorReportOpensARoutineCaseAndKeepsTraining() {
        CareSchedule created = stubCreatedCase(CareType.ROUTINE, 100L, null);

        HorseIncidentReport report = service.createReport(request(IncidentType.PROBLEM), groom);

        assertEquals(created.getId(), report.getCareScheduleId());
        assertEquals(IncidentStatus.REPORTED, report.getStatus());
        verify(trainingDecisions, never()).block(any(), anyString());
    }

    @Test
    @DisplayName("HIGH tạo ca URGENT nhưng không tự tiện chặn tập (quyền thuộc về Vet khi khám lâm sàng)")
    void seriousReportOpensAnUrgentCaseWithoutBlockingTraining() {
        stubCreatedCase(CareType.URGENT, 100L, VET_ID);

        HorseIncidentReport report = service.createReport(request(IncidentType.TRAUMA), groom);

        assertEquals(IncidentStatus.IN_REVIEW, report.getStatus());
        assertEquals(VET_ID, report.getHandledById());
        verify(trainingDecisions, never()).block(any(), anyString());
    }

    @Test
    @DisplayName("Ngựa đã bị Vet chặn: báo cáo HIGH không ghi đè lý do của Vet")
    void aBlockedHorseKeepsTheVetsReason() {
        horse.setTrainingDecision(TrainingDecision.BLOCKED);
        stubCreatedCase(CareType.URGENT, 100L, VET_ID);

        service.createReport(request(IncidentType.TRAUMA), groom);

        verify(trainingDecisions, never()).block(any(), anyString());
    }

    @Test
    @DisplayName("LOW gắn vào ca ROUTINE sự cố đang mở, không tạo ca thứ hai")
    void minorReportJoinsTheOpenRoutineCase() {
        CareSchedule open = schedule(100L, CareType.ROUTINE, CareScheduleStatus.SCHEDULED, VET_ID);
        when(careScheduleService.findOpenIncidentCase(HORSE_ID)).thenReturn(Optional.of(open));

        HorseIncidentReport report = service.createReport(request(IncidentType.PROBLEM), groom);

        assertEquals(100L, report.getCareScheduleId());
        assertEquals(IncidentStatus.IN_REVIEW, report.getStatus());
        verify(careScheduleService, never()).createSchedule(anyLong(), any(), any(), any(), anyLong(), any());
    }

    @Test
    @DisplayName("HIGH khi ca ROUTINE chưa khám: tạm dừng ca đó (giữ Vet), báo cáo cũ sang ca URGENT mới")
    void seriousReportPausesTheRoutineCase() {
        CareSchedule routine = schedule(100L, CareType.ROUTINE, CareScheduleStatus.SCHEDULED, VET_ID);
        when(careScheduleService.findOpenIncidentCase(HORSE_ID)).thenReturn(Optional.of(routine));
        HorseIncidentReport earlier = storedReport(routine, IncidentStatus.IN_REVIEW, VET_ID);
        stubCreatedCase(CareType.URGENT, 200L, 21L);

        HorseIncidentReport report = service.createReport(request(IncidentType.TRAUMA), groom);

        verify(careScheduleService).interruptForUrgent(routine);
        verify(careScheduleService).createSchedule(eq(HORSE_ID), eq(CareType.URGENT), any(), isNull(), anyLong(), eq(100L));
        assertEquals(200L, report.getCareScheduleId());
        assertEquals(200L, earlier.getCareScheduleId());
        assertEquals(21L, earlier.getHandledById());
        verify(trainingDecisions, never()).block(any(), anyString());
    }

    @Test
    @DisplayName("HIGH khi Vet đang khám ca ROUTINE: vẫn tách ca URGENT, ca Routine giữ nguyên để khám tiếp")
    void seriousReportPausesARoutineExamInProgress() {
        CareSchedule routine = schedule(100L, CareType.ROUTINE, CareScheduleStatus.IN_PROGRESS, VET_ID);
        when(careScheduleService.findOpenIncidentCase(HORSE_ID)).thenReturn(Optional.of(routine));
        HorseIncidentReport earlier = storedReport(routine, IncidentStatus.IN_REVIEW, VET_ID);
        stubCreatedCase(CareType.URGENT, 200L, 21L);

        HorseIncidentReport report = service.createReport(request(IncidentType.TRAUMA), groom);

        verify(careScheduleService).interruptForUrgent(routine);
        assertEquals(200L, report.getCareScheduleId());
        assertEquals(200L, earlier.getCareScheduleId());
        verify(trainingDecisions, never()).block(any(), anyString());
    }

    @Test
    @DisplayName("CRITICAL vào ca URGENT đang mở: chỉ gắn vào, không tạo ca thứ hai")
    void criticalReportJoinsTheOpenUrgentCase() {
        CareSchedule urgent = schedule(100L, CareType.URGENT, CareScheduleStatus.SCHEDULED, VET_ID);
        when(careScheduleService.findOpenIncidentCase(HORSE_ID)).thenReturn(Optional.of(urgent));

        HorseIncidentReport report = service.createReport(request(IncidentType.TRAUMA), groom);

        assertEquals(100L, report.getCareScheduleId());
        verify(careScheduleService, never()).createSchedule(anyLong(), any(), any(), any(), anyLong(), any());
    }

    @Test
    @DisplayName("Ngựa không ở chuồng nào (INACTIVE, REJECTED) thì không báo được")
    void aHorseWithoutAStallCannotBeReported() {
        horse.setCurrentStallId(null);

        ApiException error = assertThrows(ApiException.class,
                () -> service.createReport(request(IncidentType.TRAUMA), groom));

        assertEquals("HORSE_NOT_IN_STALL", error.getErrorCode());
        verify(reports, never()).save(any());
    }

    @Test
    @DisplayName("Groom chỉ báo ngựa trong chuồng mình")
    void groomCannotReportAHorseOutsideTheirStalls() {
        when(stalls.existsByIdAndGroomId(STALL_ID, GROOM_ID)).thenReturn(false);

        assertThrows(AccessDeniedException.class, () -> service.createReport(request(IncidentType.PROBLEM), groom));
    }

    @Test
    @DisplayName("Trainer chỉ báo ngựa thuộc khu mình")
    void trainerCannotReportAHorseOutsideTheirAreas() {
        when(horses.findTrainerIdByHorseId(HORSE_ID)).thenReturn(Optional.of(99L));

        assertThrows(AccessDeniedException.class, () -> service.createReport(request(IncidentType.PROBLEM), trainer));
    }

    @Test
    @DisplayName("Trainer không xem được báo cáo của ngựa khu khác do người khác gửi")
    void trainerCannotReadAReportOutsideTheirAreas() {
        HorseIncidentReport report = new HorseIncidentReport(GROOM_ID, HORSE_ID, "Lame", "Limping", IncidentType.PROBLEM);
        report.setId(1L);
        when(reports.findById(1L)).thenReturn(Optional.of(report));
        when(horses.findTrainerIdByHorseId(HORSE_ID)).thenReturn(Optional.of(99L));

        assertThrows(AccessDeniedException.class, () -> service.getReportById(1L, trainer));
    }

    @Test
    @DisplayName("Danh sách của Trainer chỉ lấy phần được xem, lọc thêm theo ngựa nếu có")
    void trainerListIsScopedAndFilteredByHorse() {
        HorseIncidentReport mine = new HorseIncidentReport(GROOM_ID, HORSE_ID, "Lame", "Limping", IncidentType.PROBLEM);
        HorseIncidentReport other = new HorseIncidentReport(TRAINER_ID, 6L, "Cough", "Coughing", IncidentType.PROBLEM);
        when(reports.findVisibleToTrainer(TRAINER_ID)).thenReturn(List.of(mine, other));

        assertEquals(List.of(mine), service.getReports(HORSE_ID, null, trainer));
        verify(reports, never()).findAll();
    }

    @Test
    @DisplayName("Báo cáo trả về tên ngựa, kể cả ngựa đã rời chuồng của Groom")
    void reportsCarryTheHorseName() {
        HorseIncidentReport report = new HorseIncidentReport(GROOM_ID, HORSE_ID, "Lame", "Limping", IncidentType.PROBLEM);
        horse.setName("IMPERIUM");
        when(reports.findByReportedById(GROOM_ID)).thenReturn(List.of(report));
        when(horses.findAllById(any())).thenReturn(List.of(horse));

        assertEquals("IMPERIUM", service.getReports(null, null, groom).get(0).getHorseName());
    }

    @Test
    @DisplayName("Chỉ người gửi mới đổi được ảnh")
    void onlyTheReporterCanChangeTheImage() {
        HorseIncidentReport report = new HorseIncidentReport(GROOM_ID, HORSE_ID, "Lame", "Limping", IncidentType.PROBLEM);
        when(reports.findById(1L)).thenReturn(Optional.of(report));

        assertThrows(AccessDeniedException.class, () -> service.attachImage(1L, image(), trainer));
    }

    @Test
    @DisplayName("Báo cáo đã đóng thì không đổi ảnh được nữa")
    void aClosedReportKeepsItsImage() {
        HorseIncidentReport report = new HorseIncidentReport(GROOM_ID, HORSE_ID, "Lame", "Limping", IncidentType.PROBLEM);
        report.setStatus(IncidentStatus.RESOLVED);
        when(reports.findById(1L)).thenReturn(Optional.of(report));

        ApiException error = assertThrows(ApiException.class, () -> service.attachImage(1L, image(), groom));

        assertEquals("INCIDENT_CLOSED", error.getErrorCode());
        verify(files, never()).store(any(), anyString());
    }

    @Test
    @DisplayName("Ca chưa có Vet: báo cáo gắn vào vẫn ở REPORTED")
    void aReportOnACaseWithoutVetStaysReported() {
        CareSchedule urgent = schedule(100L, CareType.URGENT, CareScheduleStatus.SCHEDULED, null);
        when(careScheduleService.findOpenIncidentCase(HORSE_ID)).thenReturn(Optional.of(urgent));

        HorseIncidentReport report = service.createReport(request(IncidentType.PROBLEM), groom);

        assertEquals(IncidentStatus.REPORTED, report.getStatus());
        assertNull(report.getHandledById());
    }

    private CareSchedule stubCreatedCase(CareType type, long id, Long vetId) {
        CareSchedule created = schedule(id, type, CareScheduleStatus.SCHEDULED, vetId);
        CareScheduleResponse response = mock(CareScheduleResponse.class);
        when(response.id()).thenReturn(id);
        when(careScheduleService.createSchedule(eq(HORSE_ID), eq(type), anyString(), isNull(), anyLong(), any())).thenReturn(response);
        when(schedules.findById(id)).thenReturn(Optional.of(created));
        return created;
    }

    private HorseIncidentReport storedReport(CareSchedule incidentCase, IncidentStatus status, Long vetId) {
        HorseIncidentReport report = new HorseIncidentReport(GROOM_ID, HORSE_ID, "Scratch", "Small scratch", IncidentType.PROBLEM);
        report.setId((long) stored.size() + 1);
        report.setCareScheduleId(incidentCase.getId());
        report.setStatus(status);
        report.setHandledById(vetId);
        stored.add(report);
        return report;
    }

    private static CareSchedule schedule(long id, CareType type, CareScheduleStatus status, Long vetId) {
        CareSchedule schedule = new CareSchedule();
        schedule.setId(id);
        schedule.setHorseId(HORSE_ID);
        schedule.setCareType(type);
        schedule.setStatus(status);
        schedule.setVeterinarianId(vetId);
        return schedule;
    }

    private static CreateIncidentReportRequest request(IncidentType severity) {
        CreateIncidentReportRequest request = new CreateIncidentReportRequest();
        request.setHorseId(HORSE_ID);
        request.setTitle("Lame");
        request.setDescription("Limping on the right front leg");
        request.setIncidentType(severity);
        return request;
    }

    private static MockMultipartFile image() {
        return new MockMultipartFile("file", "a.png", "image/png", new byte[]{1});
    }
}
