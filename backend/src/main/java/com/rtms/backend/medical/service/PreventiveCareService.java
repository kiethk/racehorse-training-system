package com.rtms.backend.medical.service;

import com.rtms.backend.horse.entity.Horse;
import com.rtms.backend.horse.repository.HorseRepository;
import com.rtms.backend.medical.dto.CareScheduleResponse;
import com.rtms.backend.medical.dto.CompletePreventiveCareScheduleRequest;
import com.rtms.backend.medical.dto.CreateNextScheduleRequest;
import com.rtms.backend.medical.dto.CreatePreventiveCareScheduleRequest;
import com.rtms.backend.medical.entity.CareSchedule;
import com.rtms.backend.medical.entity.HealthRecord;
import com.rtms.backend.medical.enums.CareSchedulePurpose;
import com.rtms.backend.medical.enums.CareScheduleStatus;
import com.rtms.backend.medical.repository.CareScheduleRepository;

import com.rtms.backend.admission.entity.AdmissionApplication;
import com.rtms.backend.admission.enums.AdmissionStatus;
import com.rtms.backend.admission.repository.AdmissionApplicationRepository;
import com.rtms.backend.config.ApiException;
import com.rtms.backend.identity.entity.User;
import com.rtms.backend.identity.repository.UserRepository;
import com.rtms.backend.medical.enums.CareType;
import com.rtms.backend.security.AuthenticatedUser;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Legacy route adapter: all work is stored and scheduled through CareSchedule.
 * @deprecated Dùng {@link PreventiveStatusService} / {@link PreventiveVaccinationService}.
 */
@Deprecated
@Service
public class PreventiveCareService {
    private static final Set<String> TYPES = Set.of("ROUTINE_EXAM", "VACCINATION", "DEWORMING");
    private static final List<CareScheduleStatus> ACTIVE_PREVENTIVE_STATUSES = CareScheduleStatus.OPEN;
    private final CareScheduleRepository schedules;
    private final HorseRepository horses;
    private final CareScheduleService care;
    private final AdmissionApplicationRepository admissions;
    private final UserRepository users;

    public PreventiveCareService(CareScheduleRepository schedules, HorseRepository horses,
            CareScheduleService care, AdmissionApplicationRepository admissions, UserRepository users) {
        this.schedules = schedules;
        this.horses = horses;
        this.care = care;
        this.admissions = admissions;
        this.users = users;
    }

    @Transactional
    public CareSchedule createSchedule(CreatePreventiveCareScheduleRequest request, AuthenticatedUser user) {
        if (!TYPES.contains(request.getCareType() == null ? "" : request.getCareType())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
                    "Only ROUTINE_EXAM, VACCINATION and DEWORMING can be created here. INITIAL_EXAM is created by the Groom/admission flow");
        }
        if (request.getHorseId() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "horseId is required");
        }

        // Chỉ tiêm phòng / tẩy giun mới là mốc PREVENTIVE; khám thường luôn là FOLLOW_UP.
        // Mỗi ngựa chỉ giữ một mốc PREVENTIVE active (uq_care_schedule_one_active_preventive_per_horse).
        boolean preventive = !"ROUTINE_EXAM".equals(request.getCareType());
        CareSchedulePurpose purpose = preventive ? CareSchedulePurpose.PREVENTIVE : CareSchedulePurpose.FOLLOW_UP;

        // 1. Look for completed schedule examined by the current veterinarian
        CareSchedule source = schedules
                .findFirstByVeterinarianIdAndHorseIdAndStatusOrderByCompletedAtDesc(
                        user.getUserId(), request.getHorseId(), CareScheduleStatus.COMPLETED)
                .orElse(null);

        // 2. If none by current vet, find the most recent completed schedule for this horse regardless of vet
        if (source == null) {
            List<CareSchedule> completed = schedules.findByHorseIdAndStatusIn(
                    request.getHorseId(), List.of(CareScheduleStatus.COMPLETED));
            if (!completed.isEmpty()) {
                source = completed.stream()
                        .filter(s -> s.getCompletedAt() != null)
                        .max(Comparator.comparing(CareSchedule::getCompletedAt))
                        .orElse(completed.get(completed.size() - 1));
            }
        }

        // 3. Verify horse exists (if source is null, verify via horses repository)
        if (source == null && horses.findById(request.getHorseId()).isEmpty()) {
            throw new ApiException(HttpStatus.NOT_FOUND, "HORSE_NOT_FOUND", "Horse not found");
        }

        String scheduledDateStr = request.getScheduledDate() == null ? null : request.getScheduledDate().toString();
        String description = request.getCareType() + ": " + (request.getDescription() == null ? "" : request.getDescription().trim());
        String idempotencyKey = "preventive:" + request.getCareType() + ":" + request.getHorseId() + ":" + request.getScheduledDate();

        // 4. If current veterinarian completed the source schedule, use unified createNextSchedule flow.
        // Chỉ lịch khám lại đi đường này: createNextSchedule luôn tạo FOLLOW_UP.
        if (!preventive && source != null && Objects.equals(source.getVeterinarianId(), user.getUserId())) {
            try {
                CreateNextScheduleRequest next = new CreateNextScheduleRequest();
                next.setHorseId(request.getHorseId());
                next.setSourceScheduleId(source.getId());
                next.setCareType(CareType.ROUTINE);
                next.setScheduledDate(scheduledDateStr);
                next.setDescription(description);
                next.setIdempotencyKey(idempotencyKey);
                CareScheduleResponse created = care.createNextSchedule(next, user.getUserId());
                return schedules.findById(created.id()).orElseThrow();
            } catch (ApiException e) {
                if (e.getStatus() != HttpStatus.FORBIDDEN) {
                    throw e;
                }
                // Fallback to standalone routine care creation if rejected with FORBIDDEN
            }
        }

        // 5. Standalone routine care request (no completed schedule yet, or examined by a different vet)
        LocalDateTime requestedAt = null;
        if (request.getScheduledDate() != null) {
            requestedAt = request.getScheduledDate().atTime(14, 0);
        }
        if (requestedAt == null || !requestedAt.isAfter(LocalDateTime.now())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_DATE", "Requested schedule time must be in the future");
        }

        Long admissionId = source != null ? source.getAdmissionId() : null;
        if (admissionId != null) {
            AdmissionApplication adm = admissions.findById(admissionId).orElse(null);
            if (adm != null && adm.getStatus() == AdmissionStatus.APPROVED) {
                admissionId = null;
            }
        }
        Optional<CareSchedule> existing = schedules.findByRequestedByIdAndIdempotencyKey(user.getUserId(), idempotencyKey);
        if (existing.isPresent()) {
            boolean payloadMatches = Objects.equals(existing.get().getHorseId(), request.getHorseId())
                    && Objects.equals(existing.get().getAdmissionId(), admissionId)
                    && Objects.equals(existing.get().getDescription(), description);
            if (!payloadMatches) {
                throw new ApiException(HttpStatus.CONFLICT, "IDEMPOTENCY_CONFLICT", "Idempotency key was already used with a different payload");
            }
            return existing.get();
        }

        // 6. Mốc tiêm phòng đang chờ là duy nhất: dời mốc đó thay vì INSERT thêm dòng PREVENTIVE thứ hai.
        if (preventive) {
            Optional<CareSchedule> milestone = schedules.findFirstByHorseIdAndPurposeAndStatusIn(
                    request.getHorseId(), CareSchedulePurpose.PREVENTIVE, ACTIVE_PREVENTIVE_STATUSES);
            if (milestone.isPresent()) {
                CareSchedule active = milestone.get();
                if (active.getVeterinarianId() != null) {
                    throw new ApiException(HttpStatus.CONFLICT, "PREVENTIVE_ALREADY_SCHEDULED",
                            "This horse already has an active preventive care visit. Reschedule that visit instead");
                }
                // Mốc chưa xếp vet: dời tới ngày Vet chọn rồi xếp cho chính Vet đó.
                active.setRequestedAt(requestedAt);
                active.setDescription(description);
                CareSchedule moved = schedules.save(active);
                care.assignRequestedSchedule(moved, user.getUserId());
                return moved;
            }
        }

        CareSchedule schedule = new CareSchedule();
        schedule.setHorseId(request.getHorseId());
        schedule.setAdmissionId(admissionId);
        schedule.setSourceScheduleId(null);
        schedule.setRequestedById(user.getUserId());
        schedule.setIdempotencyKey(idempotencyKey);
        schedule.setCareType(CareType.ROUTINE);
        schedule.setPurpose(purpose);
        schedule.setStatus(CareScheduleStatus.SCHEDULED);
        schedule.setDurationMinutes(30);
        schedule.setDescription(description);
        schedule.setRequestedAt(requestedAt);

        CareSchedule saved = schedules.save(schedule);
        care.assignRequestedSchedule(saved, user.getUserId());
        return saved;
    }

    public List<CareScheduleResponse> getSchedulesByHorse(Long horseId, AuthenticatedUser user) {
        if ("HORSE_OWNER".equals(user.getRole())) {
            Horse horse = horses.findById(horseId).orElseThrow(() ->
                    new ApiException(HttpStatus.NOT_FOUND, "HORSE_NOT_FOUND", "Horse not found"));
            if (!user.getUserId().equals(horse.getOwnerId())) {
                throw new AccessDeniedException("You can only view schedules for horses you own");
            }
        }
        List<CareSchedule> list = schedules.findByHorseIdOrderByScheduledAtAsc(horseId);
        Set<Long> vetIds = list.stream()
                .map(CareSchedule::getVeterinarianId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, String> vetNames = users != null && !vetIds.isEmpty()
                ? users.findAllById(vetIds).stream()
                        .filter(u -> u.getId() != null && u.getFullName() != null)
                        .collect(Collectors.toMap(User::getId, User::getFullName))
                : Map.of();

        return list.stream()
                .map(cs -> CareScheduleResponse.from(cs, cs.getVeterinarianId() != null ? vetNames.get(cs.getVeterinarianId()) : null))
                .toList();
    }

    public HealthRecord recordCompletion(Long id, CompletePreventiveCareScheduleRequest request, AuthenticatedUser user) {
        throw new ApiException(HttpStatus.GONE, "LEGACY_ENDPOINT_RETIRED",
                "Use /api/care-schedules/{id}/start and /complete with a training decision to complete care");
    }
}
