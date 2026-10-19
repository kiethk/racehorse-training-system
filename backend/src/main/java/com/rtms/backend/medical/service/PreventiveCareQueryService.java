package com.rtms.backend.medical.service;

import com.rtms.backend.config.ApiException;
import com.rtms.backend.horse.entity.Horse;
import com.rtms.backend.horse.enums.HorseStatus;
import com.rtms.backend.horse.repository.HorseRepository;
import com.rtms.backend.medical.dto.CareScheduleResponse;
import com.rtms.backend.medical.dto.HorsePreventiveStatusResponse;
import com.rtms.backend.medical.dto.PreventiveCoverageResponse;
import com.rtms.backend.medical.dto.PreventiveDueItem;
import com.rtms.backend.medical.dto.PreventiveProtocolResponse;
import com.rtms.backend.medical.dto.PreventiveProtocolStatus;
import com.rtms.backend.medical.entity.PreventiveProtocol;
import com.rtms.backend.medical.enums.CareSchedulePurpose;
import com.rtms.backend.medical.enums.CareScheduleStatus;
import com.rtms.backend.medical.enums.PreventiveDueStatus;
import com.rtms.backend.medical.repository.CareScheduleRepository;
import com.rtms.backend.medical.repository.PreventiveProtocolRepository;
import com.rtms.backend.security.AuthenticatedUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Dữ liệu đọc cho hub Preventive Care (docs/preventive-care-flow.md mục 6, 7). */
@Service
public class PreventiveCareQueryService {

    /** Ngựa đang ở trong trại: đã duyệt, hoặc còn trong quy trình nhập trại. */
    static final Set<HorseStatus> HORSES_IN_SCOPE = EnumSet.of(HorseStatus.CANDIDATE, HorseStatus.ELIGIBLE);
    static final Set<PreventiveDueStatus> DEFAULT_DUE_STATUSES = EnumSet.of(
            PreventiveDueStatus.OVERDUE, PreventiveDueStatus.MISSING, PreventiveDueStatus.DUE_SOON);
    private static final List<CareScheduleStatus> ACTIVE = List.of(
            CareScheduleStatus.REQUESTED, CareScheduleStatus.SCHEDULED, CareScheduleStatus.IN_PROGRESS);

    private final PreventiveProtocolRepository protocols;
    private final HorseRepository horses;
    private final CareScheduleRepository schedules;
    private final PreventiveStatusService preventiveStatus;
    private final Clock clock;

    @Autowired
    public PreventiveCareQueryService(PreventiveProtocolRepository protocols, HorseRepository horses,
            CareScheduleRepository schedules, PreventiveStatusService preventiveStatus) {
        this(protocols, horses, schedules, preventiveStatus, Clock.systemDefaultZone());
    }

    PreventiveCareQueryService(PreventiveProtocolRepository protocols, HorseRepository horses,
            CareScheduleRepository schedules, PreventiveStatusService preventiveStatus, Clock clock) {
        this.protocols = protocols;
        this.horses = horses;
        this.schedules = schedules;
        this.preventiveStatus = preventiveStatus;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<PreventiveProtocolResponse> listProtocols() {
        return protocols.findByActiveTrueOrderByCategoryAscIdAsc().stream()
                .map(PreventiveProtocolResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public HorsePreventiveStatusResponse horseStatus(Long horseId, AuthenticatedUser user) {
        Horse horse = horses.findById(horseId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "HORSE_NOT_FOUND", "Horse not found"));
        if ("HORSE_OWNER".equals(user.getRole()) && !user.getUserId().equals(horse.getOwnerId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "You can only view horses you own");
        }
        CareScheduleResponse milestone = schedules
                .findFirstByHorseIdAndPurposeAndStatusIn(horseId, CareSchedulePurpose.PREVENTIVE, ACTIVE)
                .map(CareScheduleResponse::from)
                .orElse(null);
        return new HorsePreventiveStatusResponse(horse.getId(), horse.getName(),
                preventiveStatus.statusFor(horseId), milestone);
    }

    /**
     * Tab "Việc cần làm": các (ngựa, phác đồ) theo trạng thái, quá hạn / chưa có dữ liệu lên đầu.
     *
     * @param statuses   rỗng = OVERDUE, MISSING, DUE_SOON
     * @param withinDays chỉ giữ mục có hạn trong số ngày tới (mục chưa có dữ liệu luôn giữ); null = không lọc
     * @param protocolId null = mọi phác đồ
     */
    @Transactional(readOnly = true)
    public List<PreventiveDueItem> due(Set<PreventiveDueStatus> statuses, Integer withinDays, Long protocolId) {
        Set<PreventiveDueStatus> wanted = statuses == null || statuses.isEmpty() ? DEFAULT_DUE_STATUSES : statuses;
        LocalDate limit = withinDays == null ? null : LocalDate.now(clock).plusDays(withinDays);

        List<Horse> inScope = horses.findByCurrentStatusInOrderByNameAsc(HORSES_IN_SCOPE);
        Map<Long, Horse> byId = inScope.stream().collect(Collectors.toMap(Horse::getId, Function.identity()));
        Map<Long, List<PreventiveProtocolStatus>> statusByHorse = preventiveStatus.statusFor(byId.keySet());

        List<PreventiveDueItem> items = new ArrayList<>();
        for (Horse horse : inScope) {
            for (PreventiveProtocolStatus status : statusByHorse.getOrDefault(horse.getId(), List.of())) {
                if (!wanted.contains(status.status())) continue;
                if (protocolId != null && !protocolId.equals(status.protocolId())) continue;
                if (limit != null && status.nextDueOn() != null && status.nextDueOn().isAfter(limit)) continue;
                items.add(new PreventiveDueItem(horse.getId(), horse.getName(), horse.getCurrentStatus(), status));
            }
        }
        items.sort(Comparator
                .comparingInt((PreventiveDueItem item) -> urgency(item.protocol().status()))
                .thenComparing(item -> item.protocol().nextDueOn(), Comparator.nullsFirst(Comparator.naturalOrder()))
                .thenComparing(PreventiveDueItem::horseName, Comparator.nullsLast(Comparator.naturalOrder())));
        return items;
    }

    /** Tab "Theo vaccine": đếm trạng thái của từng phác đồ trên các ngựa trong trại. */
    @Transactional(readOnly = true)
    public List<PreventiveCoverageResponse> coverage() {
        List<Long> horseIds = horses.findByCurrentStatusInOrderByNameAsc(HORSES_IN_SCOPE).stream()
                .map(Horse::getId).toList();
        Map<Long, Map<PreventiveDueStatus, Integer>> counts = new java.util.HashMap<>();
        preventiveStatus.statusFor(horseIds).values().stream().flatMap(List::stream).forEach(status ->
                counts.computeIfAbsent(status.protocolId(), id -> new EnumMap<>(PreventiveDueStatus.class))
                        .merge(status.status(), 1, Integer::sum));

        List<PreventiveCoverageResponse> coverage = new ArrayList<>();
        for (PreventiveProtocol protocol : protocols.findByActiveTrueOrderByCategoryAscIdAsc()) {
            Map<PreventiveDueStatus, Integer> c = counts.getOrDefault(protocol.getId(), Map.of());
            coverage.add(new PreventiveCoverageResponse(protocol.getId(), protocol.getCode(), protocol.getName(),
                    protocol.isMandatory(), horseIds.size(),
                    c.getOrDefault(PreventiveDueStatus.OK, 0), c.getOrDefault(PreventiveDueStatus.DUE_SOON, 0),
                    c.getOrDefault(PreventiveDueStatus.OVERDUE, 0), c.getOrDefault(PreventiveDueStatus.MISSING, 0),
                    c.getOrDefault(PreventiveDueStatus.NONE, 0)));
        }
        return coverage;
    }

    private static int urgency(PreventiveDueStatus status) {
        return switch (status) {
            case OVERDUE, MISSING -> 0;
            case DUE_SOON -> 1;
            case OK -> 2;
            case NONE -> 3;
        };
    }
}
