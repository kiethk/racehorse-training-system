package com.rtms.backend.ai.tool;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rtms.backend.admission.repository.AdmissionApplicationRepository;
import com.rtms.backend.admission.repository.CandidateHorseProfileRepository;
import com.rtms.backend.ai.rag.service.ClinicalRetrievalService;
import com.rtms.backend.ai.tool.handler.AiToolHandler;
import com.rtms.backend.audit.repository.AuditLogRepository;
import com.rtms.backend.config.ApiException;
import com.rtms.backend.horse.repository.HorsePedigreeRepository;
import com.rtms.backend.horse.repository.HorseRepository;
import com.rtms.backend.identity.repository.TrainerProfileRepository;
import com.rtms.backend.identity.repository.UserRepository;
import com.rtms.backend.identity.repository.VeterinarianProfileRepository;
import com.rtms.backend.incident.repository.HorseIncidentReportRepository;
import com.rtms.backend.medical.repository.*;
import com.rtms.backend.security.AuthenticatedUser;
import com.rtms.backend.stable.repository.AreaRepository;
import com.rtms.backend.stable.repository.GroomDailyTaskRepository;
import com.rtms.backend.stable.repository.GroomTaskCompletionRepository;
import com.rtms.backend.stable.repository.StableStallRepository;
import com.rtms.backend.stable.service.GroomDailyTaskService;
import com.rtms.backend.training.repository.*;
import com.rtms.backend.training.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Service that coordinates and executes AI tools using the Strategy Pattern (AiToolHandler).
 */
@Service
public class AiToolExecutionService {

    private static final Logger log = LoggerFactory.getLogger(AiToolExecutionService.class);

    private final DynamicToolRegistryService toolRegistryService;
    private final Map<String, AiToolHandler> handlerMap;

    /**
     * Primary constructor for Spring Boot DI (automatically collects all AiToolHandler beans).
     */
    @Autowired
    public AiToolExecutionService(DynamicToolRegistryService toolRegistryService,
                                  List<AiToolHandler> handlers) {
        this.toolRegistryService = toolRegistryService;
        Map<String, AiToolHandler> map = new HashMap<>();
        if (handlers != null) {
            for (AiToolHandler handler : handlers) {
                map.put(handler.getToolName(), handler);
            }
        }
        this.handlerMap = Collections.unmodifiableMap(map);
        log.info("AiToolExecutionService: Loaded {} AI tools into the registry.", this.handlerMap.size());
    }

    /**
     * Backward-compatible constructor for unit tests that do not load the Spring context.
     * @deprecated Use the Spring DI constructor {@link #AiToolExecutionService(DynamicToolRegistryService, List)} instead.
     */
    @Deprecated(forRemoval = false)
    public AiToolExecutionService(
            DynamicToolRegistryService toolRegistryService,
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
        this(toolRegistryService, AiToolHandlerFactory.create(
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
                courseRunRepository,
                horseSpecialtyService,
                horsePerformanceService,
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
        ));
    }

    /**
     * Executes a tool at the request of the LLM.
     */
    @Transactional(readOnly = true)
    public Object executeTool(String toolName, Map<String, Object> arguments, AuthenticatedUser currentUser) {
        if (currentUser == null || currentUser.getUserId() == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "The user is not authenticated");
        }

        // 1. Check whether the user's role is authorized to call the tool
        if (!toolRegistryService.isToolAllowedForRole(toolName, currentUser.getRole())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "TOOL_FORBIDDEN",
                    "Role [" + currentUser.getRole() + "] is not authorized to execute tool [" + toolName + "]");
        }

        AiToolHandler handler = handlerMap.get(toolName);
        if (handler == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "TOOL_NOT_FOUND", "Tool not found: " + toolName);
        }

        Map<String, Object> args = arguments != null ? arguments : Collections.emptyMap();
        log.info("AiToolExecutionService: User [{}] ({}) called tool [{}] with args {}",
                currentUser.getUserId(), currentUser.getRole(), toolName, args);

        return handler.execute(args, currentUser);
    }

    public Map<String, AiToolHandler> getHandlerMap() {
        return handlerMap;
    }
}
