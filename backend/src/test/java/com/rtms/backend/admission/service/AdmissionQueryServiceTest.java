package com.rtms.backend.admission.service;

import com.rtms.backend.common.storage.FileStorageService;
import com.rtms.backend.admission.dto.GroomAdmissionQueueResponse;
import com.rtms.backend.admission.dto.TrainerAdmissionQueueResponse;
import com.rtms.backend.admission.entity.AdmissionApplication;
import com.rtms.backend.admission.entity.CandidateHorseProfile;
import com.rtms.backend.admission.entity.AdmissionDocument;
import com.rtms.backend.admission.enums.AdmissionDocumentType;
import com.rtms.backend.stable.entity.StableStall;
import com.rtms.backend.admission.enums.AdmissionStatus;
import com.rtms.backend.admission.repository.AdmissionApplicationRepository;
import com.rtms.backend.admission.repository.AdmissionDocumentRepository;
import com.rtms.backend.admission.repository.CandidateHorseProfileRepository;
import com.rtms.backend.medical.repository.HealthRecordRepository;
import com.rtms.backend.stable.repository.StableStallRepository;
import com.rtms.backend.identity.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.rtms.backend.admission.dto.AdmissionDetailResponse;
import com.rtms.backend.admission.dto.InitialVetExamResponse;
import com.rtms.backend.identity.entity.User;
import com.rtms.backend.medical.entity.CareSchedule;
import com.rtms.backend.medical.entity.HealthRecord;
import com.rtms.backend.medical.entity.HorseHealthMetric;
import com.rtms.backend.medical.enums.CareType;
import com.rtms.backend.medical.repository.CareScheduleRepository;
import com.rtms.backend.medical.repository.HorseHealthMetricRepository;
import com.rtms.backend.training.enums.TrainingDecision;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class AdmissionQueryServiceTest {
    private AdmissionApplicationRepository admissions;
    private CandidateHorseProfileRepository candidates;
    private AdmissionDocumentRepository documents;
    private HealthRecordRepository healthRecords;
    private HorseHealthMetricRepository healthMetrics;
    private UserRepository users;
    private CareScheduleRepository careSchedules;
    private StableStallRepository stalls;
    private AdmissionQueryService service;

    @BeforeEach
    void setUp() {
        admissions = mock(AdmissionApplicationRepository.class);
        candidates = mock(CandidateHorseProfileRepository.class);
        documents = mock(AdmissionDocumentRepository.class);
        healthRecords = mock(HealthRecordRepository.class);
        healthMetrics = mock(HorseHealthMetricRepository.class);
        users = mock(UserRepository.class);
        careSchedules = mock(CareScheduleRepository.class);
        stalls = mock(StableStallRepository.class);

        service = new AdmissionQueryService(
                admissions,
                candidates,
                documents,
                stalls,
                healthRecords,
                healthMetrics,
                mock(FileStorageService.class),
                users,
                careSchedules);
    }

    @Test
    void managerListBatchesAll54RowsAndPreservesOrderNullStallsAndFirstPhoto() {
        var rows = new java.util.ArrayList<AdmissionApplication>();
        var profiles = new java.util.ArrayList<CandidateHorseProfile>();
        for (long id=1; id<=54; id++) {
            var row = new AdmissionApplication(); row.setId(id); row.setStatus(AdmissionStatus.APPROVED);
            row.setTrainerId(7L); row.setSubmittedAt(LocalDateTime.of(2026,3,1,9,0));
            rows.add(row);
            var profile = new CandidateHorseProfile(); profile.setAdmissionId(id); profile.setName("Horse "+id);
            profile.setBreed("Arabian"); profiles.add(profile);
        }
        rows.getFirst().setQuarantineStallId(500L);
        java.util.Collections.reverse(profiles);
        var stall = new StableStall(); stall.setId(500L); stall.setStallCode("Q-A01");
        var later = new AdmissionDocument(); later.setId(12L); later.setAdmissionId(1L);
        var first = new AdmissionDocument(); first.setId(11L); first.setAdmissionId(1L);
        when(admissions.findAll()).thenReturn(rows);
        when(candidates.findByAdmissionIdIn(anyCollection())).thenReturn(profiles);
        when(stalls.findAllById(any())).thenReturn(List.of(stall));
        when(documents.findByAdmissionIdInAndDocumentType(anyCollection(),eq(AdmissionDocumentType.HORSE_PHOTO)))
                .thenReturn(List.of(later,first));

        var result=service.getAdmissions(null);

        assertEquals(54,result.size()); assertEquals("Horse 1",result.getFirst().getCandidateName());
        assertEquals("Horse 54",result.getLast().getCandidateName());
        assertEquals("Q-A01",result.getFirst().getQuarantineStallCode());
        assertEquals("/api/admissions/1/documents/11/file",result.getFirst().getImageUrl());
        assertNull(result.get(1).getQuarantineStallCode()); assertNull(result.get(1).getImageUrl());
        assertEquals(7L,result.getFirst().getTrainerId());
        verify(candidates,times(1)).findByAdmissionIdIn(anyCollection());
        verify(stalls,times(1)).findAllById(any());
        verify(documents,times(1)).findByAdmissionIdInAndDocumentType(anyCollection(),eq(AdmissionDocumentType.HORSE_PHOTO));
        verify(candidates,never()).findByAdmissionId(anyLong());
        verify(stalls,never()).findById(anyLong());
        verify(documents,never()).findByAdmissionId(anyLong());
    }

    @Test
    void emptyManagerStatusFilterDoesNotLoadRelatedTables() {
        when(admissions.findByStatus(AdmissionStatus.MANAGER_REVIEW)).thenReturn(List.of());
        assertEquals(List.of(),service.getAdmissions(AdmissionStatus.MANAGER_REVIEW));
        verify(admissions,never()).findAll();
        verifyNoInteractions(candidates,stalls,documents);
    }

    @Test
    void managerListWithoutAnyStallStillMapsAndMissingSnapshotIsRejected() {
        var row=new AdmissionApplication(); row.setId(1L); row.setStatus(AdmissionStatus.GROOM_REVIEW);
        var candidate=new CandidateHorseProfile(); candidate.setAdmissionId(1L); candidate.setName("Candidate");
        when(admissions.findByStatus(AdmissionStatus.GROOM_REVIEW)).thenReturn(List.of(row));
        when(candidates.findByAdmissionIdIn(anyCollection())).thenReturn(List.of(candidate));
        when(documents.findByAdmissionIdInAndDocumentType(anyCollection(),any())).thenReturn(List.of());
        var result=service.getAdmissions(AdmissionStatus.GROOM_REVIEW);
        assertEquals("Candidate",result.getFirst().getCandidateName());
        assertNull(result.getFirst().getQuarantineStallCode());
        verifyNoInteractions(stalls);
        when(candidates.findByAdmissionIdIn(anyCollection())).thenReturn(List.of());
        assertThrows(IllegalStateException.class,()->service.getAdmissions(AdmissionStatus.GROOM_REVIEW));
    }

    @Test
    void groomQueueTrimsNameUsesInclusiveDateRangeAndCapsPageSize() {
        var page = PageRequest.of(2, 10,
                Sort.by(Sort.Order.asc("createdAt"), Sort.Order.asc("id")));
        when(stalls.isQuarantineGroom(7L)).thenReturn(true);
        when(admissions.findGroomQueue(eq(7L), eq(true), eq(AdmissionStatus.GROOM_REVIEW), eq(false), eq(AdmissionStatus.GROOM_REVIEW), eq(true), eq("Mercury"),
                eq(true), any(), eq(true), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), page, 0));

        GroomAdmissionQueueResponse response = service.getGroomQueue(7L, " Mercury ", null,
                LocalDate.of(2026, 1, 2), LocalDate.of(2026, 1, 8), 2, 50);

        assertEquals(10, response.size());
        assertEquals(2, response.page());
        var pageable = org.mockito.ArgumentCaptor.forClass(Pageable.class);
        verify(admissions).findGroomQueue(eq(7L), eq(true), eq(AdmissionStatus.GROOM_REVIEW), eq(false), eq(AdmissionStatus.GROOM_REVIEW), eq(true), eq("Mercury"),
                eq(true), eq(LocalDateTime.of(2026, 1, 2, 0, 0)),
                eq(true), eq(LocalDateTime.of(2026, 1, 9, 0, 0)), pageable.capture());
        // BR-04: oldest admission first, id as a stable tie-breaker for paging.
        assertEquals(Sort.by(Sort.Order.asc("createdAt"), Sort.Order.asc("id")), pageable.getValue().getSort());
    }

    @Test
    void groomQueueRejectsReversedDateRangeBeforeQuery() {
        assertThrows(IllegalArgumentException.class, () -> service.getGroomQueue(7L, "", null,
                LocalDate.of(2026, 1, 9), LocalDate.of(2026, 1, 8), 0, 10));
        verifyNoInteractions(admissions);
    }

    /**
     * Hàng chờ Trainer phải tách hai nhóm và chỉ hỏi dữ liệu của CHÍNH Trainer
     * đó — không được rơi về findAll()/findByStatus() như endpoint dùng chung
     * GET /api/admissions, vì khi đó mọi Trainer lại thấy hồ sơ của nhau.
     */
    @Test
    void trainerQueueSplitsPendingAndReviewedForThatTrainerOnly() {
        AdmissionApplication pending = admission(1L, AdmissionStatus.TRAINER_REVIEW, null);
        AdmissionApplication reviewed = admission(2L, AdmissionStatus.MANAGER_REVIEW,
                LocalDateTime.of(2026, 3, 1, 9, 0));

        when(admissions.findByTrainerIdAndStatusOrderBySubmittedAtAscIdAsc(7L, AdmissionStatus.TRAINER_REVIEW))
                .thenReturn(List.of(pending));
        when(admissions.findByTrainerIdAndTrainerReviewedAtIsNotNullOrderByTrainerReviewedAtDesc(7L))
                .thenReturn(List.of(reviewed));
        when(documents.findByAdmissionId(anyLong())).thenReturn(List.of());

        TrainerAdmissionQueueResponse response = service.getTrainerQueue(7L);

        assertEquals(1, response.pending().size());
        assertEquals(1L, response.pending().get(0).getAdmissionId());
        assertEquals(1, response.reviewed().size());
        assertEquals(2L, response.reviewed().get(0).getAdmissionId());

        verify(admissions, never()).findAll();
        verify(admissions, never()).findByStatus(any());
    }

    /** Đơn nào cũng phải có CandidateHorseProfile, nếu không toSummaryResponse ném lỗi. */
    private AdmissionApplication admission(Long id, AdmissionStatus status,
                                           LocalDateTime trainerReviewedAt) {
        AdmissionApplication a = new AdmissionApplication();
        a.setId(id);
        a.setStatus(status);
        a.setTrainerId(7L);
        a.setSubmittedAt(LocalDateTime.of(2026, 2, 1, 8, 0));
        a.setTrainerReviewedAt(trainerReviewedAt);

        CandidateHorseProfile profile = new CandidateHorseProfile();
        profile.setName("Ngựa #" + id);
        profile.setBreed("Thoroughbred");
        when(candidates.findByAdmissionId(id)).thenReturn(Optional.of(profile));

        return a;
    }

    @Test
    void getAdmissionDetail_populatesInitialVetExam_usingCareScheduleAndHealthRecordVet() {
        Long admissionId = 100L;
        Long assignedVetId = 88L;
        Long performingVetId = 99L;
        Long careScheduleId = 200L;
        Long healthRecordId = 300L;

        AdmissionApplication app = new AdmissionApplication();
        app.setId(admissionId);
        app.setStatus(AdmissionStatus.MANAGER_REVIEW);
        app.setVeterinarianId(assignedVetId);
        app.setSubmittedAt(LocalDateTime.of(2026, 3, 1, 10, 0));

        when(admissions.findById(admissionId)).thenReturn(Optional.of(app));

        CandidateHorseProfile profile = new CandidateHorseProfile();
        profile.setName("Thunder");
        profile.setBreed("Thoroughbred");
        when(candidates.findByAdmissionId(admissionId)).thenReturn(Optional.of(profile));
        when(documents.findByAdmissionId(admissionId)).thenReturn(List.of());

        CareSchedule schedule = new CareSchedule();
        schedule.setId(careScheduleId);
        schedule.setAdmissionId(admissionId);
        schedule.setCareType(CareType.INITIAL);
        schedule.setVeterinarianId(assignedVetId);
        when(careSchedules.findFirstByAdmissionIdAndCareTypeOrderByCreatedAtDesc(admissionId, CareType.INITIAL))
                .thenReturn(Optional.of(schedule));

        HealthRecord record = new HealthRecord();
        record.setId(healthRecordId);
        record.setCareScheduleId(careScheduleId);
        record.setVeterinarianId(performingVetId); // Vet who actually conducted the exam
        record.setExaminedAt(LocalDateTime.of(2026, 3, 2, 14, 0));
        record.setTrainingDecision(TrainingDecision.ALLOWED);
        record.setSymptoms("None");
        record.setFindings("Clear lungs, sound gait");
        when(healthRecords.findByCareScheduleId(careScheduleId)).thenReturn(Optional.of(record));

        User performingVet = new User();
        performingVet.setId(performingVetId);
        performingVet.setFullName("Dr. Elena Rostova");
        when(users.findById(performingVetId)).thenReturn(Optional.of(performingVet));

        HorseHealthMetric metric = new HorseHealthMetric();
        metric.setHealthRecordId(healthRecordId);
        metric.setHeartRate(java.math.BigDecimal.valueOf(42));
        metric.setRecordedAt(LocalDateTime.of(2026, 3, 2, 14, 15));
        when(healthMetrics.findByHealthRecordIdOrderByRecordedAtAsc(healthRecordId))
                .thenReturn(List.of(metric));

        AdmissionDetailResponse response = service.getAdmissionDetail(admissionId);

        assertNotNull(response);
        InitialVetExamResponse exam = response.getInitialVetExam();
        assertNotNull(exam, "initialVetExam should be populated");
        assertEquals(healthRecordId, exam.getHealthRecordId());
        // Verify vet identity is taken from HealthRecord.veterinarianId, NOT admission.veterinarianId
        assertEquals(performingVetId, exam.getVeterinarianId());
        assertEquals("Dr. Elena Rostova", exam.getVeterinarianName());
        assertEquals(TrainingDecision.ALLOWED, exam.getTrainingDecision());
        assertEquals("Clear lungs, sound gait", exam.getFindings());
        assertNotNull(exam.getMetrics());
        assertEquals(1, exam.getMetrics().size());
        assertEquals(java.math.BigDecimal.valueOf(42), exam.getMetrics().get(0).heartRate());
    }

    @Test
    void getAdmissionDetail_whenNoCareSchedule_initialVetExamIsNull() {
        Long admissionId = 101L;

        AdmissionApplication app = new AdmissionApplication();
        app.setId(admissionId);
        app.setStatus(AdmissionStatus.VET_REVIEW);
        app.setSubmittedAt(LocalDateTime.of(2026, 3, 1, 10, 0));

        when(admissions.findById(admissionId)).thenReturn(Optional.of(app));

        CandidateHorseProfile profile = new CandidateHorseProfile();
        profile.setName("Blaze");
        when(candidates.findByAdmissionId(admissionId)).thenReturn(Optional.of(profile));
        when(documents.findByAdmissionId(admissionId)).thenReturn(List.of());
        when(careSchedules.findFirstByAdmissionIdAndCareTypeOrderByCreatedAtDesc(admissionId, CareType.INITIAL))
                .thenReturn(Optional.empty());

        AdmissionDetailResponse response = service.getAdmissionDetail(admissionId);

        assertNotNull(response);
        assertNull(response.getInitialVetExam(), "initialVetExam should be null when no initial exam schedule exists");
    }
}
