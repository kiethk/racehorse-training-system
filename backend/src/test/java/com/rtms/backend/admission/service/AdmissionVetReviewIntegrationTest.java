package com.rtms.backend.admission.service;

import com.rtms.backend.admission.dto.VetReviewRequest;
import com.rtms.backend.admission.dto.VetReviewResponse;
import com.rtms.backend.admission.entity.AdmissionApplication;
import com.rtms.backend.admission.enums.AdmissionStatus;
import com.rtms.backend.admission.enums.ReviewDecision;
import com.rtms.backend.admission.repository.AdmissionApplicationRepository;
import com.rtms.backend.horse.entity.Horse;
import com.rtms.backend.horse.enums.HorseStatus;
import com.rtms.backend.horse.repository.HorseRepository;
import com.rtms.backend.identity.entity.Role;
import com.rtms.backend.identity.entity.User;
import com.rtms.backend.identity.repository.RoleRepository;
import com.rtms.backend.identity.repository.UserRepository;
import com.rtms.backend.medical.entity.CareSchedule;
import com.rtms.backend.medical.enums.CareScheduleStatus;
import com.rtms.backend.medical.enums.CareType;
import com.rtms.backend.medical.repository.CareScheduleRepository;
import com.rtms.backend.medical.repository.HealthRecordRepository;
import com.rtms.backend.notification.repository.NotificationRepository;
import com.rtms.backend.stable.entity.Area;
import com.rtms.backend.stable.entity.StableStall;
import com.rtms.backend.stable.enums.AreaType;
import com.rtms.backend.stable.enums.StallStatus;
import com.rtms.backend.stable.repository.AreaRepository;
import com.rtms.backend.stable.repository.StableStallRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * T15a integration trên PostgreSQL thật: vet reject đóng đơn, hủy lịch, trả chuồng, thông báo.
 * Dữ liệu tạo trong transaction rồi rollback.
 */
@SpringBootTest
@Transactional
class AdmissionVetReviewIntegrationTest {

    @Autowired private AdmissionReviewService admissionReviewService;
    @Autowired private AdmissionApplicationRepository admissions;
    @Autowired private CareScheduleRepository careSchedules;
    @Autowired private HorseRepository horses;
    @Autowired private StableStallRepository stalls;
    @Autowired private AreaRepository areas;
    @Autowired private UserRepository users;
    @Autowired private RoleRepository roles;
    @Autowired private HealthRecordRepository healthRecords;
    @Autowired private NotificationRepository notifications;

    private Long vetId;
    private Long managerId;
    private Long ownerId;
    private Horse horse;
    private StableStall quarantineStall;
    private AdmissionApplication admission;

    @BeforeEach
    void buildFixture() {
        String tag = UUID.randomUUID().toString().substring(0, 8);

        Role vetRole = roles.findByName("VETERINARIAN").orElseThrow();
        User vet = new User();
        vet.setFullName("T15a Vet");
        vet.setEmail("t15a-vet-" + tag + "@rtms.invalid");
        vet.setPasswordHash("unused");
        vet.setRole(vetRole);
        vet.setActive(true);
        vetId = users.saveAndFlush(vet).getId();

        Role managerRole = roles.findByName("CLUB_MANAGER").orElseThrow();
        User manager = new User();
        manager.setFullName("T15a Manager");
        manager.setEmail("t15a-manager-" + tag + "@rtms.invalid");
        manager.setPasswordHash("unused");
        manager.setRole(managerRole);
        manager.setActive(true);
        managerId = users.saveAndFlush(manager).getId();

        Role ownerRole = roles.findByName("HORSE_OWNER").orElseThrow();
        User owner = new User();
        owner.setFullName("T15a Owner");
        owner.setEmail("t15a-owner-" + tag + "@rtms.invalid");
        owner.setPasswordHash("unused");
        owner.setRole(ownerRole);
        owner.setActive(true);
        ownerId = users.saveAndFlush(owner).getId();

        Area quarantineArea = areas.findByType(AreaType.QUARANTINE).stream().findFirst()
                .orElseGet(() -> {
                    Area a = new Area();
                    a.setType(AreaType.QUARANTINE);
                    a.setCode("Q-T15");
                    return areas.save(a);
                });

        quarantineStall = new StableStall();
        quarantineStall.setAreaId(quarantineArea.getId());
        quarantineStall.setStallNumber(999);
        quarantineStall.setStallCode("Q-T15-" + tag);
        quarantineStall.setStatus(StallStatus.AVAILABLE);
        quarantineStall = stalls.saveAndFlush(quarantineStall);

        horse = new Horse();
        horse.setName("T15a Horse " + tag);
        horse.setCurrentStatus(HorseStatus.CANDIDATE);
        horse.setCurrentStallId(quarantineStall.getId());
        horse.setOwnerId(ownerId);
        horse = horses.saveAndFlush(horse);

        quarantineStall.setStatus(StallStatus.OCCUPIED);
        stalls.saveAndFlush(quarantineStall);

        admission = new AdmissionApplication();
        admission.setOwnerId(ownerId);
        admission.setHorseId(horse.getId());
        admission.setQuarantineStallId(quarantineStall.getId());
        admission.setStatus(AdmissionStatus.VET_REVIEW);
        admission.setSubmittedAt(LocalDateTime.now().minusDays(1));
        admission.setVeterinarianId(vetId);
        admission = admissions.saveAndFlush(admission);

        CareSchedule schedule = new CareSchedule();
        schedule.setHorseId(horse.getId());
        schedule.setAdmissionId(admission.getId());
        schedule.setCareType(CareType.INITIAL);
        schedule.setStatus(CareScheduleStatus.IN_PROGRESS);
        schedule.setVeterinarianId(vetId);
        schedule.setDurationMinutes(30);
        schedule.setScheduledAt(LocalDateTime.now().minusMinutes(15));
        careSchedules.saveAndFlush(schedule);
    }

    @Test
    void vetRejectsAdmission_closesApplicationAndCleansUp() {
        VetReviewRequest req = new VetReviewRequest();
        req.setDecision(ReviewDecision.REJECT);
        req.setRejectReason("Chronic lameness unsuitable for racing");

        VetReviewResponse response = admissionReviewService.reviewByVet(admission.getId(), req, vetId);

        assertEquals(AdmissionStatus.REJECTED, response.status());
        assertNull(response.trainingDecision());
        assertEquals("Chronic lameness unsuitable for racing", response.feedback());

        AdmissionApplication updated = admissions.findById(admission.getId()).orElseThrow();
        assertEquals(AdmissionStatus.REJECTED, updated.getStatus());
        assertEquals(vetId, updated.getVeterinarianId());
        assertNotNull(updated.getVetReviewedAt());
        assertEquals("Chronic lameness unsuitable for racing", updated.getVetFeedback());
        assertNull(updated.getVetTrainingDecision());

        Horse updatedHorse = horses.findById(horse.getId()).orElseThrow();
        assertEquals(HorseStatus.REJECTED, updatedHorse.getCurrentStatus());
        assertNull(updatedHorse.getCurrentStallId());

        StableStall updatedStall = stalls.findById(quarantineStall.getId()).orElseThrow();
        assertEquals(StallStatus.AVAILABLE, updatedStall.getStatus());

        long openSchedules = careSchedules.findByHorseIdAndStatusIn(horse.getId(),
                java.util.List.of(CareScheduleStatus.SCHEDULED, CareScheduleStatus.IN_PROGRESS)).size();
        assertEquals(0L, openSchedules);

        long completedHealthRecords = healthRecords.findByHorseIdOrderByExaminedAtDesc(horse.getId()).size();
        assertEquals(0L, completedHealthRecords, "Reject must not create a HealthRecord");

        long ownerNotifications = notifications.findByRecipientIdOrderByCreatedAtDesc(ownerId).stream()
                .filter(n -> com.rtms.backend.notification.service.NotificationTypes.ADMISSION_REJECTED.equals(n.getNotificationType()))
                .count();
        long managerNotifications = notifications.findByRecipientIdOrderByCreatedAtDesc(managerId).stream()
                .filter(n -> com.rtms.backend.notification.service.NotificationTypes.ADMISSION_REJECTED.equals(n.getNotificationType()))
                .count();
        assertTrue(ownerNotifications >= 1, "Owner must be notified");
        assertTrue(managerNotifications >= 1, "Manager must be notified");
    }

    @Test
    void vetRejectWithoutReason_throws400() {
        VetReviewRequest req = new VetReviewRequest();
        req.setDecision(ReviewDecision.REJECT);

        com.rtms.backend.config.ApiException ex = assertThrows(com.rtms.backend.config.ApiException.class,
                () -> admissionReviewService.reviewByVet(admission.getId(), req, vetId));

        assertEquals(org.springframework.http.HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals("VALIDATION_ERROR", ex.getErrorCode());
        assertEquals(AdmissionStatus.VET_REVIEW, admissions.findById(admission.getId()).orElseThrow().getStatus());
    }

    @Test
    void vetRejectByUnassignedVet_throws403() {
        User otherVet = new User();
        otherVet.setFullName("T15a Other Vet");
        otherVet.setEmail("t15a-ovet-" + UUID.randomUUID().toString().substring(0, 8) + "@rtms.invalid");
        otherVet.setPasswordHash("unused");
        otherVet.setRole(roles.findByName("VETERINARIAN").orElseThrow());
        otherVet.setActive(true);
        Long otherVetId = users.saveAndFlush(otherVet).getId();

        VetReviewRequest req = new VetReviewRequest();
        req.setDecision(ReviewDecision.REJECT);
        req.setRejectReason("Reason");

        com.rtms.backend.config.ApiException ex = assertThrows(com.rtms.backend.config.ApiException.class,
                () -> admissionReviewService.reviewByVet(admission.getId(), req, otherVetId));

        assertEquals(org.springframework.http.HttpStatus.FORBIDDEN, ex.getStatus());
        assertEquals("FORBIDDEN", ex.getErrorCode());
    }
}
