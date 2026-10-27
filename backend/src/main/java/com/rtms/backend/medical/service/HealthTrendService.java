package com.rtms.backend.medical.service;

import com.rtms.backend.medical.dto.HealthDayResponse;
import com.rtms.backend.medical.dto.HealthDayResponse.AbnormalHorse;
import com.rtms.backend.medical.dto.HealthReadingRow;
import com.rtms.backend.medical.dto.HealthTrendResponse;
import com.rtms.backend.medical.repository.HorseHealthMetricRepository;
import com.rtms.backend.security.AuthenticatedUser;
import com.rtms.backend.training.service.HorseScopeService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Biểu đồ sức khỏe của dashboard: mỗi ngày trong 30 ngày gần nhất, tính đến hôm nay, có bao nhiêu lần đo và những con nào
 * có lần đo bất thường, của các chiến mã trong phạm vi vai trò (Owner: ngựa của mình, Trainer: ngựa trong khu, vai khác:
 * toàn trang trại).
 *
 * VÌ SAO ĐẾM NGỰA BẤT THƯỜNG MÀ KHÔNG TÍNH TRUNG BÌNH: mỗi ngày đo những con khác nhau và vì lý do khác nhau (khám nhập trại,
 * khám định kỳ, khám bệnh), nên trung bình theo ngày phản ánh "hôm đó đo con nào" nhiều hơn là sức khỏe của đàn.
 * Số con có chỉ số ngoài khoảng bình thường thì không bị lệch như vậy, và chính là điều cần hành động.
 * (The health chart of the dashboard: for each of the last 30 days, up to today, how many readings were taken and which
 * horses had an abnormal one, for the horses in the scope of the role.
 * WHY COUNT ABNORMAL HORSES INSTEAD OF AVERAGING: each day measures different horses for different reasons (admission,
 * routine or illness exam), so a daily average says more about "which horse was measured that day" than about the
 * health of the herd. The number of horses outside the normal range is not skewed that way, and it is what needs action.)
 */
@Service
public class HealthTrendService {

    static final int WINDOW_DAYS = 30;

    private final HorseHealthMetricRepository metricRepository;
    private final HorseScopeService scopeService;

    public HealthTrendService(HorseHealthMetricRepository metricRepository, HorseScopeService scopeService) {
        this.metricRepository = metricRepository;
        this.scopeService = scopeService;
    }

    @Transactional(readOnly = true)
    public HealthTrendResponse getTrend(AuthenticatedUser currentUser) {
        LocalDate to = LocalDate.now();
        LocalDate from = to.minusDays(WINDOW_DAYS - 1L);
        List<Long> horseIds = scopeService.horseIdsOf(currentUser);
        List<HealthReadingRow> readings = horseIds.isEmpty()
                ? List.of()
                : metricRepository.findHealthReadings(horseIds, from.atStartOfDay(), to.plusDays(1).atStartOfDay());
        return new HealthTrendResponse(from, to, byDay(readings));
    }

    private List<HealthDayResponse> byDay(List<HealthReadingRow> readings) {
        Map<LocalDate, List<HealthReadingRow>> rowsByDate = new TreeMap<>();
        for (HealthReadingRow row : readings) {
            rowsByDate.computeIfAbsent(row.date(), date -> new ArrayList<>()).add(row);
        }
        return rowsByDate.entrySet().stream()
                .map(entry -> new HealthDayResponse(entry.getKey(), entry.getValue().size(), abnormalHorses(entry.getValue())))
                .toList();
    }

    /** Mỗi con một lần, gộp các chỉ số bất thường của mọi lần đo trong ngày. (Each horse once, the abnormal values of all its readings that day merged.) */
    private List<AbnormalHorse> abnormalHorses(List<HealthReadingRow> dayReadings) {
        Map<Long, String> names = new LinkedHashMap<>();
        Map<Long, Set<String>> issuesByHorse = new LinkedHashMap<>();
        for (HealthReadingRow row : dayReadings) {
            List<String> issues = HealthReadingPolicy.issuesOf(row.temperature(), row.heartRate(), row.bodyCondition());
            if (issues.isEmpty()) {
                continue;
            }
            names.put(row.horseId(), row.horseName());
            issuesByHorse.computeIfAbsent(row.horseId(), id -> new LinkedHashSet<>()).addAll(issues);
        }
        return issuesByHorse.entrySet().stream()
                .map(entry -> new AbnormalHorse(entry.getKey(), names.get(entry.getKey()), List.copyOf(entry.getValue())))
                .sorted(Comparator.comparing(AbnormalHorse::horseName))
                .toList();
    }
}
