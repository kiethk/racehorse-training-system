package com.rtms.backend.service;
import com.rtms.backend.dto.CareScheduleResponse;
import com.rtms.backend.dto.CompletePreventiveCareScheduleRequest;
import com.rtms.backend.dto.CreateNextScheduleRequest;
import com.rtms.backend.dto.CreatePreventiveCareScheduleRequest;
import com.rtms.backend.entity.CareSchedule;
import com.rtms.backend.entity.HealthRecord;
import com.rtms.backend.enums.CareType;
import com.rtms.backend.repository.CareScheduleRepository;
import com.rtms.backend.entity.Horse;
import com.rtms.backend.repository.HorseRepository;
import com.rtms.backend.security.AuthenticatedUser;
import com.rtms.backend.config.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Set;

/** Legacy route adapter: all work is stored and scheduled through CareSchedule. */
@Service
public class PreventiveCareService {
    private static final Set<String> TYPES = Set.of("ROUTINE_EXAM", "VACCINATION", "DEWORMING");
    private final CareScheduleRepository schedules;
    private final HorseRepository horses;
    private final CareScheduleService care;

    public PreventiveCareService(CareScheduleRepository schedules, HorseRepository horses,
            CareScheduleService care) {
        this.schedules = schedules;
        this.horses = horses;
        this.care = care;
    }

    @Transactional
    public CareSchedule createSchedule(CreatePreventiveCareScheduleRequest request, AuthenticatedUser user) {
        if (!TYPES.contains(request.getCareType() == null ? "" : request.getCareType())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
                    "Only ROUTINE_EXAM, VACCINATION and DEWORMING can be created here; INITIAL_EXAM is created by the Groom/admission flow");
        }
        CreateNextScheduleRequest next = new CreateNextScheduleRequest();
        next.setHorseId(request.getHorseId());
        CareSchedule source = schedules
                .findFirstByVeterinarianIdAndHorseIdAndStatusOrderByCompletedAtDesc(
                        user.getUserId(), request.getHorseId(), com.rtms.backend.enums.CareScheduleStatus.COMPLETED)
                .orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN",
                        "A veterinarian may only schedule preventive care for a horse they previously examined"));
        next.setSourceScheduleId(source.getId());
        next.setCareType(CareType.ROUTINE);
        next.setScheduledDate(request.getScheduledDate() == null ? null : request.getScheduledDate().toString());
        next.setDescription(request.getCareType() + ": " + (request.getDescription() == null ? "" : request.getDescription()));
        next.setIdempotencyKey("preventive:" + request.getCareType() + ":" + request.getHorseId() + ":" + request.getScheduledDate());
        CareScheduleResponse created = care.createNextSchedule(next, user.getUserId());
        return schedules.findById(created.id()).orElseThrow();
    }

    public List<CareSchedule> getSchedulesByHorse(Long horseId, AuthenticatedUser user) {
        if ("HORSE_OWNER".equals(user.getRole())) {
            Horse horse = horses.findById(horseId).orElseThrow(() ->
                    new ApiException(HttpStatus.NOT_FOUND, "HORSE_NOT_FOUND", "Horse not found"));
            if (!user.getUserId().equals(horse.getOwnerId())) {
                throw new AccessDeniedException("You can only view schedules for horses you own");
            }
        }
        return schedules.findByHorseIdOrderByScheduledAtAsc(horseId);
    }

    public HealthRecord recordCompletion(Long id, CompletePreventiveCareScheduleRequest request, AuthenticatedUser user) {
        throw new ApiException(HttpStatus.GONE, "LEGACY_ENDPOINT_RETIRED",
                "Use /api/care-schedules/{id}/start and /complete with a training decision to complete care");
    }
}
