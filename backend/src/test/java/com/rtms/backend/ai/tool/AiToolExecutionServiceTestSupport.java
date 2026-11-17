package com.rtms.backend.ai.tool;

import com.rtms.backend.admission.entity.AdmissionApplication;
import com.rtms.backend.admission.enums.AdmissionStatus;
import com.rtms.backend.admission.repository.AdmissionApplicationRepository;
import com.rtms.backend.ai.dto.ClinicalChunkResponseDto;
import com.rtms.backend.ai.rag.service.ClinicalRetrievalService;
import com.rtms.backend.config.ApiException;
import com.rtms.backend.horse.entity.Horse;
import com.rtms.backend.horse.enums.HorseStatus;
import com.rtms.backend.horse.enums.HorseSex;
import com.rtms.backend.medical.entity.TreatmentPlan;
import com.rtms.backend.horse.repository.HorseRepository;
import com.rtms.backend.identity.entity.User;
import com.rtms.backend.identity.repository.UserRepository;
import com.rtms.backend.medical.entity.CareSchedule;
import com.rtms.backend.medical.entity.HealthRecord;
import com.rtms.backend.medical.enums.CareSchedulePurpose;
import com.rtms.backend.medical.enums.CareScheduleStatus;
import com.rtms.backend.medical.enums.CareType;
import com.rtms.backend.medical.repository.CareScheduleRepository;
import com.rtms.backend.medical.repository.HealthRecordRepository;
import com.rtms.backend.notification.dto.HorseAlertResponse;
import com.rtms.backend.security.AuthenticatedUser;
import com.rtms.backend.stable.dto.TodayTaskItemResponse;
import com.rtms.backend.stable.entity.Area;
import com.rtms.backend.stable.entity.StableStall;
import com.rtms.backend.stable.enums.AreaType;
import com.rtms.backend.incident.enums.IncidentType;

import com.rtms.backend.stable.enums.StallStatus;
import com.rtms.backend.stable.enums.TaskSource;
import com.rtms.backend.stable.enums.GroomTaskType;
import com.rtms.backend.stable.repository.AreaRepository;
import com.rtms.backend.stable.repository.GroomDailyTaskRepository;
import com.rtms.backend.stable.repository.StableStallRepository;
import com.rtms.backend.stable.service.GroomDailyTaskService;
import com.rtms.backend.training.repository.projection.SessionHorseNameRow;
import com.rtms.backend.training.repository.projection.SessionRow;
import com.rtms.backend.training.dto.*;
import com.rtms.backend.training.entity.Course;
import com.rtms.backend.training.entity.RacingReadinessAssessment;
import com.rtms.backend.training.entity.TrainingWorkout;
import com.rtms.backend.training.enums.DistanceBand;
import com.rtms.backend.training.enums.SessionStatus;
import com.rtms.backend.training.enums.RacingReadinessStatus;
import com.rtms.backend.training.enums.SurfaceType;
import com.rtms.backend.training.enums.TrainingDecision;
import com.rtms.backend.training.enums.CourseRunStatus;
import com.rtms.backend.training.enums.WorkoutStatus;
import com.rtms.backend.training.repository.CourseRepository;
import com.rtms.backend.training.repository.CourseRunRepository;
import com.rtms.backend.training.repository.RacingReadinessAssessmentRepository;
import com.rtms.backend.training.repository.TrainingSessionRepository;
import com.rtms.backend.training.repository.TrainingWorkoutRepository;
import com.rtms.backend.training.service.HorsePerformanceService;
import com.rtms.backend.training.service.HorseSpecialtyService;
import com.rtms.backend.audit.entity.AuditLog;
import com.rtms.backend.audit.repository.AuditLogRepository;
import com.rtms.backend.admission.entity.CandidateHorseProfile;
import com.rtms.backend.admission.repository.CandidateHorseProfileRepository;
import com.rtms.backend.training.entity.Exercise;
import com.rtms.backend.training.enums.IntensityLevel;
import com.rtms.backend.training.enums.ExerciseCategory;
import com.rtms.backend.training.repository.ExerciseRepository;
import com.rtms.backend.medical.entity.HorseHealthMetric;
import com.rtms.backend.medical.repository.HorseHealthMetricRepository;
import com.rtms.backend.horse.entity.HorsePedigree;
import com.rtms.backend.horse.repository.HorsePedigreeRepository;
import com.rtms.backend.incident.entity.HorseIncidentReport;
import com.rtms.backend.incident.repository.HorseIncidentReportRepository;
import com.rtms.backend.medical.repository.TreatmentPlanRepository;
import com.rtms.backend.medical.repository.InjuryRecordRepository;
import com.rtms.backend.identity.repository.VeterinarianProfileRepository;
import com.rtms.backend.identity.repository.TrainerProfileRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rtms.backend.stable.entity.GroomDailyTask;
import com.rtms.backend.stable.repository.GroomTaskCompletionRepository;
import com.rtms.backend.identity.entity.Role;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;

import com.rtms.backend.training.dto.HorseAttendanceResponse;
import com.rtms.backend.training.repository.projection.PastWorkoutRow;
import com.rtms.backend.training.enums.SessionStatus;
import com.rtms.backend.training.service.HorseAttendanceService;
import com.rtms.backend.training.service.TrainingSessionService;
import com.rtms.backend.training.service.TrainingGroupService;
import com.rtms.backend.training.dto.GroupMemberResponse;
import com.rtms.backend.training.dto.SessionSummaryResponse;
import com.rtms.backend.training.dto.SessionDetailResponse;
import com.rtms.backend.training.dto.SessionHorseResponse;
import com.rtms.backend.training.dto.TrainingGroupResponse;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

abstract class AiToolExecutionServiceTestSupport {

    protected DynamicToolRegistryService toolRegistryService;
    protected ClinicalRetrievalService clinicalRetrievalService;
    protected HorseRepository horseRepository;
    protected TrainingWorkoutRepository trainingWorkoutRepository;
    protected HealthRecordRepository healthRecordRepository;
    protected CareScheduleRepository careScheduleRepository;
    protected StableStallRepository stableStallRepository;
    protected GroomDailyTaskRepository groomDailyTaskRepository;
    protected GroomDailyTaskService groomDailyTaskService;
    protected AdmissionApplicationRepository admissionApplicationRepository;
    protected TrainingSessionRepository trainingSessionRepository;
    protected CourseRunRepository horseTrainingPlanRepository;
    protected HorseSpecialtyService horseSpecialtyService;
    protected HorsePerformanceService horsePerformanceService;
    protected RacingReadinessAssessmentRepository racingReadinessAssessmentRepository;
    protected AreaRepository areaRepository;
    protected UserRepository userRepository;
    protected CourseRepository courseRepository;
    protected AuditLogRepository auditLogRepository;
    protected CandidateHorseProfileRepository candidateHorseProfileRepository;
    protected ExerciseRepository exerciseRepository;
    protected HorseHealthMetricRepository horseHealthMetricRepository;
    protected HorsePedigreeRepository horsePedigreeRepository;
    protected HorseIncidentReportRepository horseIncidentReportRepository;
    protected GroomTaskCompletionRepository groomTaskCompletionRepository;
    protected HorseAttendanceService horseAttendanceService;
    protected TrainingGroupService trainingGroupService;
    protected TrainingSessionService trainingSessionService;
    protected TreatmentPlanRepository treatmentPlanRepository;
    protected InjuryRecordRepository injuryRecordRepository;
    protected VeterinarianProfileRepository veterinarianProfileRepository;
    protected TrainerProfileRepository trainerProfileRepository;
    protected ObjectMapper objectMapper;

    protected AiToolExecutionService toolExecutionService;

    @BeforeEach
    void setUp() {
        toolRegistryService = new DynamicToolRegistryService();
        clinicalRetrievalService = Mockito.mock(ClinicalRetrievalService.class);
        horseRepository = Mockito.mock(HorseRepository.class);
        trainingWorkoutRepository = Mockito.mock(TrainingWorkoutRepository.class);
        healthRecordRepository = Mockito.mock(HealthRecordRepository.class);
        careScheduleRepository = Mockito.mock(CareScheduleRepository.class);
        stableStallRepository = Mockito.mock(StableStallRepository.class);
        groomDailyTaskRepository = Mockito.mock(GroomDailyTaskRepository.class);
        groomDailyTaskService = Mockito.mock(GroomDailyTaskService.class);
        admissionApplicationRepository = Mockito.mock(AdmissionApplicationRepository.class);
        trainingSessionRepository = Mockito.mock(TrainingSessionRepository.class);
        horseTrainingPlanRepository = Mockito.mock(CourseRunRepository.class);
        horseSpecialtyService = Mockito.mock(HorseSpecialtyService.class);
        horsePerformanceService = Mockito.mock(HorsePerformanceService.class);
        racingReadinessAssessmentRepository = Mockito.mock(RacingReadinessAssessmentRepository.class);
        areaRepository = Mockito.mock(AreaRepository.class);
        userRepository = Mockito.mock(UserRepository.class);
        courseRepository = Mockito.mock(CourseRepository.class);
        auditLogRepository = Mockito.mock(AuditLogRepository.class);
        candidateHorseProfileRepository = Mockito.mock(CandidateHorseProfileRepository.class);
        exerciseRepository = Mockito.mock(ExerciseRepository.class);
        horseHealthMetricRepository = Mockito.mock(HorseHealthMetricRepository.class);
        horsePedigreeRepository = Mockito.mock(HorsePedigreeRepository.class);
        horseIncidentReportRepository = Mockito.mock(HorseIncidentReportRepository.class);
        groomTaskCompletionRepository = Mockito.mock(GroomTaskCompletionRepository.class);

        horseAttendanceService = Mockito.mock(HorseAttendanceService.class);
        trainingGroupService = Mockito.mock(TrainingGroupService.class);
        trainingSessionService = Mockito.mock(TrainingSessionService.class);
        treatmentPlanRepository = Mockito.mock(TreatmentPlanRepository.class);
        injuryRecordRepository = Mockito.mock(InjuryRecordRepository.class);
        veterinarianProfileRepository = Mockito.mock(VeterinarianProfileRepository.class);
        trainerProfileRepository = Mockito.mock(TrainerProfileRepository.class);
        objectMapper = new ObjectMapper();
        toolExecutionService = new AiToolExecutionService(
                toolRegistryService,
                clinicalRetrievalService,
                horseRepository,
                trainingWorkoutRepository,
                healthRecordRepository,
                careScheduleRepository,
                stableStallRepository,
                groomDailyTaskRepository,
                groomDailyTaskService,
                admissionApplicationRepository,
                trainingSessionRepository,
                horseTrainingPlanRepository,
                horseSpecialtyService,
                horsePerformanceService,
                racingReadinessAssessmentRepository,
                areaRepository,
                userRepository,
                courseRepository,
                auditLogRepository,
                candidateHorseProfileRepository,
                exerciseRepository,
                horseHealthMetricRepository,
                horsePedigreeRepository,
                horseIncidentReportRepository,
                groomTaskCompletionRepository,
                horseAttendanceService,
                trainingGroupService,
                trainingSessionService,
                treatmentPlanRepository,
                injuryRecordRepository,
                veterinarianProfileRepository,
                trainerProfileRepository,
                objectMapper
        );
    }

    /** A workout completed in yesterday's session. */
    protected static PastWorkoutRow ranYesterday(TrainingWorkout workout) {
        workout.setStatus(WorkoutStatus.RAN);
        return new PastWorkoutRow(workout, LocalDate.now().minusDays(1), LocalTime.of(6, 0), LocalTime.of(6, 30),
                "Gallop 1200m", "Group 1", false, SessionStatus.HELD);
    }
}
