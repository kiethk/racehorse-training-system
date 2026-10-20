package com.rtms.backend.medical.service;

import com.rtms.backend.admission.entity.AdmissionApplication;
import com.rtms.backend.admission.enums.AdmissionStatus;
import com.rtms.backend.admission.repository.AdmissionApplicationRepository;
import com.rtms.backend.horse.entity.Horse;
import com.rtms.backend.horse.enums.HorseStatus;
import com.rtms.backend.horse.repository.HorseRepository;
import com.rtms.backend.medical.dto.PreventiveProtocolStatus;
import com.rtms.backend.medical.entity.CareSchedule;
import com.rtms.backend.medical.entity.PreventiveProtocol;
import com.rtms.backend.medical.enums.CareSchedulePurpose;
import com.rtms.backend.medical.enums.CareScheduleStatus;
import com.rtms.backend.medical.enums.CareType;
import com.rtms.backend.medical.enums.PreventiveDueStatus;
import com.rtms.backend.medical.enums.RescheduleReason;
import com.rtms.backend.medical.repository.CareScheduleRepository;
import com.rtms.backend.medical.repository.PrescriptionRepository;
import com.rtms.backend.medical.repository.PrescriptionRepository.LastPreventiveDose;
import com.rtms.backend.medical.repository.PreventiveProtocolRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Trạng thái tiêm được TÍNH từ mũi tiêm gần nhất (prescriptions), không lưu cứng.
 * care_schedule chỉ là lịch hẹn: mỗi ngựa có tối đa một mốc PREVENTIVE đang chờ,
 * đặt vào ngày đến hạn sớm nhất của các phác đồ bắt buộc (docs/preventive-care-flow.md mục 2, 5.3).
 */
@Service
public class PreventiveStatusService {

    /** Mục đến hạn trong cửa sổ này được gom vào cùng một ca. */
    static final int OPPORTUNITY_WINDOW_DAYS = 7;
    static final LocalTime MILESTONE_TIME = LocalTime.of(14, 0);
    private static final List<CareScheduleStatus> ACTIVE_STATUSES = List.of(
            CareScheduleStatus.REQUESTED, CareScheduleStatus.SCHEDULED, CareScheduleStatus.IN_PROGRESS);
    private static final DateTimeFormatter DAY_MONTH = DateTimeFormatter.ofPattern("dd/MM");
    private static final DateTimeFormatter FULL_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final PreventiveProtocolRepository protocols;
    private final PrescriptionRepository prescriptions;
    private final CareScheduleRepository schedules;
    private final HorseRepository horses;
    private final AdmissionApplicationRepository admissions;
    private final Clock clock;

    @Autowired
    public PreventiveStatusService(PreventiveProtocolRepository protocols, PrescriptionRepository prescriptions,
            CareScheduleRepository schedules, HorseRepository horses, AdmissionApplicationRepository admissions) {
        this(protocols, prescriptions, schedules, horses, admissions, Clock.systemDefaultZone());
    }

    PreventiveStatusService(PreventiveProtocolRepository protocols, PrescriptionRepository prescriptions,
            CareScheduleRepository schedules, HorseRepository horses, AdmissionApplicationRepository admissions,
            Clock clock) {
        this.protocols = protocols;
        this.prescriptions = prescriptions;
        this.schedules = schedules;
        this.horses = horses;
        this.admissions = admissions;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<PreventiveProtocolStatus> statusFor(Long horseId) {
        return statusFor(List.of(horseId)).getOrDefault(horseId, List.of());
    }

    /** Trạng thái của mọi phác đồ đang active cho từng ngựa, giữ thứ tự horseIds. */
    @Transactional(readOnly = true)
    public Map<Long, List<PreventiveProtocolStatus>> statusFor(Collection<Long> horseIds) {
        Map<Long, List<PreventiveProtocolStatus>> result = new LinkedHashMap<>();
        if (horseIds == null || horseIds.isEmpty()) return result;

        List<PreventiveProtocol> active = protocols.findByActiveTrueOrderByCategoryAscIdAsc();
        Map<Long, Map<Long, LocalDate>> lastDoses = new HashMap<>();
        for (LastPreventiveDose dose : prescriptions.findLastPreventiveDoses(horseIds)) {
            lastDoses.computeIfAbsent(dose.getHorseId(), id -> new HashMap<>())
                    .put(dose.getProtocolId(), dose.getLastDoseOn());
        }

        LocalDate today = LocalDate.now(clock);
        for (Long horseId : horseIds) {
            Map<Long, LocalDate> horseDoses = lastDoses.getOrDefault(horseId, Map.of());
            List<PreventiveProtocolStatus> statuses = new ArrayList<>(active.size());
            for (PreventiveProtocol protocol : active) {
                LocalDate lastDose = horseDoses.get(protocol.getId());
                LocalDate nextDue = lastDose == null ? null : lastDose.plusDays(protocol.getIntervalDays());
                statuses.add(new PreventiveProtocolStatus(horseId, protocol.getId(), protocol.getCode(),
                        protocol.getName(), protocol.getCategory(), protocol.isMandatory(), lastDose, nextDue,
                        classify(protocol, nextDue, today)));
            }
            result.put(horseId, statuses);
        }
        return result;
    }

    static PreventiveDueStatus classify(PreventiveProtocol protocol, LocalDate nextDue, LocalDate today) {
        if (nextDue == null) return protocol.isMandatory() ? PreventiveDueStatus.MISSING : PreventiveDueStatus.NONE;
        if (nextDue.isBefore(today)) return PreventiveDueStatus.OVERDUE;
        if (!nextDue.isAfter(today.plusDays(protocol.getWarningDays()))) return PreventiveDueStatus.DUE_SOON;
        return PreventiveDueStatus.OK;
    }

    /**
     * Đặt lại mốc PREVENTIVE của ngựa theo trạng thái tiêm hiện tại. Gọi sau mỗi lần ghi nhận mũi tiêm
     * theo phác đồ, và khi Manager duyệt admission.
     *
     * @param operationKey  idempotency key khi phải tạo mốc mới, ví dụ "preventive:" + healthRecordId
     * @param missingDueOn  ngày coi như đến hạn cho phác đồ bắt buộc chưa từng tiêm (MISSING);
     *                      null = bỏ qua MISSING (ngựa còn đang khám nhập trại, chờ Manager duyệt)
     * @return mốc đang chờ sau khi upsert, rỗng nếu chưa có gì cần hẹn
     */
    @Transactional
    public Optional<CareSchedule> upsertMilestone(Long horseId, String operationKey, Long actorId,
            LocalDate missingDueOn) {
        // Khóa ngựa để hai lần ghi nhận tiêm đồng thời không cùng tạo mốc (unique index sẽ ném lỗi).
        Horse horse = horses.findByIdForUpdate(horseId).orElse(null);
        if (horse == null || horse.getCurrentStatus() == HorseStatus.REJECTED) return Optional.empty();

        Map<PreventiveProtocolStatus, LocalDate> dueByProtocol = mandatoryDueDates(horseId, missingDueOn);
        Optional<CareSchedule> existing = schedules.findFirstByHorseIdAndPurposeAndStatusIn(
                horseId, CareSchedulePurpose.PREVENTIVE, ACTIVE_STATUSES);
        if (dueByProtocol.isEmpty()) return existing;

        LocalDate target = dueByProtocol.values().stream().min(Comparator.naturalOrder()).orElseThrow();
        LocalDate today = LocalDate.now(clock);
        LocalDateTime requestedAt = (target.isAfter(today) ? target : today.plusDays(1)).atTime(MILESTONE_TIME);
        String description = describe(dueByProtocol, requestedAt.toLocalDate());

        if (existing.isPresent()) {
            CareSchedule milestone = existing.get();
            switch (milestone.getStatus()) {
                case IN_PROGRESS:
                    // Vet đang khám chính ca này; kết quả ca khám sẽ quyết định mốc kế tiếp.
                    return existing;
                case REQUESTED:
                    milestone.setRequestedAt(requestedAt);
                    milestone.setDescription(description);
                    return Optional.of(schedules.save(milestone));
                case SCHEDULED:
                    LocalDate slotDay = milestone.getScheduledAt() == null
                            ? milestone.getRequestedAt().toLocalDate()
                            : milestone.getScheduledAt().toLocalDate();
                    if (!target.isAfter(slotDay.plusDays(OPPORTUNITY_WINDOW_DAYS))) {
                        // Ca đã xếp vẫn còn việc để làm: giữ nguyên, không xáo lịch của vet.
                        // Only the list of what is still due changes.
                        milestone.setDescription(description);
                        return Optional.of(schedules.save(milestone));
                    }
                    milestone.setStatus(CareScheduleStatus.CANCELLED);
                    milestone.setCancelReason("Vaccinated early in a combined visit on " + today.format(DAY_MONTH));
                    // Flush trước khi tạo mốc mới: Hibernate chạy INSERT trước UPDATE,
                    // mốc cũ còn active sẽ đụng uq_care_schedule_one_active_preventive_per_horse.
                    schedules.saveAndFlush(milestone);
                    break;
                default:
                    break;
            }
        }

        if (operationKey != null && actorId != null) {
            Optional<CareSchedule> replay = schedules.findByRequestedByIdAndIdempotencyKey(actorId, operationKey);
            if (replay.isPresent()) return replay;
        }

        CareSchedule milestone = new CareSchedule();
        milestone.setHorseId(horseId);
        milestone.setAdmissionId(currentAdmissionId(horseId));
        milestone.setCareType(CareType.ROUTINE);
        milestone.setPurpose(CareSchedulePurpose.PREVENTIVE);
        milestone.setStatus(CareScheduleStatus.REQUESTED);
        milestone.setRequestedAt(requestedAt);
        milestone.setRequestedById(actorId);
        milestone.setIdempotencyKey(operationKey);
        milestone.setDescription(description);
        return Optional.of(schedules.save(milestone));
    }

    /**
     * Vet hoãn tiêm vì ngựa chưa đủ khỏe: đặt lại mốc như thường rồi dời nó tới ngày hoãn.
     * Trạng thái tiêm của ngựa vẫn tính trung thực (có thể là quá hạn).
     */
    @Transactional
    public Optional<CareSchedule> deferMilestone(Long horseId, String operationKey, Long actorId, LocalDate deferredTo) {
        Optional<CareSchedule> milestone = upsertMilestone(horseId, operationKey, actorId, LocalDate.now(clock));
        milestone.filter(m -> m.getStatus() == CareScheduleStatus.REQUESTED).ifPresent(m -> {
            m.setRequestedAt(deferredTo.atTime(MILESTONE_TIME));
            m.setRescheduleReason(RescheduleReason.UNDER_TREATMENT);
            schedules.save(m);
        });
        return milestone;
    }

    /**
     * Màn khám của vet mở lịch qua admission của ngựa, nên mốc gắn với admission gần nhất
     * chưa bị từ chối. Ngựa không đi qua admission (dữ liệu cũ) thì để trống.
     */
    private Long currentAdmissionId(Long horseId) {
        return admissions.findByHorseIdOrderBySubmittedAtDesc(horseId).stream()
                .filter(admission -> admission.getStatus() != AdmissionStatus.REJECTED)
                .map(AdmissionApplication::getId)
                .findFirst()
                .orElse(null);
    }

    /** Ngày đến hạn thực tế của từng phác đồ bắt buộc; MISSING chỉ có mặt khi missingDueOn != null. */
    private Map<PreventiveProtocolStatus, LocalDate> mandatoryDueDates(Long horseId, LocalDate missingDueOn) {
        Map<PreventiveProtocolStatus, LocalDate> due = new LinkedHashMap<>();
        for (PreventiveProtocolStatus status : statusFor(horseId)) {
            if (!status.mandatory()) continue;
            LocalDate date = status.nextDueOn() != null ? status.nextDueOn() : missingDueOn;
            if (date != null) due.put(status, date);
        }
        return due;
    }

    /** "Preventive care: Equine Influenza (due 18/04/2027), Deworming (no record)". */
    private static String describe(Map<PreventiveProtocolStatus, LocalDate> dueByProtocol, LocalDate visitDay) {
        LocalDate windowEnd = visitDay.plusDays(OPPORTUNITY_WINDOW_DAYS);
        String items = dueByProtocol.entrySet().stream()
                .filter(entry -> !entry.getValue().isAfter(windowEnd))
                .map(entry -> entry.getKey().name() + (entry.getKey().nextDueOn() == null
                        ? " (no record)"
                        : " (due " + entry.getKey().nextDueOn().format(FULL_DATE) + ")"))
                .collect(Collectors.joining(", "));
        return "Preventive care: " + items;
    }
}
