package com.rtms.backend.ai.tool.handler.vet;

import com.rtms.backend.ai.tool.handler.AiToolHandler;
import com.rtms.backend.ai.tool.handler.AiToolSupport;
import com.rtms.backend.config.ApiException;
import com.rtms.backend.horse.entity.Horse;
import com.rtms.backend.identity.entity.User;
import com.rtms.backend.identity.repository.UserRepository;
import com.rtms.backend.medical.entity.HealthRecord;
import com.rtms.backend.medical.entity.InjuryRecord;
import com.rtms.backend.medical.repository.HealthRecordRepository;
import com.rtms.backend.medical.repository.InjuryRecordRepository;
import com.rtms.backend.security.AuthenticatedUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class GetHorseMedicalHistoryTool implements AiToolHandler {

    private final HealthRecordRepository healthRecordRepository;
    private final InjuryRecordRepository injuryRecordRepository;
    private final UserRepository userRepository;
    private final AiToolSupport support;

    @Autowired
    public GetHorseMedicalHistoryTool(HealthRecordRepository healthRecordRepository,
                                      @Autowired(required = false) InjuryRecordRepository injuryRecordRepository,
                                      @Autowired(required = false) UserRepository userRepository,
                                      AiToolSupport support) {
        this.healthRecordRepository = healthRecordRepository;
        this.injuryRecordRepository = injuryRecordRepository;
        this.userRepository = userRepository;
        this.support = support;
    }

    @Override
    public String getToolName() {
        return "getHorseMedicalHistory";
    }

    @Override
    public Object execute(Map<String, Object> arguments, AuthenticatedUser currentUser) {
        Horse horse = support.resolveHorse(arguments);
        if (horse == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_ARGS", "Please provide a horse ID or name");
        }
        List<HealthRecord> records = healthRecordRepository.findByHorseIdOrderByExaminedAtDesc(horse.getId());
        List<Map<String, Object>> list = new ArrayList<>();
        for (HealthRecord hr : records) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("horseName", horse.getName());
            item.put("recordType", hr.getRecordType() != null ? hr.getRecordType() : "ROUTINE");
            String docName = "Veterinarian";
            if (hr.getVeterinarianId() != null && userRepository != null) {
                docName = userRepository.findById(hr.getVeterinarianId()).map(User::getFullName).orElse("Veterinarian #" + hr.getVeterinarianId());
            }
            item.put("veterinarianName", docName);
            item.put("examDate", hr.getExaminedAt());
            item.put("clinicalObservations", hr.getFindings());
            item.put("diagnosis", hr.getDiagnosis());
            item.put("treatmentPlan", hr.getTreatment());
            item.put("trainingDecision", hr.getTrainingDecision());
            item.put("restrictionDetails", hr.getRestrictionDetails());
            item.put("notes", hr.getNotes() != null ? hr.getNotes() : "");

            if (injuryRecordRepository != null) {
                List<InjuryRecord> activeInjuries = injuryRecordRepository.findByHorseIdAndStatusIgnoreCaseOrderByDiagnosedAtDesc(horse.getId(), "ACTIVE");
                List<Map<String, Object>> injList = new ArrayList<>();
                for (InjuryRecord ir : activeInjuries) {
                    Map<String, Object> injMap = new LinkedHashMap<>();
                    injMap.put("conditionType", ir.getConditionType());
                    injMap.put("severity", ir.getSeverity() != null ? ir.getSeverity() : "MEDIUM");
                    injMap.put("description", ir.getDescription());
                    injMap.put("expectedRecoveryDate", ir.getExpectedRecoveryDate() != null ? ir.getExpectedRecoveryDate().toString() : "Not specified");
                    injMap.put("zoneId", ir.getZoneId());
                    injList.add(injMap);
                }
                item.put("activeInjuries", injList);
            }
            list.add(item);
        }
        return list;
    }
}
