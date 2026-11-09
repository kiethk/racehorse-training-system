package com.rtms.backend.identity.service;

import com.rtms.backend.admission.entity.AdmissionApplication;
import com.rtms.backend.admission.enums.AdmissionStatus;
import com.rtms.backend.admission.repository.AdmissionApplicationRepository;
import com.rtms.backend.identity.dto.EligibleReplacementTrainerResponse;
import com.rtms.backend.identity.entity.GroomProfile;
import com.rtms.backend.identity.entity.User;
import com.rtms.backend.identity.repository.GroomProfileRepository;
import com.rtms.backend.identity.repository.UserRepository;
import com.rtms.backend.stable.entity.Area;
import com.rtms.backend.stable.enums.AreaType;
import com.rtms.backend.stable.repository.AreaRepository;
import com.rtms.backend.training.entity.CourseRun;
import com.rtms.backend.training.entity.TrainingGroup;
import com.rtms.backend.training.entity.TrainingSession;
import com.rtms.backend.training.enums.CourseRunStatus;
import com.rtms.backend.training.enums.SessionStatus;
import com.rtms.backend.training.repository.CourseRunRepository;
import com.rtms.backend.training.repository.TrainingGroupRepository;
import com.rtms.backend.training.repository.TrainingSessionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class TrainerHandoverService {

    public static final int MAX_AREAS_PER_TRAINER = 2;

    private final UserRepository userRepository;
    private final AreaRepository areaRepository;
    private final GroomProfileRepository groomProfileRepository;
    private final TrainingGroupRepository trainingGroupRepository;
    private final CourseRunRepository courseRunRepository;
    private final TrainingSessionRepository trainingSessionRepository;
    private final AdmissionApplicationRepository admissionApplicationRepository;

    public TrainerHandoverService(
            UserRepository userRepository,
            AreaRepository areaRepository,
            GroomProfileRepository groomProfileRepository,
            TrainingGroupRepository trainingGroupRepository,
            CourseRunRepository courseRunRepository,
            TrainingSessionRepository trainingSessionRepository,
            AdmissionApplicationRepository admissionApplicationRepository) {
        this.userRepository = userRepository;
        this.areaRepository = areaRepository;
        this.groomProfileRepository = groomProfileRepository;
        this.trainingGroupRepository = trainingGroupRepository;
        this.courseRunRepository = courseRunRepository;
        this.trainingSessionRepository = trainingSessionRepository;
        this.admissionApplicationRepository = admissionApplicationRepository;
    }

    @Transactional(readOnly = true)
    public List<EligibleReplacementTrainerResponse> getEligibleReplacements(Long targetTrainerId) {
        User target = userRepository.findById(targetTrainerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Trainer not found."));

        if (target.getRole() == null || !"HEAD_TRAINER".equals(target.getRole().getName())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User is not a Head Trainer.");
        }

        long areasToTransfer = areaRepository != null ? areaRepository.countRegularAreasByTrainerId(targetTrainerId) : 0;

        List<User> activeTrainers = userRepository.findActiveHeadTrainers();
        if (activeTrainers == null) {
            activeTrainers = Collections.emptyList();
        }

        return activeTrainers.stream()
                .filter(t -> !Objects.equals(t.getId(), targetTrainerId))
                .map(t -> {
                    long managedAreas = areaRepository != null ? areaRepository.countRegularAreasByTrainerId(t.getId()) : 0;
                    long remainingCapacity = Math.max(0, MAX_AREAS_PER_TRAINER - managedAreas);
                    boolean canAccept = remainingCapacity >= areasToTransfer;
                    return new EligibleReplacementTrainerResponse(
                            t.getId(),
                            t.getFullName(),
                            t.getEmail(),
                            managedAreas,
                            remainingCapacity,
                            canAccept
                    );
                })
                .sorted(Comparator
                        .comparing(EligibleReplacementTrainerResponse::isCanAccept).reversed()
                        .thenComparing(EligibleReplacementTrainerResponse::getRemainingCapacity, Comparator.reverseOrder())
                        .thenComparing(EligibleReplacementTrainerResponse::getFullName, String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.toList());
    }

    @Transactional
    public void executeHandover(Long targetTrainerId, Long replacementTrainerId) {
        if (Objects.equals(targetTrainerId, replacementTrainerId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Replacement trainer cannot be the same as the trainer being deactivated.");
        }

        User target = userRepository.findById(targetTrainerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Trainer not found."));

        if (target.getRole() == null || !"HEAD_TRAINER".equals(target.getRole().getName())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Target user is not a Head Trainer.");
        }

        User replacement = userRepository.findById(replacementTrainerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Replacement trainer not found."));

        if (!replacement.isActive() || replacement.getRole() == null || !"HEAD_TRAINER".equals(replacement.getRole().getName())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Replacement trainer must be an active Head Trainer.");
        }

        long areasToTransfer = areaRepository != null ? areaRepository.countRegularAreasByTrainerId(targetTrainerId) : 0;
        long currentAreasOfReplacement = areaRepository != null ? areaRepository.countRegularAreasByTrainerId(replacementTrainerId) : 0;

        if (currentAreasOfReplacement + areasToTransfer > MAX_AREAS_PER_TRAINER) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Replacement Head Trainer does not have enough capacity. Each trainer can manage at most "
                            + MAX_AREAS_PER_TRAINER + " regular areas (current: " + currentAreasOfReplacement
                            + ", transferring: " + areasToTransfer + "). Please register a new trainer or select another replacement.");
        }

        // 1. Handover regular areas
        if (areaRepository != null) {
            List<Area> regularAreas = areaRepository.findByTypeAndTrainerId(AreaType.REGULAR, targetTrainerId);
            if (regularAreas != null) {
                for (Area area : regularAreas) {
                    area.setTrainerId(replacementTrainerId);
                    areaRepository.save(area);
                }
            }
        }

        // 2. Handover subordinate grooms
        if (groomProfileRepository != null) {
            List<GroomProfile> grooms = groomProfileRepository.findByTrainerId(targetTrainerId);
            if (grooms != null) {
                for (GroomProfile groom : grooms) {
                    groom.setTrainerId(replacementTrainerId);
                    groomProfileRepository.save(groom);
                }
            }
        }

        // 3. Handover active training groups (disbanded_at IS NULL)
        if (trainingGroupRepository != null) {
            List<TrainingGroup> activeGroups = trainingGroupRepository.findByTrainerIdAndDisbandedAtIsNullOrderByCreatedAtDesc(targetTrainerId);
            if (activeGroups != null) {
                for (TrainingGroup group : activeGroups) {
                    String groupName = group.getName();
                    String candidateName = groupName;
                    int suffix = 1;
                    while (trainingGroupRepository.existsByTrainerIdAndNameIgnoreCaseAndDisbandedAtIsNull(replacementTrainerId, candidateName)) {
                        String suffixStr = suffix == 1 ? " (Handover)" : " (Handover " + suffix + ")";
                        int maxBaseLen = Math.max(1, 100 - suffixStr.length());
                        String base = groupName.length() > maxBaseLen ? groupName.substring(0, maxBaseLen) : groupName;
                        candidateName = base + suffixStr;
                        suffix++;
                    }
                    group.setName(candidateName);
                    group.setTrainerId(replacementTrainerId);
                    trainingGroupRepository.save(group);
                }
            }
        }

        // 4. Handover UPCOMING and ACTIVE course runs
        if (courseRunRepository != null) {
            List<CourseRun> liveRuns = courseRunRepository.findByTrainerIdAndStatusIn(
                    targetTrainerId,
                    List.of(CourseRunStatus.UPCOMING, CourseRunStatus.ACTIVE)
            );
            if (liveRuns != null) {
                for (CourseRun run : liveRuns) {
                    run.setTrainerId(replacementTrainerId);
                    courseRunRepository.save(run);
                }
            }
        }

        // 5. Handover sessions where status != 'CANCELLED' and session_date >= CURRENT_DATE
        if (trainingSessionRepository != null) {
            List<TrainingSession> sessionsToTransfer = trainingSessionRepository.findLiveSessionsFromDate(
                    targetTrainerId,
                    SessionStatus.CANCELLED,
                    LocalDate.now()
            );
            if (sessionsToTransfer != null && !sessionsToTransfer.isEmpty()) {
                List<TrainingSession> replacementSessions = trainingSessionRepository.findLiveSessionsFromDate(
                        replacementTrainerId,
                        SessionStatus.CANCELLED,
                        LocalDate.now()
                );
                if (replacementSessions != null && !replacementSessions.isEmpty()) {
                    for (TrainingSession ts : sessionsToTransfer) {
                        boolean overlaps = replacementSessions.stream().anyMatch(rs ->
                                Objects.equals(rs.getSessionDate(), ts.getSessionDate()) &&
                                        rs.getStartTime().isBefore(ts.getEndTime()) &&
                                        ts.getStartTime().isBefore(rs.getEndTime())
                        );
                        if (overlaps) {
                            throw new ResponseStatusException(HttpStatus.CONFLICT,
                                    "Cannot handover to replacement trainer: session on " + ts.getSessionDate()
                                            + " (" + ts.getStartTime() + " - " + ts.getEndTime()
                                            + ") conflicts with an existing session of the replacement trainer. Please reschedule or select another replacement.");
                        }
                    }
                }
                for (TrainingSession session : sessionsToTransfer) {
                    session.setTrainerId(replacementTrainerId);
                    trainingSessionRepository.save(session);
                }
            }
        }

        // 6. Handover pending admissions in TRAINER_REVIEW
        if (admissionApplicationRepository != null) {
            List<AdmissionApplication> pendingAdmissions = admissionApplicationRepository
                    .findByTrainerIdAndStatusOrderBySubmittedAtAscIdAsc(targetTrainerId, AdmissionStatus.TRAINER_REVIEW);
            if (pendingAdmissions != null) {
                for (AdmissionApplication app : pendingAdmissions) {
                    app.setTrainerId(replacementTrainerId);
                    admissionApplicationRepository.save(app);
                }
            }
        }
    }
}
