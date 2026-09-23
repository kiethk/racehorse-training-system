package com.rtms.backend.service;

import com.rtms.backend.dto.ManagerReviewRequest;
import com.rtms.backend.entity.AdmissionApplication;
import com.rtms.backend.entity.Horse;
import com.rtms.backend.entity.PreventiveCareSchedule;
import com.rtms.backend.entity.StableStall;
import com.rtms.backend.enums.AdmissionStatus;
import com.rtms.backend.enums.HorseStatus;
import com.rtms.backend.enums.ReviewDecision;
import com.rtms.backend.enums.StallStatus;
import com.rtms.backend.repository.AdmissionApplicationRepository;
import com.rtms.backend.repository.HorseRepository;
import com.rtms.backend.repository.PreventiveCareScheduleRepository;
import com.rtms.backend.repository.StableStallRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdmissionManagerReviewServiceTest {

    @Mock
    private AdmissionApplicationRepository admissionApplicationRepository;

    @Mock
    private StableStallRepository stableStallRepository;

    @Mock
    private HorseRepository horseRepository;

    @Mock
    private PreventiveCareScheduleRepository preventiveCareScheduleRepository;

    private AdmissionManagerReviewService service;

    // ─── Shared helpers ───────────────────────────────────────────────────────

    private AdmissionApplication buildAdmission(AdmissionStatus status) {
        AdmissionApplication a = new AdmissionApplication();
        a.setId(1L);
        a.setStatus(status);
        a.setHorseId(10L);
        a.setQuarantineStallId(99L);
        return a;
    }

    private Horse buildHorse(HorseStatus status) {
        Horse h = new Horse();
        h.setId(10L);
        h.setCurrentStatus(status);
        h.setCurrentStallId(99L);
        return h;
    }

    private StableStall buildStall(Long id, StallStatus status) {
        StableStall s = new StableStall();
        s.setId(id);
        s.setStatus(status);
        return s;
    }

    private ManagerReviewRequest approveRequest(Long stallId) {
        ManagerReviewRequest r = new ManagerReviewRequest();
        r.setDecision(ReviewDecision.APPROVED);
        r.setStallId(stallId);
        return r;
    }

    private ManagerReviewRequest rejectRequest(String feedback) {
        ManagerReviewRequest r = new ManagerReviewRequest();
        r.setDecision(ReviewDecision.REJECTED);
        r.setFeedback(feedback);
        return r;
    }

    @BeforeEach
    void setUp() {
        service = new AdmissionManagerReviewService(
                admissionApplicationRepository,
                stableStallRepository,
                horseRepository,
                preventiveCareScheduleRepository);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // APPROVE — manual stall selection
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("APPROVE thủ công: Admission=APPROVED, Horse=ELIGIBLE, chuồng thường=OCCUPIED, chuồng Q=AVAILABLE")
    void approve_withSelectedStall_success() {
        AdmissionApplication admission = buildAdmission(AdmissionStatus.MANAGER_REVIEW);
        Horse horse = buildHorse(HorseStatus.CANDIDATE);
        StableStall regularStall = buildStall(20L, StallStatus.AVAILABLE);
        StableStall qStall = buildStall(99L, StallStatus.OCCUPIED);

        when(admissionApplicationRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(admission));
        when(horseRepository.findById(10L)).thenReturn(Optional.of(horse));
        when(stableStallRepository.findAvailableRegularStallByIdForUpdate(20L)).thenReturn(Optional.of(regularStall));
        when(stableStallRepository.findById(99L)).thenReturn(Optional.of(qStall));
        when(admissionApplicationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        AdmissionApplication result = service.review(1L, 5L, approveRequest(20L));

        assertEquals(AdmissionStatus.APPROVED, result.getStatus());
        assertEquals(ReviewDecision.APPROVED, result.getManagerDecision());
        assertEquals(5L, result.getManagerId());
        assertNotNull(result.getManagerReviewedAt());

        assertEquals(HorseStatus.ELIGIBLE, horse.getCurrentStatus());
        assertEquals(20L, horse.getCurrentStallId());
        verify(horseRepository).save(horse);

        assertEquals(StallStatus.OCCUPIED, regularStall.getStatus());
        verify(stableStallRepository).save(regularStall);

        assertEquals(StallStatus.AVAILABLE, qStall.getStatus());
        verify(stableStallRepository).save(qStall);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // APPROVE — automatic stall selection (no stallId in request)
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("APPROVE tự động: không truyền stallId, hệ thống tự chọn chuồng REGULAR đầu tiên")
    void approve_withAutoStall_success() {
        AdmissionApplication admission = buildAdmission(AdmissionStatus.MANAGER_REVIEW);
        Horse horse = buildHorse(HorseStatus.CANDIDATE);
        StableStall regularStall = buildStall(21L, StallStatus.AVAILABLE);
        StableStall qStall = buildStall(99L, StallStatus.OCCUPIED);

        when(admissionApplicationRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(admission));
        when(horseRepository.findById(10L)).thenReturn(Optional.of(horse));
        when(stableStallRepository.findFirstAvailableRegularStallForUpdate()).thenReturn(Optional.of(regularStall));
        when(stableStallRepository.findById(99L)).thenReturn(Optional.of(qStall));
        when(admissionApplicationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        AdmissionApplication result = service.review(1L, 5L, approveRequest(null));

        assertEquals(AdmissionStatus.APPROVED, result.getStatus());
        assertEquals(HorseStatus.ELIGIBLE, horse.getCurrentStatus());
        assertEquals(21L, horse.getCurrentStallId());
        assertEquals(StallStatus.OCCUPIED, regularStall.getStatus());
        assertEquals(StallStatus.AVAILABLE, qStall.getStatus());
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // REJECT — full state verification
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("REJECT: Admission=REJECTED, Horse=REJECTED, currentStallId=null, chuồng Q=AVAILABLE")
    void reject_success_admissionAndHorseRejected() {
        AdmissionApplication admission = buildAdmission(AdmissionStatus.MANAGER_REVIEW);
        Horse horse = buildHorse(HorseStatus.CANDIDATE);
        StableStall qStall = buildStall(99L, StallStatus.OCCUPIED);

        when(admissionApplicationRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(admission));
        when(horseRepository.findById(10L)).thenReturn(Optional.of(horse));
        when(stableStallRepository.findById(99L)).thenReturn(Optional.of(qStall));
        when(preventiveCareScheduleRepository.findByHorseIdAndStatusIn(10L, List.of("PENDING", "OVERDUE")))
                .thenReturn(List.of());
        when(admissionApplicationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        AdmissionApplication result = service.review(1L, 5L, rejectRequest("Not suitable"));

        assertEquals(AdmissionStatus.REJECTED, result.getStatus());
        assertEquals(ReviewDecision.REJECTED, result.getManagerDecision());
        assertEquals("Not suitable", result.getManagerFeedback());
        assertEquals(5L, result.getManagerId());
        assertNotNull(result.getManagerReviewedAt());

        assertEquals(HorseStatus.REJECTED, horse.getCurrentStatus());
        assertNull(horse.getCurrentStallId());
        verify(horseRepository).save(horse);

        assertEquals(StallStatus.AVAILABLE, qStall.getStatus());
        verify(stableStallRepository).save(qStall);
    }

    @Test
    @DisplayName("REJECT: lịch PENDING và OVERDUE bị CANCELLED, lịch COMPLETED không bị ảnh hưởng")
    void reject_cancelsPendingAndOverdueSchedules_notCompleted() {
        AdmissionApplication admission = buildAdmission(AdmissionStatus.MANAGER_REVIEW);
        Horse horse = buildHorse(HorseStatus.CANDIDATE);
        StableStall qStall = buildStall(99L, StallStatus.OCCUPIED);

        PreventiveCareSchedule pending = new PreventiveCareSchedule();
        pending.setId(1L);
        pending.setHorseId(10L);
        pending.setStatus("PENDING");

        PreventiveCareSchedule overdue = new PreventiveCareSchedule();
        overdue.setId(2L);
        overdue.setHorseId(10L);
        overdue.setStatus("OVERDUE");

        when(admissionApplicationRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(admission));
        when(horseRepository.findById(10L)).thenReturn(Optional.of(horse));
        when(stableStallRepository.findById(99L)).thenReturn(Optional.of(qStall));
        when(preventiveCareScheduleRepository.findByHorseIdAndStatusIn(10L, List.of("PENDING", "OVERDUE")))
                .thenReturn(List.of(pending, overdue));
        when(admissionApplicationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        service.review(1L, 5L, rejectRequest("Rejected"));

        assertEquals("CANCELLED", pending.getStatus());
        assertEquals("CANCELLED", overdue.getStatus());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<PreventiveCareSchedule>> captor = ArgumentCaptor.forClass(List.class);
        verify(preventiveCareScheduleRepository).saveAll(captor.capture());
        List<PreventiveCareSchedule> saved = captor.getValue();
        assertEquals(2, saved.size());
        assertTrue(saved.stream().allMatch(s -> "CANCELLED".equals(s.getStatus())));

        // COMPLETED schedules are never queried in this call — no interference
        verify(preventiveCareScheduleRepository, never()).findByHorseIdOrderByScheduledDateAsc(any());
    }

    @Test
    @DisplayName("REJECT: không có lịch nào cần huỷ — saveAll vẫn được gọi với danh sách rỗng")
    void reject_noSchedulesToCancel_saveAllCalledWithEmptyList() {
        AdmissionApplication admission = buildAdmission(AdmissionStatus.MANAGER_REVIEW);
        Horse horse = buildHorse(HorseStatus.CANDIDATE);
        StableStall qStall = buildStall(99L, StallStatus.OCCUPIED);

        when(admissionApplicationRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(admission));
        when(horseRepository.findById(10L)).thenReturn(Optional.of(horse));
        when(stableStallRepository.findById(99L)).thenReturn(Optional.of(qStall));
        when(preventiveCareScheduleRepository.findByHorseIdAndStatusIn(10L, List.of("PENDING", "OVERDUE")))
                .thenReturn(List.of());
        when(admissionApplicationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        service.review(1L, 5L, rejectRequest("Rejected"));

        verify(preventiveCareScheduleRepository).saveAll(List.of());
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // REJECT — validation failures
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("REJECT thiếu feedback: ném IllegalArgumentException, không lưu gì")
    void reject_missingFeedback_throwsAndNoSave() {
        AdmissionApplication admission = buildAdmission(AdmissionStatus.MANAGER_REVIEW);
        Horse horse = buildHorse(HorseStatus.CANDIDATE);

        when(admissionApplicationRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(admission));
        when(horseRepository.findById(10L)).thenReturn(Optional.of(horse));

        ManagerReviewRequest req = rejectRequest(null);

        assertThrows(IllegalArgumentException.class,
                () -> service.review(1L, 5L, req));

        verify(admissionApplicationRepository, never()).save(any());
        verify(horseRepository, never()).save(any());
        verify(stableStallRepository, never()).save(any());
    }

    @Test
    @DisplayName("REJECT feedback rỗng (blank): ném IllegalArgumentException")
    void reject_blankFeedback_throws() {
        AdmissionApplication admission = buildAdmission(AdmissionStatus.MANAGER_REVIEW);
        Horse horse = buildHorse(HorseStatus.CANDIDATE);

        when(admissionApplicationRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(admission));
        when(horseRepository.findById(10L)).thenReturn(Optional.of(horse));

        assertThrows(IllegalArgumentException.class,
                () -> service.review(1L, 5L, rejectRequest("   ")));
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Admission state validation
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("Admission không ở trạng thái MANAGER_REVIEW: ném IllegalStateException")
    void review_admissionNotManagerReview_throws() {
        AdmissionApplication admission = buildAdmission(AdmissionStatus.VET_REVIEW);
        when(admissionApplicationRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(admission));

        assertThrows(IllegalStateException.class,
                () -> service.review(1L, 5L, approveRequest(20L)));

        verify(horseRepository, never()).findById(any());
        verify(admissionApplicationRepository, never()).save(any());
    }

    @Test
    @DisplayName("APPROVE sau khi Admission đã APPROVED: ném IllegalStateException (không cho review lần 2)")
    void review_alreadyApproved_throws() {
        AdmissionApplication admission = buildAdmission(AdmissionStatus.APPROVED);
        when(admissionApplicationRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(admission));

        assertThrows(IllegalStateException.class,
                () -> service.review(1L, 5L, approveRequest(20L)));
    }

    @Test
    @DisplayName("REJECT sau khi Admission đã REJECTED: ném IllegalStateException (không cho review lần 2)")
    void review_alreadyRejected_throws() {
        AdmissionApplication admission = buildAdmission(AdmissionStatus.REJECTED);
        when(admissionApplicationRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(admission));

        assertThrows(IllegalStateException.class,
                () -> service.review(1L, 5L, rejectRequest("Again")));
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Horse validation
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("horseId là null trong Admission: ném IllegalStateException")
    void review_horseIdNull_throws() {
        AdmissionApplication admission = buildAdmission(AdmissionStatus.MANAGER_REVIEW);
        admission.setHorseId(null);

        when(admissionApplicationRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(admission));

        assertThrows(IllegalStateException.class,
                () -> service.review(1L, 5L, approveRequest(20L)));

        verify(horseRepository, never()).findById(any());
    }

    @Test
    @DisplayName("Horse không tồn tại: ném IllegalStateException")
    void review_horseNotFound_throws() {
        AdmissionApplication admission = buildAdmission(AdmissionStatus.MANAGER_REVIEW);
        when(admissionApplicationRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(admission));
        when(horseRepository.findById(10L)).thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class,
                () -> service.review(1L, 5L, approveRequest(20L)));

        verify(admissionApplicationRepository, never()).save(any());
    }

    @Test
    @DisplayName("Horse không ở trạng thái CANDIDATE: ném IllegalStateException")
    void review_horseNotCandidate_throws() {
        AdmissionApplication admission = buildAdmission(AdmissionStatus.MANAGER_REVIEW);
        Horse horse = buildHorse(HorseStatus.ELIGIBLE); // Already past CANDIDATE

        when(admissionApplicationRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(admission));
        when(horseRepository.findById(10L)).thenReturn(Optional.of(horse));

        assertThrows(IllegalStateException.class,
                () -> service.review(1L, 5L, approveRequest(20L)));

        verify(admissionApplicationRepository, never()).save(any());
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Stall validation
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("Chuồng REGULAR được chọn thủ công không còn AVAILABLE: ném IllegalStateException, không lưu gì")
    void approve_selectedStallUnavailable_throwsAndNoSave() {
        AdmissionApplication admission = buildAdmission(AdmissionStatus.MANAGER_REVIEW);
        Horse horse = buildHorse(HorseStatus.CANDIDATE);

        when(admissionApplicationRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(admission));
        when(horseRepository.findById(10L)).thenReturn(Optional.of(horse));
        when(stableStallRepository.findAvailableRegularStallByIdForUpdate(20L)).thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class,
                () -> service.review(1L, 5L, approveRequest(20L)));

        verify(admissionApplicationRepository, never()).save(any());
        verify(horseRepository, never()).save(any());
    }

    @Test
    @DisplayName("Không có chuồng REGULAR nào AVAILABLE cho tự động chọn: ném IllegalStateException")
    void approve_noAvailableRegularStall_throws() {
        AdmissionApplication admission = buildAdmission(AdmissionStatus.MANAGER_REVIEW);
        Horse horse = buildHorse(HorseStatus.CANDIDATE);

        when(admissionApplicationRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(admission));
        when(horseRepository.findById(10L)).thenReturn(Optional.of(horse));
        when(stableStallRepository.findFirstAvailableRegularStallForUpdate()).thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class,
                () -> service.review(1L, 5L, approveRequest(null)));

        verify(admissionApplicationRepository, never()).save(any());
        verify(horseRepository, never()).save(any());
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Decision validation
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("decision là null: ném IllegalArgumentException trước khi chạm tới chuồng")
    void review_nullDecision_throws() {
        AdmissionApplication admission = buildAdmission(AdmissionStatus.MANAGER_REVIEW);
        Horse horse = buildHorse(HorseStatus.CANDIDATE);

        when(admissionApplicationRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(admission));
        when(horseRepository.findById(10L)).thenReturn(Optional.of(horse));

        ManagerReviewRequest req = new ManagerReviewRequest();
        // decision left null

        assertThrows(IllegalArgumentException.class,
                () -> service.review(1L, 5L, req));

        verify(admissionApplicationRepository, never()).save(any());
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Q stall absent (null quarantineStallId)
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("APPROVE khi Admission không có Q stall: không cố gắng giải phóng Q stall")
    void approve_noQuarantineStall_skipsQStallRelease() {
        AdmissionApplication admission = buildAdmission(AdmissionStatus.MANAGER_REVIEW);
        admission.setQuarantineStallId(null); // No Q stall assigned
        Horse horse = buildHorse(HorseStatus.CANDIDATE);
        horse.setCurrentStallId(null);
        StableStall regularStall = buildStall(20L, StallStatus.AVAILABLE);

        when(admissionApplicationRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(admission));
        when(horseRepository.findById(10L)).thenReturn(Optional.of(horse));
        when(stableStallRepository.findAvailableRegularStallByIdForUpdate(20L)).thenReturn(Optional.of(regularStall));
        when(admissionApplicationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        AdmissionApplication result = service.review(1L, 5L, approveRequest(20L));

        assertEquals(AdmissionStatus.APPROVED, result.getStatus());
        // findById for Q stall must never be called
        verify(stableStallRepository, never()).findById(any());
    }
}
