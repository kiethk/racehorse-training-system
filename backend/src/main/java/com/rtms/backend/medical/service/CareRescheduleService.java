package com.rtms.backend.medical.service;

import com.rtms.backend.audit.entity.AuditLog;
import com.rtms.backend.audit.repository.AuditLogRepository;
import com.rtms.backend.config.ApiException;
import com.rtms.backend.identity.repository.UserRepository;
import com.rtms.backend.medical.dto.CareScheduleResponse;
import com.rtms.backend.medical.dto.PreventiveProtocolStatus;
import com.rtms.backend.medical.dto.RescheduleCareScheduleRequest;
import com.rtms.backend.medical.dto.RescheduleCareScheduleResponse;
import com.rtms.backend.medical.entity.CareSchedule;
import com.rtms.backend.medical.enums.CareSchedulePurpose;
import com.rtms.backend.medical.enums.CareScheduleStatus;
import com.rtms.backend.medical.enums.CareType;
import com.rtms.backend.medical.repository.CareScheduleRepository;
import com.rtms.backend.security.AuthenticatedUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Dời ngày hẹn của lịch khám thường / mốc tiêm phòng (docs/preventive-care-flow.md mục 5.5).
 * Lịch đã xếp vet bị trả về REQUESTED để scheduler xếp lại slot ở ngày mới.
 */
@Service
public class CareRescheduleService {

    static final String AUDIT_ACTION = "CARE_SCHEDULE_RESCHEDULED";
    private static final String MANAGER = "CLUB_MANAGER";
    private static final String VET = "VETERINARIAN";
    private static final DateTimeFormatter FULL_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final CareScheduleRepository schedules;
    private final UserRepository users;
    private final AuditLogRepository auditLogs;
    private final PreventiveStatusService preventiveStatus;
    private final CareScheduleService careScheduleService;
    private final Clock clock;

    @Autowired
    public CareRescheduleService(CareScheduleRepository schedules, UserRepository users,
            AuditLogRepository auditLogs, PreventiveStatusService preventiveStatus,
            CareScheduleService careScheduleService) {
        this(schedules, users, auditLogs, preventiveStatus, careScheduleService, Clock.systemDefaultZone());
    }

    public CareRescheduleService(CareScheduleRepository schedules, UserRepository users,
            AuditLogRepository auditLogs, PreventiveStatusService preventiveStatus) {
        this(schedules, users, auditLogs, preventiveStatus, null, Clock.systemDefaultZone());
    }

    CareRescheduleService(CareScheduleRepository schedules, UserRepository users, AuditLogRepository auditLogs,
            PreventiveStatusService preventiveStatus, Clock clock) {
        this(schedules, users, auditLogs, preventiveStatus, null, clock);
    }

    CareRescheduleService(CareScheduleRepository schedules, UserRepository users, AuditLogRepository auditLogs,
            PreventiveStatusService preventiveStatus, CareScheduleService careScheduleService, Clock clock) {
        this.schedules = schedules;
        this.users = users;
        this.auditLogs = auditLogs;
        this.preventiveStatus = preventiveStatus;
        this.careScheduleService = careScheduleService;
        this.clock = clock;
    }

    @Transactional
    public RescheduleCareScheduleResponse reschedule(Long scheduleId, RescheduleCareScheduleRequest request,
            AuthenticatedUser actor) {
        if (request.getNewDate() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "newDate is required");
        }
        if (request.getReason() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Reschedule reason is required");
        }
        LocalDate today = LocalDate.now(clock);
        if (!request.getNewDate().isAfter(today)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_DATE", "The new date must be in the future");
        }

        CareSchedule schedule = schedules.findByIdForUpdate(scheduleId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "SCHEDULE_NOT_FOUND", "Care schedule not found"));
        // Khám nhập trại gắn với admission, khám khẩn cấp phải làm ngay: không dời.
        if (schedule.getCareType() != CareType.ROUTINE) {
            throw new ApiException(HttpStatus.CONFLICT, "UNSUPPORTED_CARE_TYPE", "Only ROUTINE schedules can be rescheduled");
        }
        authorize(schedule, actor);

        if (schedule.getStatus() == CareScheduleStatus.SCHEDULED) {
            schedule.setVeterinarianId(null);
            schedule.setScheduledAt(null);
            schedule.setStatus(CareScheduleStatus.REQUESTED);
        }
        schedule.setRequestedAt(request.getNewDate().atTime(PreventiveStatusService.MILESTONE_TIME));
        schedule.setRescheduleReason(request.getReason());
        schedule.setRescheduleNote(request.getNote() == null || request.getNote().isBlank() ? null : request.getNote().trim());
        CareSchedule saved = schedules.save(schedule);
        if (careScheduleService != null) {
            careScheduleService.assignRequestedSchedule(saved);
        }

        AuditLog audit = new AuditLog();
        audit.setAction("Care Schedule Rescheduled");
        audit.setEntityName("CareSchedule");
        audit.setEntityId(saved.getId());
        audit.setUser(users.findById(actor.getUserId()).orElse(null));
        audit.setDetails("Care schedule #" + saved.getId() + " rescheduled to " + request.getNewDate() + (request.getReason() != null ? " (Reason: " + request.getReason() + ")" : ""));
        auditLogs.save(audit);

        return new RescheduleCareScheduleResponse(CareScheduleResponse.from(saved), warnings(saved, request.getNewDate()));
    }

    private static void authorize(CareSchedule schedule, AuthenticatedUser actor) {
        boolean manager = MANAGER.equals(actor.getRole());
        switch (schedule.getStatus()) {
            case REQUESTED:
                // Chưa có vet được giao: chỉ Manager điều phối.
                if (!manager) {
                    throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN",
                            "Only club managers can reschedule an unassigned schedule");
                }
                return;
            case SCHEDULED:
                boolean assignedVet = VET.equals(actor.getRole())
                        && Objects.equals(schedule.getVeterinarianId(), actor.getUserId());
                if (!manager && !assignedVet) {
                    throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN",
                            "Only the assigned veterinarian or a club manager can reschedule this schedule");
                }
                return;
            default:
                throw new ApiException(HttpStatus.CONFLICT, "INVALID_STATUS",
                        "Only REQUESTED or SCHEDULED schedules can be rescheduled");
        }
    }

    /** Cảnh báo khi mốc tiêm phòng bị dời qua hạn của một phác đồ bắt buộc. Không nhắc tới thi đấu. */
    private List<String> warnings(CareSchedule schedule, LocalDate newDate) {
        List<String> warnings = new ArrayList<>();
        if (schedule.getPurpose() != CareSchedulePurpose.PREVENTIVE) return warnings;
        for (PreventiveProtocolStatus status : preventiveStatus.statusFor(schedule.getHorseId())) {
            if (!status.mandatory()) continue;
            if (status.nextDueOn() == null) {
                warnings.add("The horse has no record for " + status.name() + ".");
            } else if (status.nextDueOn().isBefore(newDate)) {
                warnings.add("The horse will be overdue for " + status.name() + " from "
                        + status.nextDueOn().format(FULL_DATE) + ".");
            }
        }
        return warnings;
    }
}
