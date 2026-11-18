package com.rtms.backend.ai.tool.handler.vet;

import com.rtms.backend.ai.dto.ClinicalChunkResponseDto;
import com.rtms.backend.ai.rag.service.ClinicalRetrievalService;
import com.rtms.backend.ai.tool.handler.AiToolHandler;
import com.rtms.backend.ai.tool.handler.AiToolSupport;
import com.rtms.backend.config.ApiException;
import com.rtms.backend.horse.entity.Horse;
import com.rtms.backend.security.AuthenticatedUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class RecommendClinicalInterventionPlanTool implements AiToolHandler {

    private final ClinicalRetrievalService clinicalRetrievalService;
    private final AiToolSupport support;

    @Autowired
    public RecommendClinicalInterventionPlanTool(ClinicalRetrievalService clinicalRetrievalService,
                                                  AiToolSupport support) {
        this.clinicalRetrievalService = clinicalRetrievalService;
        this.support = support;
    }

    @Override
    public String getToolName() {
        return "recommendClinicalInterventionPlan";
    }

    @Override
    public Object execute(Map<String, Object> arguments, AuthenticatedUser currentUser) {
        String symptoms = arguments.get("symptoms") != null ? arguments.get("symptoms").toString().trim() : null;
        if (symptoms == null || symptoms.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_ARGS", "Please provide a description of the clinical symptoms");
        }

        Horse horse = support.resolveHorse(arguments);
        Long horseId = horse != null ? horse.getId() : null;

        List<ClinicalChunkResponseDto> chunks = (clinicalRetrievalService != null)
                ? clinicalRetrievalService.retrieveSimilarClinicalRecords(symptoms, horseId, 3)
                : Collections.emptyList();

        List<String> refDocs = chunks.stream()
                .map(c -> c.title() != null ? c.title() + " (" + c.category() + ")" : "Clinical reference")
                .distinct()
                .toList();
        if (refDocs.isEmpty()) {
            refDocs = List.of("RTMS standard veterinary medical treatment protocol", "Racehorse clinical emergency handbook");
        }

        String proposedInterventionPlan = "Step 1 (Initial management): Immediately isolate the horse in an infirmary stall, establish an IV line for fluid and electrolyte replacement, and temporarily withhold roughage. "
                + "Step 2 (Advanced intervention): Administer a non-steroidal anti-inflammatory drug (NSAID Flunixin Meglumine 1.1 mg/kg IV) and a mucosal protectant, and monitor gut motility every 2 hours.";

        String requiredTestsLabel = "Complete blood count (CBC), plasma lactate measurement, blood chemistry to assess liver/kidney function, and abdominal or lower-limb X-ray/ultrasound.";
        String minimumObservationPeriod = "24 - 48 hours of continuous observation under the direct supervision of the veterinarian on duty.";

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("horseName", horse != null ? horse.getName() : "Horse under examination");
        result.put("registrationNumber", horse != null && horse.getRegistrationNumber() != null ? horse.getRegistrationNumber() : "N/A");
        result.put("intakeSymptoms", symptoms);
        result.put("recommendedIntervention", proposedInterventionPlan);
        result.put("requiredTests", requiredTestsLabel);
        result.put("minObservationTime", minimumObservationPeriod);
        result.put("referenceGuidelines", refDocs);
        result.put("actionUrl", "/veterinarian/urgent");
        return result;
    }
}
