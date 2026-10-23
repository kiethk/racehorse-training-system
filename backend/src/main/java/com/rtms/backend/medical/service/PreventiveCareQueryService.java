package com.rtms.backend.medical.service;

import com.rtms.backend.config.ApiException;
import com.rtms.backend.horse.entity.Horse;
import com.rtms.backend.horse.enums.HorseStatus;
import com.rtms.backend.horse.repository.HorseRepository;
import com.rtms.backend.medical.dto.CareScheduleResponse;
import com.rtms.backend.medical.dto.HorsePreventiveStatusResponse;
import com.rtms.backend.medical.dto.PreventiveCoverageResponse;
import com.rtms.backend.medical.dto.PreventiveDueHorse;
import com.rtms.backend.medical.dto.PreventiveDueSummary;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
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
import java.util.Locale;
import java.util.Map;
import java.util.Set;

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
     * Tab "To do": mỗi dòng một ngựa, kèm các phác đồ khớp bộ lọc; ngựa khẩn nhất lên đầu.
     * Trạng thái được tính trong bộ nhớ (số ngựa trong trại nhỏ), rồi mới cắt trang.
     *
     * @param statuses   rỗng = OVERDUE, MISSING, DUE_SOON
     * @param protocolId null = mọi phác đồ
     * @param search     lọc theo tên ngựa, không phân biệt hoa thường; rỗng = không lọc
     */
    @Transactional(readOnly = true)
    public Page<PreventiveDueHorse> dueHorses(Set<PreventiveDueStatus> statuses, Long protocolId, String search,
            int page, int size) {
        Set<PreventiveDueStatus> wanted = statuses == null || statuses.isEmpty() ? DEFAULT_DUE_STATUSES : statuses;
        String needle = search == null ? "" : search.trim().toLowerCase(Locale.ROOT);
        List<Horse> inScope = horses.findByCurrentStatusInOrderByNameAsc(HORSES_IN_SCOPE).stream()
                .filter(h -> needle.isEmpty()
                        || (h.getName() != null && h.getName().toLowerCase(Locale.ROOT).contains(needle)))
                .toList();
        Map<Long, List<PreventiveProtocolStatus>> statusByHorse =
                preventiveStatus.statusFor(inScope.stream().map(Horse::getId).toList());

        List<PreventiveDueHorse> rows = new ArrayList<>();
        for (Horse horse : inScope) {
            List<PreventiveProtocolStatus> matching = statusByHorse.getOrDefault(horse.getId(), List.of()).stream()
                    .filter(status -> wanted.contains(status.status()))
                    .filter(status -> protocolId == null || protocolId.equals(status.protocolId()))
                    .sorted(BY_URGENCY)
                    .toList();
            if (matching.isEmpty()) continue;
            PreventiveProtocolStatus worst = matching.get(0);
            rows.add(new PreventiveDueHorse(horse.getId(), horse.getName(), horse.getCurrentStatus(), matching,
                    worst.nextDueOn(), worst.status()));
        }
        rows.sort(Comparator
                .comparingInt((PreventiveDueHorse row) -> urgency(row.worstStatus()))
                .thenComparing(PreventiveDueHorse::earliestDueOn, Comparator.nullsFirst(Comparator.naturalOrder()))
                .thenComparing(PreventiveDueHorse::horseName, Comparator.nullsLast(Comparator.naturalOrder())));

        int pageSize = Math.max(1, Math.min(size, 100));
        int pageIndex = Math.max(0, page);
        int from = Math.min(pageIndex * pageSize, rows.size());
        int to = Math.min(from + pageSize, rows.size());
        return new PageImpl<>(rows.subList(from, to), PageRequest.of(pageIndex, pageSize), rows.size());
    }

    /** Số liệu cho các thẻ của tab "To do", trên toàn bộ ngựa trong trại (không phụ thuộc trang hay bộ lọc). */
    @Transactional(readOnly = true)
    public PreventiveDueSummary dueSummary() {
        List<Long> horseIds = horses.findByCurrentStatusInOrderByNameAsc(HORSES_IN_SCOPE).stream()
                .map(Horse::getId).toList();
        int overdue = 0;
        int dueSoon = 0;
        int horsesNeedingCare = 0;
        for (List<PreventiveProtocolStatus> statuses : preventiveStatus.statusFor(horseIds).values()) {
            boolean needsCare = false;
            for (PreventiveProtocolStatus status : statuses) {
                if (status.status() == PreventiveDueStatus.OVERDUE || status.status() == PreventiveDueStatus.MISSING) {
                    overdue++;
                    needsCare = true;
                } else if (status.status() == PreventiveDueStatus.DUE_SOON) {
                    dueSoon++;
                    needsCare = true;
                }
            }
            if (needsCare) horsesNeedingCare++;
        }
        return new PreventiveDueSummary(overdue, dueSoon, horsesNeedingCare);
    }

    private static final Comparator<PreventiveProtocolStatus> BY_URGENCY = Comparator
            .comparingInt((PreventiveProtocolStatus status) -> urgency(status.status()))
            .thenComparing(PreventiveProtocolStatus::nextDueOn, Comparator.nullsFirst(Comparator.naturalOrder()));

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
