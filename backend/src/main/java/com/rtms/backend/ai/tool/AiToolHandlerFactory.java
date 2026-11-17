package com.rtms.backend.ai.tool;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rtms.backend.admission.repository.AdmissionApplicationRepository;
import com.rtms.backend.admission.repository.CandidateHorseProfileRepository;
import com.rtms.backend.ai.rag.service.ClinicalRetrievalService;
import com.rtms.backend.ai.tool.handler.AiToolHandler;
import com.rtms.backend.ai.tool.handler.AiToolSupport;
import com.rtms.backend.ai.tool.handler.groom.*;
import com.rtms.backend.ai.tool.handler.manager.*;
import com.rtms.backend.ai.tool.handler.owner.*;
import com.rtms.backend.ai.tool.handler.trainer.*;
import com.rtms.backend.ai.tool.handler.vet.*;
import com.rtms.backend.audit.repository.AuditLogRepository;
import com.rtms.backend.horse.repository.HorsePedigreeRepository;
import com.rtms.backend.horse.repository.HorseRepository;
import com.rtms.backend.identity.repository.TrainerProfileRepository;
import com.rtms.backend.identity.repository.UserRepository;
import com.rtms.backend.identity.repository.VeterinarianProfileRepository;
import com.rtms.backend.incident.repository.HorseIncidentReportRepository;
import com.rtms.backend.medical.repository.*;
import com.rtms.backend.stable.repository.AreaRepository;
import com.rtms.backend.stable.repository.GroomDailyTaskRepository;
import com.rtms.backend.stable.repository.GroomTaskCompletionRepository;
import com.rtms.backend.stable.repository.StableStallRepository;
import com.rtms.backend.stable.service.GroomDailyTaskService;
import com.rtms.backend.training.repository.*;
import com.rtms.backend.training.service.*;

import java.util.*;

/** Builds the handler list for the backwards-compatible constructor. */
final class AiToolHandlerFactory {
    private AiToolHandlerFactory() {
    }

    static List<AiToolHandler> create(
                ClinicalRetrievalService clinicalRetrievalService,
                HorseRepository horseRepository,
                TrainingWorkoutRepository trainingWorkoutRepository,
                HealthRecordRepository healthRecordRepository,
                CareScheduleRepository careScheduleRepository,
                StableStallRepository stableStallRepository,
                GroomDailyTaskRepository groomDailyTaskRepository,
                GroomDailyTaskService groomDailyTaskService,
                AdmissionApplicationRepository admissionApplicationRepository,
                TrainingSessionRepository trainingSessionRepository,
                CourseRunRepository courseRunRepository,
                HorseSpecialtyService horseSpecialtyService,
                HorsePerformanceService horsePerformanceService,
                RacingReadinessAssessmentRepository racingReadinessAssessmentRepository,
                AreaRepository areaRepository,
                UserRepository userRepository,
                CourseRepository courseRepository,
                AuditLogRepository auditLogRepository,
                CandidateHorseProfileRepository candidateHorseProfileRepository,
                ExerciseRepository exerciseRepository,
                HorseHealthMetricRepository horseHealthMetricRepository,
                HorsePedigreeRepository horsePedigreeRepository,
                HorseIncidentReportRepository horseIncidentReportRepository,
                GroomTaskCompletionRepository groomTaskCompletionRepository,
                HorseAttendanceService horseAttendanceService,
                TrainingGroupService trainingGroupService,
                TrainingSessionService trainingSessionService,
                TreatmentPlanRepository treatmentPlanRepository,
                InjuryRecordRepository injuryRecordRepository,
                VeterinarianProfileRepository veterinarianProfileRepository,
                TrainerProfileRepository trainerProfileRepository,
                ObjectMapper objectMapper
        ) {
        AiToolSupport support = new AiToolSupport(
                        horseRepository,
                        stableStallRepository,
                        areaRepository,
                        userRepository,
                        trainingWorkoutRepository,
                        courseRunRepository,
                        veterinarianProfileRepository
                );
        
                List<AiToolHandler> handlers = new ArrayList<>();
        
                // HORSE_OWNER (10)
                handlers.add(new GetMyHorsesTool(horseRepository, support));
                handlers.add(new GetMyHorseHealthSummaryTool(healthRecordRepository, careScheduleRepository, support));
                handlers.add(new GetMyHorseTrainingProgressTool(horseAttendanceService, support));
                handlers.add(new GetMyHorseUpcomingScheduleTool(trainingSessionRepository, support));
                handlers.add(new GetMyHorseAdmissionsTool(admissionApplicationRepository, candidateHorseProfileRepository, horseRepository, support));
                handlers.add(new GetHorseTrainingSummaryTool(horseAttendanceService, support));
                handlers.add(new GetHorseCareScheduleTool(careScheduleRepository, support));
                handlers.add(new GetMyHorseIncidentHistoryTool(horseIncidentReportRepository, userRepository, support));
                handlers.add(new GetMyHorsePedigreeInfoTool(horsePedigreeRepository, support));
                handlers.add(new AnalyzeHorseInvestmentAndFitnessRoiTool(support));
        
                // HEAD_TRAINER (16)
                handlers.add(new GetTrainerHorsesTool(horseRepository, horseSpecialtyService, stableStallRepository, userRepository, support));
                handlers.add(new GetTrainerGroupsTool(trainingGroupService, support));
                handlers.add(new GetWorkoutHistoryTool(userRepository, support));
                handlers.add(new GetHorseFitnessTrendTool(trainingWorkoutRepository, support));
                handlers.add(new GetTrainerScheduleTool(trainingSessionRepository, trainingSessionService, exerciseRepository, support));
                handlers.add(new GetTrainerPlansSummaryTool(courseRunRepository, horseAttendanceService, courseRepository, support));
                handlers.add(new GetHorseSpecialtyAnalysisTool(horseRepository, horseSpecialtyService, trainingWorkoutRepository, support));
                handlers.add(new GetTrainerRiskAlertsTool(horseRepository, horsePerformanceService, support));
                handlers.add(new GetRacingReadinessAssessmentTool(racingReadinessAssessmentRepository, userRepository, support));
                handlers.add(new GetTrainerAssignedGroomsTool(areaRepository, stableStallRepository, horseRepository, userRepository, support));
                handlers.add(new GetAssignedAdmissionReviewsTool(admissionApplicationRepository, candidateHorseProfileRepository, userRepository, support));
                handlers.add(new GetCourseCurriculumOverviewTool(courseRepository, exerciseRepository));
                handlers.add(new AnalyzeWorkoutLoadAndOvertrainingRiskTool(support));
                RecommendOptimalRaceDistanceAndTrackTool optimalRaceTool = new RecommendOptimalRaceDistanceAndTrackTool(horseSpecialtyService, support);
                handlers.add(optimalRaceTool);
                RecommendNextWorkoutAdjustmentTool nextWorkoutTool = new RecommendNextWorkoutAdjustmentTool(support);
                handlers.add(nextWorkoutTool);
                handlers.add(new RecommendTrainerStrategyTool(optimalRaceTool, nextWorkoutTool, support));
        
                // VETERINARIAN (14)
                handlers.add(new SearchClinicalKnowledgeTool(clinicalRetrievalService, support));
                handlers.add(new GetVetExaminationOverviewTool(careScheduleRepository, horseRepository, support));
                handlers.add(new GetHorseMedicalHistoryTool(healthRecordRepository, injuryRecordRepository, userRepository, support));
                handlers.add(new GetActivePrescriptionsTool(treatmentPlanRepository, careScheduleRepository, objectMapper, support));
                handlers.add(new GetPreventiveCareScheduleTool(careScheduleRepository, horseRepository, userRepository, support));
                handlers.add(new GetQuarantineStatusTool(areaRepository, stableStallRepository, horseRepository, healthRecordRepository));
                GetRecentIncidentReportsTool recentIncidentsTool = new GetRecentIncidentReportsTool(horseIncidentReportRepository, horseRepository, userRepository);
                handlers.add(recentIncidentsTool);
                handlers.add(new GetPendingAdmissionExamsTool(admissionApplicationRepository, careScheduleRepository, candidateHorseProfileRepository, userRepository));
                GetUrgentCareAlertsTool urgentCareTool = new GetUrgentCareAlertsTool(careScheduleRepository, horseRepository, support);
                handlers.add(urgentCareTool);
                handlers.add(new GetUrgentCareAndIncidentAlertsTool(urgentCareTool, recentIncidentsTool));
                handlers.add(new GetHorseVitalSignsTrendTool(horseHealthMetricRepository, horseRepository, support));
                handlers.add(new AnalyzeIncidentPatternsAndRecurringRisksTool(horseIncidentReportRepository, healthRecordRepository, support));
                handlers.add(new RecommendClinicalInterventionPlanTool(clinicalRetrievalService, support));
                handlers.add(new EvaluateTrainingResumptionReadinessTool(horseHealthMetricRepository, support));
        
                // GROOM (9)
                handlers.add(new GetDailyGroomTasksTool(groomDailyTaskRepository));
                handlers.add(new GetMyAssignedHorsesTool(stableStallRepository, horseRepository, areaRepository, support));
                handlers.add(new GetTodayAggregatedTasksTool(groomDailyTaskService));
                handlers.add(new GetHorseDietaryAndCareAlertsTool(healthRecordRepository, careScheduleRepository, support));
                handlers.add(new GetHorseStallLocationTool(stableStallRepository, areaRepository, userRepository, support));
                handlers.add(new GetMyReportedIncidentsTool(horseIncidentReportRepository, horseRepository, userRepository));
                handlers.add(new GetGroomTaskCompletionRateTool(groomDailyTaskRepository, support));
                handlers.add(new DetectHorseBehavioralAnomalyTool(horseIncidentReportRepository, horseHealthMetricRepository, support));
                handlers.add(new RecommendDailyCarePriorityOrderTool(groomDailyTaskRepository, horseRepository, support));
        
                // CLUB_MANAGER (11)
                handlers.add(new GetStableOccupancyMetricsTool(stableStallRepository));
                GetAdmissionRequestsOverviewTool admissionOverview = new GetAdmissionRequestsOverviewTool(admissionApplicationRepository);
                handlers.add(admissionOverview);
                handlers.add(new GetTrainerAreaDistributionTool(areaRepository, stableStallRepository, userRepository));
                handlers.add(new GetClubHealthOverviewTool(horseRepository, horseIncidentReportRepository));
                handlers.add(new GetSystemStaffOverviewTool(userRepository, trainerProfileRepository, veterinarianProfileRepository, support));
                handlers.add(new GetClubHorsesDirectoryTool(horseRepository, areaRepository, stableStallRepository, userRepository, courseRunRepository, horseSpecialtyService, support));
                handlers.add(new GetRecentAuditLogsTool(auditLogRepository, support));
                GetAdmissionPipelineDetailsTool admissionDetails = new GetAdmissionPipelineDetailsTool(admissionApplicationRepository, candidateHorseProfileRepository, horseRepository, userRepository, support);
                handlers.add(admissionDetails);
                handlers.add(new GetAdmissionPipelineOverviewTool(admissionOverview, admissionDetails));
                handlers.add(new AnalyzeStableCapacityAndStallBottlenecksTool(stableStallRepository, admissionApplicationRepository));
                handlers.add(new RecommendGroomWorkloadRebalancingTool(userRepository, stableStallRepository, groomDailyTaskRepository));
        
                return handlers;
    }
}
