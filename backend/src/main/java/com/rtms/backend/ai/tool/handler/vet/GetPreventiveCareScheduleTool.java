package com.rtms.backend.ai.tool.handler.vet;

import com.rtms.backend.ai.tool.handler.AiToolHandler;
import com.rtms.backend.ai.tool.handler.AiToolSupport;
import com.rtms.backend.horse.entity.Horse;
import com.rtms.backend.horse.repository.HorseRepository;
import com.rtms.backend.identity.entity.User;
import com.rtms.backend.identity.repository.UserRepository;
import com.rtms.backend.medical.entity.CareSchedule;
import com.rtms.backend.medical.enums.CareSchedulePurpose;
import com.rtms.backend.medical.enums.CareType;
import com.rtms.backend.medical.repository.CareScheduleRepository;
import com.rtms.backend.security.AuthenticatedUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class GetPreventiveCareScheduleTool implements AiToolHandler {

    private final CareScheduleRepository careScheduleRepository;
    private final HorseRepository horseRepository;
    private final UserRepository userRepository;
    private final AiToolSupport support;

    @Autowired
    public GetPreventiveCareScheduleTool(CareScheduleRepository careScheduleRepository,
                                         HorseRepository horseRepository,
                                         @Autowired(required = false) UserRepository userRepository,
                                         AiToolSupport support) {
        this.careScheduleRepository = careScheduleRepository;
        this.horseRepository = horseRepository;
        this.userRepository = userRepository;
        this.support = support;
    }

    @Override
    public String getToolName() {
        return "getPreventiveCareSchedule";
    }

    @Override
    public Object execute(Map<String, Object> arguments, AuthenticatedUser currentUser) {
        Horse targetHorse = support.resolveHorse(arguments);
        Long horseId = targetHorse != null ? targetHorse.getId() : support.parseLong(arguments.get("horseId"));
        LocalDate from = support.parseDate(arguments.get("fromDate"), null);
        LocalDate to = support.parseDate(arguments.get("toDate"), null);

        List<CareSchedule> schedules;
        if (horseId != null) {
            schedules = careScheduleRepository.findByHorseIdOrderByScheduledAtAsc(horseId);
        } else {
            schedules = careScheduleRepository.findAll();
        }

        Map<Long, String> horseNames = horseRepository.findAll().stream()
                .collect(Collectors.toMap(Horse::getId, Horse::getName, (a, b) -> a));

        List<Map<String, Object>> result = schedules.stream()
                .filter(cs -> cs.getPurpose() == CareSchedulePurpose.PREVENTIVE || cs.getCareType() == CareType.ROUTINE)
                .filter(cs -> {
                    if (cs.getScheduledAt() == null) return false;
                    LocalDate d = cs.getScheduledAt().toLocalDate();
                    if (from != null && d.isBefore(from)) return false;
                    if (to != null && d.isAfter(to)) return false;
                    return true;
                })
                .sorted(Comparator.comparing(CareSchedule::getScheduledAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(cs -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("horseName", horseNames.getOrDefault(cs.getHorseId(), "Horse #" + cs.getHorseId()));
                    item.put("careType", cs.getCareType());
                    item.put("purpose", cs.getPurpose());
                    item.put("status", cs.getStatus());
                    item.put("durationMinutes", cs.getDurationMinutes());
                    String prevVet = "Unassigned";
                    if (cs.getVeterinarianId() != null && userRepository != null) {
                        prevVet = userRepository.findById(cs.getVeterinarianId()).map(User::getFullName).orElse("Veterinarian #" + cs.getVeterinarianId());
                    }
                    item.put("veterinarianName", prevVet);
                    item.put("rescheduleReason", cs.getRescheduleReason() != null ? cs.getRescheduleReason().name() : "N/A");
                    item.put("rescheduleNote", cs.getRescheduleNote() != null ? cs.getRescheduleNote() : "");
                    item.put("scheduledAt", cs.getScheduledAt());
                    item.put("notes", cs.getDescription());
                    return item;
                })
                .toList();

        return result;
    }
}
