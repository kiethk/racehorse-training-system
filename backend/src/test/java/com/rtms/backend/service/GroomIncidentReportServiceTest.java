package com.rtms.backend.service;

import com.rtms.backend.dto.HandleIncidentRequest;
import com.rtms.backend.entity.GroomIncidentReport;
import com.rtms.backend.enums.IncidentStatus;
import com.rtms.backend.repository.GroomIncidentReportRepository;
import com.rtms.backend.repository.HorseRepository;
import com.rtms.backend.security.AuthenticatedUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GroomIncidentReportServiceTest {

    @Mock
    private GroomIncidentReportRepository incidentReportRepository;

    @Mock
    private HorseRepository horseRepository;

    private GroomIncidentReportService incidentReportService;

    @BeforeEach
    void setUp() {
        incidentReportService = new GroomIncidentReportService(incidentReportRepository, horseRepository);
    }

    @Test
    @DisplayName("Vòng đời: REPORTED -> IN_REVIEW (Vet tiếp nhận sự cố)")
    void testHandleReport_ReportedToInReview_Success() {
        GroomIncidentReport report = new GroomIncidentReport();
        report.setId(1L);
        report.setStatus(IncidentStatus.REPORTED);

        when(incidentReportRepository.findById(1L)).thenReturn(Optional.of(report));
        when(incidentReportRepository.save(any(GroomIncidentReport.class))).thenAnswer(inv -> inv.getArgument(0));

        AuthenticatedUser vetUser = new AuthenticatedUser(100L, "vet@example.com", "VETERINARIAN");

        HandleIncidentRequest req = new HandleIncidentRequest();
        req.setStatus(IncidentStatus.IN_REVIEW);
        req.setHandlerNote("Đang trên đường đến chuồng kiểm tra");

        GroomIncidentReport result = incidentReportService.handleReport(1L, req, vetUser);

        assertEquals(IncidentStatus.IN_REVIEW, result.getStatus());
        assertEquals(100L, result.getHandledById());
        assertEquals("Đang trên đường đến chuồng kiểm tra", result.getHandlerNote());
        assertNotNull(result.getHandledAt());
    }

    @Test
    @DisplayName("Vòng đời: IN_REVIEW -> RESOLVED (Khám xong, đã xử lý)")
    void testHandleReport_InReviewToResolved_Success() {
        GroomIncidentReport report = new GroomIncidentReport();
        report.setId(1L);
        report.setStatus(IncidentStatus.IN_REVIEW);

        when(incidentReportRepository.findById(1L)).thenReturn(Optional.of(report));
        when(incidentReportRepository.save(any(GroomIncidentReport.class))).thenAnswer(inv -> inv.getArgument(0));

        AuthenticatedUser vetUser = new AuthenticatedUser(100L, "vet@example.com", "VETERINARIAN");

        HandleIncidentRequest req = new HandleIncidentRequest();
        req.setStatus(IncidentStatus.RESOLVED);
        req.setHandlerNote("Đã băng bó và lập bệnh án");

        GroomIncidentReport result = incidentReportService.handleReport(1L, req, vetUser);

        assertEquals(IncidentStatus.RESOLVED, result.getStatus());
        assertEquals(100L, result.getHandledById());
    }

    @Test
    @DisplayName("Vòng đời: Chặn chuyển trạng thái bất hợp lệ (REPORTED -> RESOLVED nhảy cóc)")
    void testHandleReport_InvalidTransition_ThrowsException() {
        GroomIncidentReport report = new GroomIncidentReport();
        report.setId(1L);
        report.setStatus(IncidentStatus.REPORTED);

        when(incidentReportRepository.findById(1L)).thenReturn(Optional.of(report));

        AuthenticatedUser vetUser = new AuthenticatedUser(100L, "vet@example.com", "VETERINARIAN");

        HandleIncidentRequest req = new HandleIncidentRequest();
        req.setStatus(IncidentStatus.RESOLVED); // Nhảy cóc mà chưa qua IN_REVIEW

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> incidentReportService.handleReport(1L, req, vetUser));

        assertTrue(ex.getMessage().contains("Không thể chuyển báo cáo từ REPORTED sang RESOLVED"));
        verify(incidentReportRepository, never()).save(any());
    }

    @Test
    @DisplayName("Lọc danh sách báo cáo theo trạng thái IncidentStatus")
    void testGetReports_FilterByStatus() {
        GroomIncidentReport r1 = new GroomIncidentReport();
        r1.setStatus(IncidentStatus.REPORTED);

        GroomIncidentReport r2 = new GroomIncidentReport();
        r2.setStatus(IncidentStatus.IN_REVIEW);

        when(incidentReportRepository.findAllByOrderByReportedAtDesc()).thenReturn(List.of(r1, r2));

        AuthenticatedUser manager = new AuthenticatedUser(1L, "mgr@example.com", "CLUB_MANAGER");

        List<GroomIncidentReport> reportedOnly = incidentReportService.getReports(null, IncidentStatus.REPORTED, manager);
        assertEquals(1, reportedOnly.size());
        assertEquals(IncidentStatus.REPORTED, reportedOnly.get(0).getStatus());
    }

    @Test
    @DisplayName("Groom truyền horseId chỉ xem được báo cáo do chính mình tạo")
    void testGetReports_GroomRoleWithHorseId_OnlyReturnsOwnReports() {
        GroomIncidentReport r1 = new GroomIncidentReport();
        r1.setId(1L);
        r1.setHorseId(10L);
        r1.setGroomId(5L); // Groom hiện tại

        GroomIncidentReport r2 = new GroomIncidentReport();
        r2.setId(2L);
        r2.setHorseId(10L);
        r2.setGroomId(9L); // Groom khác

        when(incidentReportRepository.findByHorseId(10L)).thenReturn(List.of(r1, r2));

        AuthenticatedUser groom = new AuthenticatedUser(5L, "groom@example.com", "GROOM");

        List<GroomIncidentReport> results = incidentReportService.getReports(10L, null, groom);
        assertEquals(1, results.size());
        assertEquals(5L, results.get(0).getGroomId());
    }
}