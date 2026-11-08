package com.rtms.backend.identity.service;

import com.rtms.backend.admission.entity.AdmissionApplication;
import com.rtms.backend.admission.enums.AdmissionStatus;
import com.rtms.backend.admission.repository.AdmissionApplicationRepository;
import com.rtms.backend.identity.dto.EligibleReplacementTrainerResponse;
import com.rtms.backend.identity.entity.GroomProfile;
import com.rtms.backend.identity.entity.Role;
import com.rtms.backend.identity.entity.User;
import com.rtms.backend.identity.repository.GroomProfileRepository;
import com.rtms.backend.identity.repository.UserRepository;
import com.rtms.backend.stable.entity.Area;
import com.rtms.backend.stable.enums.AreaType;
import com.rtms.backend.stable.repository.AreaRepository;
import com.rtms.backend.training.entity.GroupPlan;
import com.rtms.backend.training.entity.TrainingGroup;
import com.rtms.backend.training.entity.TrainingSession;
import com.rtms.backend.training.enums.SessionStatus;
import com.rtms.backend.training.enums.TrainingPlanStatus;
import com.rtms.backend.training.repository.GroupPlanRepository;
import com.rtms.backend.training.repository.TrainingGroupRepository;
import com.rtms.backend.training.repository.TrainingSessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class TrainerHandoverServiceTest {

    private UserRepository userRepository;
    private AreaRepository areaRepository;
    private GroomProfileRepository groomProfileRepository;
    private TrainingGroupRepository trainingGroupRepository;
    private GroupPlanRepository groupPlanRepository;
    private TrainingSessionRepository trainingSessionRepository;
    private AdmissionApplicationRepository admissionApplicationRepository;

    private TrainerHandoverService handoverService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        areaRepository = mock(AreaRepository.class);
        groomProfileRepository = mock(GroomProfileRepository.class);
        trainingGroupRepository = mock(TrainingGroupRepository.class);
        groupPlanRepository = mock(GroupPlanRepository.class);
        trainingSessionRepository = mock(TrainingSessionRepository.class);
        admissionApplicationRepository = mock(AdmissionApplicationRepository.class);

        handoverService = new TrainerHandoverService(
                userRepository,
                areaRepository,
                groomProfileRepository,
                trainingGroupRepository,
                groupPlanRepository,
                trainingSessionRepository,
                admissionApplicationRepository
        );
    }

    private User createTrainer(Long id, String name, boolean active) {
        User user = new User();
        user.setId(id);
        user.setFullName(name);
        user.setEmail(name.toLowerCase().replace(" ", "") + "@rtms.com");
        user.setActive(active);
        Role role = new Role();
        role.setName("HEAD_TRAINER");
        user.setRole(role);
        return user;
    }

    @Test
    void getEligibleReplacements_returnsCandidatesWithCapacityCalculation() {
        Long targetId = 1L;
        User target = createTrainer(targetId, "Old Trainer", true);
        User candidateA = createTrainer(2L, "Candidate A", true);
        User candidateB = createTrainer(3L, "Candidate B", true);

        when(userRepository.findById(targetId)).thenReturn(Optional.of(target));
        when(areaRepository.countRegularAreasByTrainerId(targetId)).thenReturn(1L);

        when(userRepository.findActiveHeadTrainers()).thenReturn(List.of(target, candidateA, candidateB));
        when(areaRepository.countRegularAreasByTrainerId(2L)).thenReturn(1L); // 1 area -> remaining 1 -> canAccept 1 >= 1 (true)
        when(areaRepository.countRegularAreasByTrainerId(3L)).thenReturn(2L); // 2 areas -> remaining 0 -> canAccept 0 >= 1 (false)

        List<EligibleReplacementTrainerResponse> result = handoverService.getEligibleReplacements(targetId);

        assertEquals(2, result.size());
        EligibleReplacementTrainerResponse r1 = result.get(0);
        assertEquals(2L, r1.getId());
        assertEquals(1, r1.getManagedAreasCount());
        assertEquals(1, r1.getRemainingCapacity());
        assertTrue(r1.isCanAccept());

        EligibleReplacementTrainerResponse r2 = result.get(1);
        assertEquals(3L, r2.getId());
        assertEquals(2, r2.getManagedAreasCount());
        assertEquals(0, r2.getRemainingCapacity());
        assertFalse(r2.isCanAccept());
    }

    @Test
    void getEligibleReplacements_targetNotFound_throwsNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                handoverService.getEligibleReplacements(99L)
        );
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void getEligibleReplacements_targetNotHeadTrainer_throwsBadRequest() {
        User groom = new User();
        groom.setId(10L);
        Role role = new Role();
        role.setName("GROOM");
        groom.setRole(role);

        when(userRepository.findById(10L)).thenReturn(Optional.of(groom));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                handoverService.getEligibleReplacements(10L)
        );
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void executeHandover_sameTrainer_throwsBadRequest() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                handoverService.executeHandover(1L, 1L)
        );
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void executeHandover_targetNotFound_throwsNotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                handoverService.executeHandover(1L, 2L)
        );
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Trainer not found"));
    }

    @Test
    void executeHandover_targetNotHeadTrainer_throwsBadRequest() {
        User targetGroom = new User();
        targetGroom.setId(1L);
        Role role = new Role();
        role.setName("GROOM");
        targetGroom.setRole(role);

        when(userRepository.findById(1L)).thenReturn(Optional.of(targetGroom));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                handoverService.executeHandover(1L, 2L)
        );
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void executeHandover_replacementNotFound_throwsNotFound() {
        User target = createTrainer(1L, "Target Trainer", true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(target));
        when(userRepository.findById(2L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                handoverService.executeHandover(1L, 2L)
        );
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void executeHandover_replacementNotHeadTrainer_throwsConflict() {
        User target = createTrainer(1L, "Target Trainer", true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(target));

        User notTrainer = new User();
        notTrainer.setId(2L);
        notTrainer.setActive(true);
        Role role = new Role();
        role.setName("VETERINARIAN");
        notTrainer.setRole(role);

        when(userRepository.findById(2L)).thenReturn(Optional.of(notTrainer));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                handoverService.executeHandover(1L, 2L)
        );
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    }

    @Test
    void executeHandover_capacityOverflow_throwsConflict() {
        Long oldTrainerId = 1L;
        Long replacementId = 2L;

        User target = createTrainer(oldTrainerId, "Old Trainer", true);
        User replacement = createTrainer(replacementId, "Replacement Trainer", true);
        when(userRepository.findById(oldTrainerId)).thenReturn(Optional.of(target));
        when(userRepository.findById(replacementId)).thenReturn(Optional.of(replacement));

        // Old trainer has 1 area, replacement already has 2 areas -> total 3 > 2!
        when(areaRepository.countRegularAreasByTrainerId(oldTrainerId)).thenReturn(1L);
        when(areaRepository.countRegularAreasByTrainerId(replacementId)).thenReturn(2L);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                handoverService.executeHandover(oldTrainerId, replacementId)
        );
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        assertTrue(ex.getReason().contains("capacity"));
    }

    @Test
    void executeHandover_success_atomicTransferOfAllResponsibilities() {
        Long oldTrainerId = 1L;
        Long replacementId = 2L;

        User target = createTrainer(oldTrainerId, "Old Trainer", true);
        User replacement = createTrainer(replacementId, "Replacement Trainer", true);
        when(userRepository.findById(oldTrainerId)).thenReturn(Optional.of(target));
        when(userRepository.findById(replacementId)).thenReturn(Optional.of(replacement));

        when(areaRepository.countRegularAreasByTrainerId(oldTrainerId)).thenReturn(1L);
        when(areaRepository.countRegularAreasByTrainerId(replacementId)).thenReturn(0L);

        // 1. Regular area
        Area area = new Area();
        area.setCode("Area A");
        area.setType(AreaType.REGULAR);
        area.setTrainerId(oldTrainerId);
        when(areaRepository.findByTypeAndTrainerId(AreaType.REGULAR, oldTrainerId)).thenReturn(List.of(area));

        // 2. Groom
        GroomProfile groom = new GroomProfile();
        groom.setUserId(101L);
        groom.setTrainerId(oldTrainerId);
        when(groomProfileRepository.findByTrainerId(oldTrainerId)).thenReturn(List.of(groom));

        // 3. Training group (with name collision on "Group Alpha")
        TrainingGroup group = new TrainingGroup();
        group.setId(201L);
        group.setName("Group Alpha");
        group.setTrainerId(oldTrainerId);
        when(trainingGroupRepository.findByTrainerIdAndDisbandedAtIsNullOrderByCreatedAtDesc(oldTrainerId))
                .thenReturn(List.of(group));
        when(trainingGroupRepository.existsByTrainerIdAndNameIgnoreCaseAndDisbandedAtIsNull(replacementId, "Group Alpha"))
                .thenReturn(true);

        // 4. Group plan
        GroupPlan plan = new GroupPlan();
        plan.setId(301L);
        plan.setStatus(TrainingPlanStatus.ACTIVE);
        plan.setTrainerId(oldTrainerId);
        when(groupPlanRepository.findByTrainerIdAndStatusIn(eq(oldTrainerId), any()))
                .thenReturn(List.of(plan));

        // 5. Training session
        TrainingSession session = new TrainingSession();
        session.setId(401L);
        session.setSessionDate(LocalDate.now().plusDays(1));
        session.setStatus(SessionStatus.SCHEDULED);
        session.setTrainerId(oldTrainerId);
        when(trainingSessionRepository.findLiveSessionsFromDate(eq(oldTrainerId), eq(SessionStatus.CANCELLED), any()))
                .thenReturn(List.of(session));

        // 6. Admission application
        AdmissionApplication app = new AdmissionApplication();
        app.setId(501L);
        app.setStatus(AdmissionStatus.TRAINER_REVIEW);
        app.setTrainerId(oldTrainerId);
        when(admissionApplicationRepository.findByTrainerIdAndStatusOrderBySubmittedAtAscIdAsc(oldTrainerId, AdmissionStatus.TRAINER_REVIEW))
                .thenReturn(List.of(app));

        // Execute handover
        handoverService.executeHandover(oldTrainerId, replacementId);

        // Verify area transferred
        assertEquals(replacementId, area.getTrainerId());
        verify(areaRepository).save(area);

        // Verify groom transferred
        assertEquals(replacementId, groom.getTrainerId());
        verify(groomProfileRepository).save(groom);

        // Verify group transferred and suffixed due to collision
        assertEquals(replacementId, group.getTrainerId());
        assertEquals("Group Alpha (Handover)", group.getName());
        verify(trainingGroupRepository).save(group);

        // Verify plan transferred
        assertEquals(replacementId, plan.getTrainerId());
        verify(groupPlanRepository).save(plan);

        // Verify session transferred
        assertEquals(replacementId, session.getTrainerId());
        verify(trainingSessionRepository).save(session);

        // Verify admission application transferred
        assertEquals(replacementId, app.getTrainerId());
        verify(admissionApplicationRepository).save(app);
    }

    @Test
    void executeHandover_progressiveGroupRenaming_handlesRepeatedCollisions() {
        Long oldTrainerId = 1L;
        Long replacementId = 2L;

        User target = createTrainer(oldTrainerId, "Old Trainer", true);
        User replacement = createTrainer(replacementId, "Replacement Trainer", true);
        when(userRepository.findById(oldTrainerId)).thenReturn(Optional.of(target));
        when(userRepository.findById(replacementId)).thenReturn(Optional.of(replacement));

        TrainingGroup group = new TrainingGroup();
        group.setId(201L);
        group.setName("Sprint Team");
        group.setTrainerId(oldTrainerId);
        when(trainingGroupRepository.findByTrainerIdAndDisbandedAtIsNullOrderByCreatedAtDesc(oldTrainerId))
                .thenReturn(List.of(group));

        // Both "Sprint Team" and "Sprint Team (Handover)" already exist
        when(trainingGroupRepository.existsByTrainerIdAndNameIgnoreCaseAndDisbandedAtIsNull(replacementId, "Sprint Team"))
                .thenReturn(true);
        when(trainingGroupRepository.existsByTrainerIdAndNameIgnoreCaseAndDisbandedAtIsNull(replacementId, "Sprint Team (Handover)"))
                .thenReturn(true);
        when(trainingGroupRepository.existsByTrainerIdAndNameIgnoreCaseAndDisbandedAtIsNull(replacementId, "Sprint Team (Handover 2)"))
                .thenReturn(false);

        handoverService.executeHandover(oldTrainerId, replacementId);

        assertEquals("Sprint Team (Handover 2)", group.getName());
        assertEquals(replacementId, group.getTrainerId());
        verify(trainingGroupRepository).save(group);
    }

    @Test
    void executeHandover_sessionTimeOverlap_throwsConflict() {
        Long oldTrainerId = 1L;
        Long replacementId = 2L;

        User target = createTrainer(oldTrainerId, "Old Trainer", true);
        User replacement = createTrainer(replacementId, "Replacement Trainer", true);
        when(userRepository.findById(oldTrainerId)).thenReturn(Optional.of(target));
        when(userRepository.findById(replacementId)).thenReturn(Optional.of(replacement));

        LocalDate sessionDate = LocalDate.now().plusDays(2);

        // Session to transfer: 09:00 - 10:00
        TrainingSession sessionToTransfer = new TrainingSession();
        sessionToTransfer.setId(401L);
        sessionToTransfer.setSessionDate(sessionDate);
        sessionToTransfer.setStartTime(java.time.LocalTime.of(9, 0));
        sessionToTransfer.setEndTime(java.time.LocalTime.of(10, 0));
        sessionToTransfer.setTrainerId(oldTrainerId);

        // Replacement trainer already has session: 09:30 - 10:30 (overlaps!)
        TrainingSession existingReplacementSession = new TrainingSession();
        existingReplacementSession.setId(901L);
        existingReplacementSession.setSessionDate(sessionDate);
        existingReplacementSession.setStartTime(java.time.LocalTime.of(9, 30));
        existingReplacementSession.setEndTime(java.time.LocalTime.of(10, 30));
        existingReplacementSession.setTrainerId(replacementId);

        when(trainingSessionRepository.findLiveSessionsFromDate(eq(oldTrainerId), eq(SessionStatus.CANCELLED), any()))
                .thenReturn(List.of(sessionToTransfer));
        when(trainingSessionRepository.findLiveSessionsFromDate(eq(replacementId), eq(SessionStatus.CANCELLED), any()))
                .thenReturn(List.of(existingReplacementSession));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                handoverService.executeHandover(oldTrainerId, replacementId)
        );

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        assertTrue(ex.getReason().contains("conflicts with an existing session"));
    }
}
