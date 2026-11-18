package com.rtms.backend.ai.tool.handler.vet;

import com.rtms.backend.ai.tool.handler.AiToolHandler;
import com.rtms.backend.ai.tool.handler.AiToolSupport;
import com.rtms.backend.horse.entity.Horse;
import com.rtms.backend.horse.repository.HorseRepository;
import com.rtms.backend.medical.entity.CareSchedule;
import com.rtms.backend.medical.enums.CareScheduleStatus;
import com.rtms.backend.medical.repository.CareScheduleRepository;
import com.rtms.backend.security.AuthenticatedUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class GetVetExaminationOverviewTool implements AiToolHandler {

    private final CareScheduleRepository careScheduleRepository;
    private final HorseRepository horseRepository;
    private final AiToolSupport support;

    @Autowired
    public GetVetExaminationOverviewTool(CareScheduleRepository careScheduleRepository,
                                         HorseRepository horseRepository,
                                         AiToolSupport support) {
        this.careScheduleRepository = careScheduleRepository;
        this.horseRepository = horseRepository;
        this.support = support;
    }

    @Override
    public String getToolName() {
        return "getVetExaminationOverview";
    }

    @Override
    public Object execute(Map<String, Object> arguments, AuthenticatedUser currentUser) {
        List<CareSchedule> allSchedules = careScheduleRepository.findByVeterinarianId(currentUser.getUserId());
        List<Map<String, Object>> inProgressExams = new ArrayList<>();
        List<Map<String, Object>> chuaKham = new ArrayList<>();
        List<Map<String, Object>> daKham = new ArrayList<>();

        Map<Long, Horse> horseMap = horseRepository.findAll().stream()
                .collect(Collectors.toMap(Horse::getId, h -> h, (a, b) -> a));

        for (CareSchedule cs : allSchedules) {
            Horse h = horseMap.get(cs.getHorseId());
            String horseName = h != null ? h.getName() : "Horse #" + cs.getHorseId();
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("careScheduleId", cs.getId());
            item.put("horseName", horseName);
            item.put("stallLocation", support.resolveStallName(h != null ? h.getCurrentStallId() : null));
            item.put("durationMinutes", cs.getDurationMinutes());
            item.put("examType", cs.getCareType() != null ? cs.getCareType().name() : "General examination");
            item.put("purpose", cs.getPurpose() != null ? cs.getPurpose().name() : "N/A");
            item.put("scheduledAt", cs.getScheduledAt() != null ? cs.getScheduledAt().toString() : "N/A");
            item.put("description", cs.getDescription() != null ? cs.getDescription() : "");

            if (cs.getStatus() == CareScheduleStatus.IN_PROGRESS) {
                inProgressExams.add(item);
            } else if (cs.getStatus() == CareScheduleStatus.SCHEDULED) {
                chuaKham.add(item);
            } else if (cs.getStatus() == CareScheduleStatus.COMPLETED) {
                daKham.add(item);
            }
        }

        return Map.of(
                "totalScheduledExams", allSchedules.size(),
                "inProgressExams", inProgressExams,
                "inProgressExamsCount", inProgressExams.size(),
                "pendingScheduledExams", chuaKham,
                "pendingScheduledExamsCount", chuaKham.size(),
                "completedExams", daKham,
                "completedExamsCount", daKham.size()
        );
    }
}
