package com.rtms.backend.training.service;

import com.rtms.backend.notification.dto.HorseAlertResponse;
import com.rtms.backend.security.AuthenticatedUser;
import com.rtms.backend.training.dto.DashboardHorseResponse;
import com.rtms.backend.training.dto.DashboardHorseRow;
import com.rtms.backend.training.dto.DashboardCourseRunRow;
import com.rtms.backend.training.dto.HorseFitnessTrendItemResponse;
import com.rtms.backend.training.dto.HorseScheduleItemResponse;
import com.rtms.backend.training.dto.HorseSpecialtyResponse;
import com.rtms.backend.training.dto.PerformanceSessionRow;
import com.rtms.backend.training.dto.PerformanceTrendResponse;
import com.rtms.backend.training.dto.RecentWorkoutRow;
import com.rtms.backend.training.enums.CourseRunStatus;
import com.rtms.backend.training.repository.CourseRunRepository;
import com.rtms.backend.training.repository.TrainingWorkoutRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Theo dõi phong độ và thể lực chiến mã: biểu đồ thể lực, cảnh báo nguy cơ, và bảng tiến độ của Trainer.
 * (Tracks horse performance and fitness: the fitness chart, risk alerts, and the trainer progress board.)
 *
 * Mọi dữ liệu được tính khi đọc từ các buổi tập đã hoàn thành, không lưu bảng riêng.
 * (Everything is computed on read from completed workouts; nothing is stored in a table of its own.)
 */
@Service
public class HorsePerformanceService {

    private static final int RECENT_WORKOUT_DAYS = 30;
    private static final int TREND_WEEKS = 12;
    public static final int MAX_HEART_RATE_LIMIT = 220;
    public static final int AVERAGE_HEART_RATE_LIMIT = 180;
    public static final int RECOVERY_HEART_RATE_LIMIT = 100;
    private static final LocalTime DEFAULT_END_TIME = LocalTime.of(10, 0);

    private final TrainingWorkoutRepository workoutRepository;
    private final CourseRunRepository courseRunRepository;
    private final HorseViewAccessService accessService;
    private final HorseSpecialtyService specialtyService;
    private final HorseScopeService scopeService;

    public HorsePerformanceService(TrainingWorkoutRepository workoutRepository,
                                   CourseRunRepository courseRunRepository,
                                   HorseViewAccessService accessService,
                                   HorseSpecialtyService specialtyService,
                                   HorseScopeService scopeService) {
        this.workoutRepository = workoutRepository;
        this.courseRunRepository = courseRunRepository;
        this.accessService = accessService;
        this.specialtyService = specialtyService;
        this.scopeService = scopeService;
    }

    // =================================================================
    // BIỂU ĐỒ THỂ LỰC (Fitness chart)
    // =================================================================

    /**
     * Chuỗi thời gian các buổi tập đã hoàn thành của một chiến mã (FE-7.2).
     * (Time series of a horse completed workouts.)
     */
    @Transactional(readOnly = true)
    public List<HorseFitnessTrendItemResponse> getFitnessTrend(Long horseId, LocalDate fromDate,
                                                               LocalDate toDate, AuthenticatedUser currentUser) {
        accessService.assertCanViewHorse(horseId, currentUser);

        // Bỏ trống thì dùng mốc bao trùm, để câu truy vấn luôn có BETWEEN cụ thể và khỏi phải viết
        // "(:from IS NULL OR ...)" vốn hay lỗi ép kiểu tham số null trên PostgreSQL.
        // (When empty, wide bounds are used so the query always has a concrete BETWEEN, avoiding
        // "(:from IS NULL OR ...)" which often breaks null parameter typing on PostgreSQL.)
        LocalDate from = fromDate != null ? fromDate : LocalDate.of(1900, 1, 1);
        LocalDate to = toDate != null ? toDate : LocalDate.of(2999, 12, 31);
        return workoutRepository.findFitnessTrend(horseId, from, to);
    }

    /**
     * Lịch tập của một con ngựa trong khoảng ngày (vai nào xem được ngựa thì xem được lịch).
     * (A horse training schedule over a date range; whoever may view the horse may view it.)
     */
    @Transactional(readOnly = true)
    public List<HorseScheduleItemResponse> getHorseSchedule(Long horseId, LocalDate from, LocalDate to,
                                                            AuthenticatedUser currentUser) {
        accessService.assertCanViewHorse(horseId, currentUser);
        if (to.isBefore(from)) {
            throw new IllegalArgumentException("The end date cannot be before the start date");
        }
        return workoutRepository.findHorseSchedule(horseId, from, to);
    }

    // =================================================================
    // CẢNH BÁO (Alerts)
    // =================================================================

    /**
     * Bộ luật cảnh báo nguy cơ chấn thương và suy giảm thể lực, tính động theo dữ liệu thời gian thực.
     * (The rule set for injury risk and fitness decline, computed dynamically from live data.)
     */
    @Transactional(readOnly = true)
    public List<HorseAlertResponse> getHorseAlerts(Long horseId, AuthenticatedUser currentUser) {
        accessService.assertCanViewHorse(horseId, currentUser);

        List<RecentWorkoutRow> recent = workoutRepository.findRecentCompletedWorkouts(
                List.of(horseId), LocalDate.now().minusDays(RECENT_WORKOUT_DAYS));
        return evaluateAlerts(recent);
    }

    /**
     * Áp dụng các luật lên các buổi tập gần đây (mới nhất trước). Dùng chung cho cả getHorseAlerts và dashboard.
     * (Applies the rules to recent workouts (newest first). Shared by getHorseAlerts and the dashboard.)
     *
     * Luật "khối lượng tăng đột biến" đã bị bỏ: khóa XOAY VÒNG bài tập có cự ly rất khác nhau nên tổng cự ly
     * tuần dao động theo lịch xoay chứ không theo tải thật, gây báo động giả.
     * (The "sudden volume spike" rule was dropped: the course ROTATES exercises of very different distance, so
     * weekly distance varies with the rotation rather than the real load, causing false alarms.)
     *
     * Luật "sự cố lặp lại" cũng đã bị bỏ: nó đếm cả báo cáo do chính Trainer gửi từ thẻ cảnh báo, nên một lần
     * "Report to Vet" có thể tự sinh thêm cảnh báo; sự cố đã có màn hình riêng để theo dõi.
     * (The "repeated incidents" rule was dropped too: it also counted reports the trainer sent from an alert card,
     * so one "Report to Vet" could raise another alert; incidents have their own screen.)
     */
    private List<HorseAlertResponse> evaluateAlerts(List<RecentWorkoutRow> recent) {
        List<HorseAlertResponse> alerts = new ArrayList<>();
        maxHeartRateAlert(recent).ifPresent(alerts::add);
        recoveryHeartRateAlert(recent).ifPresent(alerts::add);
        averageHeartRateAlert(recent).ifPresent(alerts::add);
        return alerts;
    }

    /** Luật 1: nhịp tim tối đa vượt ngưỡng; chỉ báo mốc gần nhất. (Rule 1: max heart rate over the limit; newest only.) */
    private Optional<HorseAlertResponse> maxHeartRateAlert(List<RecentWorkoutRow> recent) {
        return recent.stream()
                .filter(r -> r.maxHeartRate() != null && r.maxHeartRate() > MAX_HEART_RATE_LIMIT)
                .findFirst()
                .map(r -> new HorseAlertResponse(
                        "MAX_HEART_RATE_EXCEEDED",
                        "Maximum heart rate above safe limit",
                        String.format("Recorded a maximum heart rate of %d bpm (safe limit <= %d bpm) on %s.",
                                r.maxHeartRate(), MAX_HEART_RATE_LIMIT, r.sessionDate()),
                        "DANGER",
                        triggeredAt(r),
                        r.maxHeartRate().doubleValue(),
                        (double) MAX_HEART_RATE_LIMIT));
    }

    /** Luật 2: hồi phục tim kém. (Rule 2: poor heart-rate recovery.) */
    private Optional<HorseAlertResponse> recoveryHeartRateAlert(List<RecentWorkoutRow> recent) {
        return recent.stream()
                .filter(r -> r.recoveryHeartRate() != null && r.recoveryHeartRate() > RECOVERY_HEART_RATE_LIMIT)
                .findFirst()
                .map(r -> new HorseAlertResponse(
                        "POOR_RECOVERY_HEART_RATE",
                        "Poor heart-rate recovery",
                        String.format("Post-workout recovery heart rate is abnormally high (%d bpm, safe limit <= %d bpm) on %s.",
                                r.recoveryHeartRate(), RECOVERY_HEART_RATE_LIMIT, r.sessionDate()),
                        "DANGER",
                        triggeredAt(r),
                        r.recoveryHeartRate().doubleValue(),
                        (double) RECOVERY_HEART_RATE_LIMIT));
    }

    /** Luật 3: nhịp tim trung bình vượt ngưỡng; chỉ báo mốc gần nhất. (Rule 3: average heart rate over the limit; newest only.) */
    private Optional<HorseAlertResponse> averageHeartRateAlert(List<RecentWorkoutRow> recent) {
        return recent.stream()
                .filter(r -> r.averageHeartRate() != null && r.averageHeartRate() > AVERAGE_HEART_RATE_LIMIT)
                .findFirst()
                .map(r -> new HorseAlertResponse(
                        "AVERAGE_HEART_RATE_EXCEEDED",
                        "Elevated average heart rate",
                        String.format("Recorded an average heart rate of %d bpm (safe limit <= %d bpm) on %s.",
                                r.averageHeartRate(), AVERAGE_HEART_RATE_LIMIT, r.sessionDate()),
                        "WARNING",
                        triggeredAt(r),
                        r.averageHeartRate().doubleValue(),
                        (double) AVERAGE_HEART_RATE_LIMIT));
    }

    private LocalDateTime triggeredAt(RecentWorkoutRow row) {
        return row.sessionDate().atTime(row.sessionEndTime() != null ? row.sessionEndTime() : DEFAULT_END_TIME);
    }

    // =================================================================
    // DASHBOARD THEO VAI TRÒ (Role dashboard)
    // =================================================================

    /**
     * Bảng tiến độ và thể lực của các chiến mã trong phạm vi của vai trò (FE-7.1): Owner thấy ngựa của mình, Trainer
     * thấy ngựa trong khu, Manager thấy toàn trang trại.
     * (Progress and fitness board of the horses in the scope of the role (FE-7.1): an owner sees own horses, a
     * trainer the horses of the areas, the manager the whole farm.)
     *
     * Số truy vấn là HẰNG SỐ, không phụ thuộc số ngựa: ngựa trong khu, kế hoạch, buổi tập 30 ngày,
     * buổi tập tính sở trường, đều lấy một lượt rồi ghép theo horseId.
     * (The number of queries is CONSTANT, independent of the horse count: area horses, course runs, 30-day
     * workouts and the specialty workouts are each fetched once and joined by horseId.)
     */
    @Transactional(readOnly = true)
    public List<DashboardHorseResponse> getDashboard(AuthenticatedUser currentUser) {
        List<DashboardHorseRow> horses = scopeService.horsesOf(currentUser);
        if (horses.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> horseIds = horses.stream().map(DashboardHorseRow::horseId).toList();

        Map<Long, List<DashboardCourseRunRow>> courseRunsByHorse = courseRunRepository.findDashboardCourseRuns(horseIds).stream()
                .collect(Collectors.groupingBy(DashboardCourseRunRow::horseId));
        Map<Long, List<RecentWorkoutRow>> recentByHorse = workoutRepository
                .findRecentCompletedWorkouts(horseIds, LocalDate.now().minusDays(RECENT_WORKOUT_DAYS)).stream()
                .collect(Collectors.groupingBy(RecentWorkoutRow::horseId));
        Map<Long, HorseSpecialtyResponse> specialtyByHorse = specialtyService.getSpecialties(horseIds);

        return horses.stream()
                .map(horse -> buildDashboardRow(
                        horse,
                        courseRunsByHorse.getOrDefault(horse.horseId(), List.of()),
                        recentByHorse.getOrDefault(horse.horseId(), List.of()),
                        specialtyByHorse.get(horse.horseId())))
                .toList();
    }

    private DashboardHorseResponse buildDashboardRow(DashboardHorseRow horse,
                                                            List<DashboardCourseRunRow> courseRuns,
                                                            List<RecentWorkoutRow> recent,
                                                            HorseSpecialtyResponse specialty) {
        DashboardHorseResponse response = new DashboardHorseResponse();
        response.setHorseId(horse.horseId());
        response.setHorseName(horse.horseName());
        response.setBreed(horse.breed());
        response.setStallCode(horse.stallCode());

        fillCourseRunProgress(response, pickCurrentCourseRun(courseRuns));
        fillRatingStats(response, recent);

        List<HorseAlertResponse> alerts = evaluateAlerts(recent);
        response.setAlertsCount(alerts.size());
        response.setAlertTitles(alerts.stream().map(HorseAlertResponse::getTitle).toList());
        response.setSpecialty(specialty);
        return response;
    }

    /**
     * Biểu đồ phong độ chung trên dashboard: trung bình từng tuần của các buổi đã hoàn thành trong 12 tuần gần nhất (tuần
     * hiện tại tính đến hôm nay), của các chiến mã trong phạm vi vai trò. Mỗi con được tính một lần mỗi tuần.
     * (Performance chart on the dashboard: the weekly averages of the sessions completed in the last 12 weeks (the
     * current week up to today), for the horses in the scope of the role. Each horse counts once per week.)
     */
    @Transactional(readOnly = true)
    public PerformanceTrendResponse getPerformanceTrend(AuthenticatedUser currentUser) {
        LocalDate to = LocalDate.now();
        LocalDate from = PerformanceTrendCalculator.weekStartOf(to).minusWeeks(TREND_WEEKS - 1L);
        List<Long> horseIds = scopeService.horseIdsOf(currentUser);
        List<PerformanceSessionRow> sessions = horseIds.isEmpty()
                ? List.of()
                : workoutRepository.findPerformanceSessions(horseIds, from, to);
        return new PerformanceTrendResponse(from, to, PerformanceTrendCalculator.byWeek(sessions));
    }

    /**
     * Kế hoạch ưu tiên, chọn theo NGÀY chứ không theo thứ tự tạo: kế hoạch đang chạy; nếu không có thì kế hoạch sắp tới
     * bắt đầu sớm nhất; nếu không có thì kế hoạch đã kết thúc gần đây nhất. null nếu ngựa chưa có kế hoạch.
     * (Preferred course run, chosen by DATE rather than creation order: the running course run; otherwise the upcoming course run that
     * starts first; otherwise the course run that ended most recently. null if the horse has no course run.)
     */
    static DashboardCourseRunRow pickCurrentCourseRun(List<DashboardCourseRunRow> courseRuns) {
        Optional<DashboardCourseRunRow> active = courseRuns.stream()
                .filter(courseRun -> courseRun.status() == CourseRunStatus.ACTIVE)
                .min(Comparator.comparing(DashboardCourseRunRow::startDate));
        if (active.isPresent()) {
            return active.get();
        }
        Optional<DashboardCourseRunRow> nextUpcoming = courseRuns.stream()
                .filter(courseRun -> courseRun.status() == CourseRunStatus.UPCOMING)
                .min(Comparator.comparing(DashboardCourseRunRow::startDate));
        if (nextUpcoming.isPresent()) {
            return nextUpcoming.get();
        }
        return courseRuns.stream()
                .max(Comparator.comparing(DashboardCourseRunRow::endDate))
                .orElse(null);
    }

    private void fillCourseRunProgress(DashboardHorseResponse response, DashboardCourseRunRow courseRun) {
        if (courseRun == null) {
            response.setCourseRunStatus("NONE");
            response.setCourseName("No course run yet");
            response.setCompletedSessions(0);
            response.setTotalSessions(0);
            response.setCancelledSessions(0);
            response.setProgressPercent(0.0);
            return;
        }

        int total = courseRun.totalSessions() != null ? courseRun.totalSessions() : 0;
        int completed = courseRun.completedSessions().intValue();
        int cancelled = courseRun.cancelledSessions().intValue();
        // Giống trang Course run: buổi bị hủy ra khỏi mẫu số (khóa 6 buổi hủy 2 đạt 100% khi tập đủ 4), trừ khi cả kế hoạch bị
        // hủy giữa chừng: khi đó tập 3 trên 6 là 50%, không phải 100%.
        // (Like the Course run page: cancelled sessions leave the denominator (a 6-session course run with 2 cancelled reaches 100%
        // after 4), unless the whole course run was dropped part-way: then 3 held of 6 is 50%, not 100%.)
        int counted = courseRun.status() == CourseRunStatus.CANCELLED ? total : Math.max(total - cancelled, 0);

        response.setCourseRunId(courseRun.courseRunId());
        response.setCourseRunStatus(courseRun.status().name());
        response.setStartDate(courseRun.startDate());
        response.setEndDate(courseRun.endDate());
        response.setCourseName(courseRun.courseName());
        response.setCompletedSessions(completed);
        response.setTotalSessions(total > 0 ? total : completed);
        response.setCancelledSessions(cancelled);
        response.setProgressPercent(counted > 0 ? round1(Math.min(completed * 100.0 / counted, 100.0)) : 0.0);
    }

    /** Điểm phong độ mới nhất và trung bình 30 ngày. (Latest rating and the 30-day average.) */
    private void fillRatingStats(DashboardHorseResponse response, List<RecentWorkoutRow> recent) {
        if (recent.isEmpty()) {
            return;
        }
        response.setLatestPerformanceRating(recent.get(0).performanceRating());
        response.setAvgPerformanceRating30d(round1(recent.stream()
                .map(RecentWorkoutRow::performanceRating)
                .filter(rating -> rating != null)
                .mapToInt(Integer::intValue)
                .average()
                .orElse(0.0)));
    }

    private double round1(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}
