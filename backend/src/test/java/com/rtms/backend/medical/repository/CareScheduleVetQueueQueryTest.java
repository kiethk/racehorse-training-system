package com.rtms.backend.medical.repository;

import com.rtms.backend.admission.entity.AdmissionApplication;
import com.rtms.backend.admission.entity.CandidateHorseProfile;
import com.rtms.backend.admission.enums.AdmissionStatus;
import com.rtms.backend.admission.repository.AdmissionApplicationRepository;
import com.rtms.backend.admission.repository.CandidateHorseProfileRepository;
import com.rtms.backend.horse.entity.Horse;
import com.rtms.backend.horse.enums.HorseStatus;
import com.rtms.backend.horse.repository.HorseRepository;
import com.rtms.backend.identity.entity.User;
import com.rtms.backend.identity.repository.RoleRepository;
import com.rtms.backend.identity.repository.UserRepository;
import com.rtms.backend.medical.entity.CareSchedule;
import com.rtms.backend.medical.enums.CareScheduleStatus;
import com.rtms.backend.medical.enums.CareType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Hàng đợi khám của bác sĩ thú y: bộ lọc trạng thái cụ thể phải thắng quy tắc mặc định
 * "ẩn COMPLETED/CANCELLED", còn bộ lọc rỗng thì vẫn ẩn đúng như cũ. Query đếm phải
 * khớp với query chính, nếu lệch thì phân trang sẽ hỏng.
 * (The vet queue: an explicit status filter must win over the default "hide
 * COMPLETED/CANCELLED" rule, and the unfiltered queue must keep hiding them. The count
 * query must stay in sync with the main query or paging breaks.)
 *
 * Dữ liệu tạo trong một transaction rồi rollback, không để lại gì trong DB.
 */
@SpringBootTest
@Transactional
class CareScheduleVetQueueQueryTest {

    @Autowired private CareScheduleRepository careSchedules;
    @Autowired private AdmissionApplicationRepository admissions;
    @Autowired private CandidateHorseProfileRepository candidates;
    @Autowired private UserRepository users;
    @Autowired private RoleRepository roles;
    @Autowired private HorseRepository horses;
    @Autowired private com.rtms.backend.stable.repository.StableStallRepository stableStalls;

    private Long vetId;
    private Long horseId;
    private Long admissionId;
    private CareSchedule completedExam;
    private CareSchedule scheduledInitial;
    private CareSchedule scheduledFollowUp;
    private CareSchedule requestedFollowUp;
    private CareSchedule overdueExam;

    @BeforeEach
    void buildFixture() {
        String tag = UUID.randomUUID().toString().substring(0, 8);

        // Vet riêng cho test: hàng đợi chỉ chứa đúng dữ liệu của test này.
        User vet = new User();
        vet.setFullName("Queue Test Vet");
        vet.setEmail("queue-test-vet-" + tag + "@rtms.invalid");
        vet.setPasswordHash("unused");
        vet.setRole(roles.findByName("VETERINARIAN").orElseThrow());
        vetId = users.saveAndFlush(vet).getId();

        User owner = users.findByEmail("owner@rtms.com").orElseThrow();
        Horse horse = horses.findAll().stream()
                .filter(h -> h.getCurrentStatus() == HorseStatus.ELIGIBLE)
                .findFirst().orElseThrow();
        horseId = horse.getId();

        AdmissionApplication admission = new AdmissionApplication();
        admission.setOwnerId(owner.getId());
        admission.setStatus(AdmissionStatus.GROOM_REVIEW);
        admission.setSubmittedAt(LocalDateTime.now());
        admissionId = admissions.saveAndFlush(admission).getId();

        CandidateHorseProfile profile = new CandidateHorseProfile();
        profile.setAdmissionId(admissionId);
        profile.setName("Queue Test Horse " + tag);
        profile.setBreed("Thoroughbred");
        candidates.saveAndFlush(profile);

        // Lần khám nhập học đã hoàn thành: chỉ thấy được khi lọc Completed.
        completedExam = schedule(CareType.INITIAL, CareScheduleStatus.COMPLETED,
                LocalDateTime.now().minusDays(2));
        // Lần khám đang chờ và lần tái khám còn hiệu lực: luôn thấy trong hàng đợi mặc định.
        scheduledInitial = schedule(CareType.INITIAL, CareScheduleStatus.SCHEDULED,
                LocalDateTime.now().plusDays(1));
        scheduledFollowUp = schedule(CareType.ROUTINE, CareScheduleStatus.SCHEDULED,
                LocalDateTime.now().plusDays(2));
        requestedFollowUp = schedule(CareType.ROUTINE, CareScheduleStatus.REQUESTED, null);
        // Ca đã hủy: mặc định ẩn, chỉ thấy khi lọc tường minh.
        overdueExam = schedule(CareType.ROUTINE, CareScheduleStatus.OVERDUE,
                LocalDateTime.now().plusDays(3));
    }

    @Test
    void defaultQueueHidesCompletedExamsButKeepsOverdue() {
        Page<CareSchedule> page = queue(null, "");

        assertEquals(4L, page.getTotalElements());
        assertEquals(Set.of(scheduledInitial.getId(), scheduledFollowUp.getId(),
                requestedFollowUp.getId(), overdueExam.getId()),
                idsOf(page));
    }

    @Test
    void completedFilterReturnsCompletedExams() {
        Page<CareSchedule> page = queue(CareScheduleStatus.COMPLETED, "");

        assertEquals(1L, page.getTotalElements());
        assertEquals(Set.of(completedExam.getId()), idsOf(page));
        assertEquals(CareScheduleStatus.COMPLETED, page.getContent().get(0).getStatus());
    }

    @Test
    void overdueFilterReturnsOverdueExamsOnlyWhenAsked() {
        Page<CareSchedule> page = queue(CareScheduleStatus.OVERDUE, "");

        assertEquals(1L, page.getTotalElements());
        assertEquals(Set.of(overdueExam.getId()), idsOf(page));
    }

    @Test
    void scheduledFilterReturnsOnlyScheduledExams() {
        Page<CareSchedule> page = queue(CareScheduleStatus.SCHEDULED, "");

        assertEquals(2L, page.getTotalElements());
        assertEquals(Set.of(scheduledInitial.getId(), scheduledFollowUp.getId()), idsOf(page));
    }

    @Test
    void explicitStatusFilterWinsOverAwaitingPill() {
        // Pill "Awaiting" mô tả ca đang chờ; khi đã chọn trạng thái cụ thể thì pill bị bỏ qua
        // để ca đã hoàn thành vẫn xem được.
        Page<CareSchedule> page = queue(CareScheduleStatus.COMPLETED, "AWAITING");

        assertEquals(1L, page.getTotalElements());
        assertEquals(Set.of(completedExam.getId()), idsOf(page));
    }

    @Test
    void countQueryStaysInSyncWithTheMainQuery() {
        // Trang chỉ 1 dòng nhưng totalElements phải đếm đủ số ca khớp bộ lọc.
        Page<CareSchedule> paged = careSchedules.findVetQueuePage(vetId, false, "", "", null, null, null, "",
                PageRequest.of(0, 1));

        assertEquals(1, paged.getContent().size());
        assertEquals(4L, paged.getTotalElements());

        Page<CareSchedule> completed = careSchedules.findVetQueuePage(vetId, false, "", "", null,
                CareScheduleStatus.COMPLETED, null, "", PageRequest.of(0, 1));

        assertEquals(1, completed.getContent().size());
        assertEquals(1L, completed.getTotalElements());
    }


    @Test
    void activeAndApprovedPillFiltersSeparateActiveAndApprovedAdmissions() {
        AdmissionApplication approvedAdmission = new AdmissionApplication();
        approvedAdmission.setOwnerId(users.findByEmail("owner@rtms.com").orElseThrow().getId());
        approvedAdmission.setStatus(AdmissionStatus.GROOM_REVIEW);
        approvedAdmission.setSubmittedAt(LocalDateTime.now());
        Long approvedAdmissionId = admissions.saveAndFlush(approvedAdmission).getId();

        CandidateHorseProfile approvedProfile = new CandidateHorseProfile();
        approvedProfile.setAdmissionId(approvedAdmissionId);
        approvedProfile.setName("Approved Horse");
        approvedProfile.setBreed("Thoroughbred");
        candidates.saveAndFlush(approvedProfile);

        approvedAdmission.setStatus(AdmissionStatus.APPROVED);
        approvedAdmission.setHorseId(horseId);
        approvedAdmission.setQuarantineStallId(stableStalls.findAll().stream().findFirst().orElseThrow().getId());
        admissions.saveAndFlush(approvedAdmission);

        CareSchedule routineOnApproved = new CareSchedule();
        routineOnApproved.setHorseId(horseId);
        routineOnApproved.setVeterinarianId(vetId);
        routineOnApproved.setAdmissionId(approvedAdmissionId);
        routineOnApproved.setCareType(CareType.ROUTINE);
        routineOnApproved.setStatus(CareScheduleStatus.SCHEDULED);
        routineOnApproved.setScheduledAt(LocalDateTime.now().plusDays(5));
        routineOnApproved = careSchedules.saveAndFlush(routineOnApproved);

        // ACTIVE pill should only return exams for non-approved admissions
        Page<CareSchedule> activePage = queue(null, "ACTIVE");
        assertEquals(4L, activePage.getTotalElements());
        assertEquals(Set.of(scheduledInitial.getId(), scheduledFollowUp.getId(),
                requestedFollowUp.getId(), overdueExam.getId()),
                idsOf(activePage));

        // APPROVED pill returns exams for approved admissions AND exams completed by the vet
        Page<CareSchedule> approvedPage = queue(null, "APPROVED");
        assertEquals(2L, approvedPage.getTotalElements());
        assertEquals(Set.of(routineOnApproved.getId(), completedExam.getId()), idsOf(approvedPage));

        // Summary counts
        List<CareScheduleStatus> activeStatuses = List.of(
                CareScheduleStatus.REQUESTED, CareScheduleStatus.SCHEDULED, CareScheduleStatus.IN_PROGRESS);
        assertEquals(3L, careSchedules.countActiveVetQueueItems(vetId, activeStatuses));
        assertEquals(2L, careSchedules.countApprovedVetQueueItems(vetId, activeStatuses));
    }

    @Test
    void trainerAndManagerReviewAdmissionsWithFollowUpsMoveToApprovedOnly() {
        AdmissionApplication trainerReviewAdm = new AdmissionApplication();
        trainerReviewAdm.setOwnerId(users.findByEmail("owner@rtms.com").orElseThrow().getId());
        trainerReviewAdm.setStatus(AdmissionStatus.GROOM_REVIEW);
        trainerReviewAdm.setSubmittedAt(LocalDateTime.now());
        Long trAdmId = admissions.saveAndFlush(trainerReviewAdm).getId();

        CandidateHorseProfile trProfile = new CandidateHorseProfile();
        trProfile.setAdmissionId(trAdmId);
        trProfile.setName("Futura Test Horse");
        trProfile.setBreed("Thoroughbred");
        candidates.saveAndFlush(trProfile);

        trainerReviewAdm.setStatus(AdmissionStatus.TRAINER_REVIEW);
        trainerReviewAdm.setHorseId(horseId);
        trainerReviewAdm.setQuarantineStallId(stableStalls.findAll().stream().findFirst().orElseThrow().getId());
        admissions.saveAndFlush(trainerReviewAdm);

        CareSchedule trInitialCompleted = new CareSchedule();
        trInitialCompleted.setHorseId(horseId);
        trInitialCompleted.setVeterinarianId(vetId);
        trInitialCompleted.setAdmissionId(trAdmId);
        trInitialCompleted.setCareType(CareType.INITIAL);
        trInitialCompleted.setStatus(CareScheduleStatus.COMPLETED);
        trInitialCompleted.setScheduledAt(LocalDateTime.now().minusHours(2));
        trInitialCompleted.setCompletedAt(LocalDateTime.now().minusHours(1));
        trInitialCompleted = careSchedules.saveAndFlush(trInitialCompleted);

        CareSchedule trRoutineFollowUp = new CareSchedule();
        trRoutineFollowUp.setHorseId(horseId);
        trRoutineFollowUp.setVeterinarianId(vetId);
        trRoutineFollowUp.setAdmissionId(trAdmId);
        trRoutineFollowUp.setCareType(CareType.ROUTINE);
        trRoutineFollowUp.setStatus(CareScheduleStatus.IN_PROGRESS);
        trRoutineFollowUp.setScheduledAt(LocalDateTime.now().plusDays(7));
        careSchedules.saveAndFlush(trRoutineFollowUp);

        // Neither the completed INITIAL nor the follow-up ROUTINE on TRAINER_REVIEW may appear in ACTIVE or IN_PROGRESS
        Page<CareSchedule> activePage = queue(null, "ACTIVE");
        assertEquals(Set.of(scheduledInitial.getId(), scheduledFollowUp.getId(),
                requestedFollowUp.getId(), overdueExam.getId()), idsOf(activePage));

        Page<CareSchedule> inProgressPage = queue(null, "IN_PROGRESS");
        assertEquals(0L, inProgressPage.getTotalElements());

        // Only the completed INITIAL admission exam appears in APPROVED (not the ROUTINE follow-up)
        Page<CareSchedule> approvedPage = queue(null, "APPROVED");
        assertEquals(Set.of(completedExam.getId(), trInitialCompleted.getId()), idsOf(approvedPage));
        assertEquals(trInitialCompleted.getId(), approvedPage.getContent().get(0).getId());
    }

    private Page<CareSchedule> queue(CareScheduleStatus scheduleStatus, String pill) {
        return careSchedules.findVetQueuePage(vetId, false, "", pill, null, scheduleStatus, null, "",
                PageRequest.of(0, 20));
    }

    private Set<Long> idsOf(Page<CareSchedule> page) {
        return new HashSet<>(page.getContent().stream().map(CareSchedule::getId).toList());
    }

    private CareSchedule schedule(CareType careType, CareScheduleStatus status, LocalDateTime scheduledAt) {
        CareSchedule schedule = new CareSchedule();
        schedule.setHorseId(horseId);
        schedule.setVeterinarianId(vetId);
        schedule.setAdmissionId(admissionId);
        schedule.setCareType(careType);
        schedule.setStatus(status);
        schedule.setScheduledAt(scheduledAt);
        schedule.setRequestedAt(scheduledAt);
        schedule.setCompletedAt(status == CareScheduleStatus.COMPLETED ? LocalDateTime.now() : null);
        
        return careSchedules.saveAndFlush(schedule);
    }
}
