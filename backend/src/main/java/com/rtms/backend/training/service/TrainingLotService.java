package com.rtms.backend.training.service;

import com.rtms.backend.config.ApiException;
import com.rtms.backend.config.FarmSchedulePolicy;
import com.rtms.backend.security.AuthenticatedUser;
import com.rtms.backend.training.dto.LotHorseNameRow;
import com.rtms.backend.training.dto.LotHorseResponse;
import com.rtms.backend.training.dto.TrainingLotResponse;
import com.rtms.backend.training.dto.TrainingLotRow;
import com.rtms.backend.training.entity.Subject;
import com.rtms.backend.training.entity.TrainingLot;
import com.rtms.backend.training.entity.TrainingWorkout;
import com.rtms.backend.training.enums.LotStatus;
import com.rtms.backend.training.enums.WorkoutStatus;
import com.rtms.backend.training.repository.SubjectRepository;
import com.rtms.backend.training.repository.TrainingLotRepository;
import com.rtms.backend.training.repository.TrainingWorkoutRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Quản lý vòng đời LOT và thuật toán xếp khe giờ.
 * (Manages the LOT lifecycle and the time-slot algorithm.)
 *
 * NGUYÊN LÝ: ưu tiên GHÉP CHUNG trước, MỞ MỚI sau.
 * (PRINCIPLE: join an existing lot first, open a new one second.)
 *   - Ghép một con ngựa vào lot có sẵn tốn 0 phút khung giờ vàng.
 *     (Adding a horse to an existing lot costs 0 minutes of the golden window.)
 *   - Mở lot mới tốn T phút. (Opening a new lot costs T minutes.)
 * Nhờ đó khung giờ vàng chỉ bị tiêu hao theo SỐ BÀI TẬP KHÁC NHAU trong ngày, chứ không theo số ngựa: đó là
 * lý do cơ chế lot cho phép một Trainer quản lý hàng chục chiến mã thay vì 4 con như mô hình 1 workout = 1 khung giờ.
 * (So the golden window is consumed by the number of DISTINCT SUBJECTS in a day, not by the number of horses:
 * that is why lots let one trainer manage dozens of horses instead of 4 as in a 1 workout = 1 slot model.)
 */
@Service
public class TrainingLotService {

    private static final DateTimeFormatter DAY_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /** Tên miền của khóa lịch, để khóa của Trainer không đụng khóa advisory của chức năng khác. (Namespace of the schedule lock.) */
    private static final long SCHEDULE_LOCK_NAMESPACE = 0x5254_4D53_0000_0000L;

    private final TrainingLotRepository lotRepository;
    private final TrainingWorkoutRepository workoutRepository;
    private final SubjectRepository subjectRepository;
    private final PlanCompletionService planCompletion;

    public TrainingLotService(TrainingLotRepository lotRepository,
                              TrainingWorkoutRepository workoutRepository,
                              SubjectRepository subjectRepository,
                              PlanCompletionService planCompletion) {
        this.lotRepository = lotRepository;
        this.workoutRepository = workoutRepository;
        this.subjectRepository = subjectRepository;
        this.planCompletion = planCompletion;
    }

    // =================================================================
    // TÌM-HOẶC-TẠO LOT (Find-or-create a lot)
    // =================================================================

    /**
     * BƯỚC 1: duyệt các lot (trainer, ngày, BÀI TẬP) theo giờ tăng dần. Một lot PHÙ HỢP khi thỏa CẢ HAI:
     *   (a) số ngựa hiện có < sức chứa [BR-10]
     *   (b) chưa có ngựa nào của Groom này [BR-09]
     * và lấy lot phù hợp ĐẦU TIÊN.
     * (STEP 1: scan the lots of (trainer, day, SUBJECT) in ascending time. A lot FITS when BOTH hold:
     *   (a) current horse count < capacity [BR-10]
     *   (b) none of this groom horses is in it yet [BR-09]
     * and the FIRST fitting lot is taken.)
     *
     * BƯỚC 2: không có lot phù hợp thì mở lot mới ở khe trống đầu tiên (lot mới luôn thỏa (a) và (b)).
     * (STEP 2: with no fitting lot, open a new lot in the first free slot (a new lot always satisfies (a) and (b)).)
     *
     * BƯỚC 3: hết khe thì ném lỗi. (STEP 3: if no slot is left, throw.)
     *
     * LƯU Ý về điều kiện (b): phải gộp vào bước tìm lot, KHÔNG tách thành bước kiểm tra riêng sau khi đã chọn lot.
     * Nếu tách, "Groom An có 2 con cùng khóa cùng ngày bắt đầu" sẽ bị từ chối oan, trong khi con thứ hai chỉ cần
     * vào một lot CÙNG BÀI nhưng KHÁC GIỜ.
     * (NOTE on condition (b): it must be part of the lot search, NOT a separate check after a lot is chosen.
     * Otherwise "groom An has 2 horses in the same course starting the same day" would be rejected wrongly,
     * although the second horse only needs a lot with the SAME SUBJECT at a DIFFERENT TIME.)
     */
    @Transactional
    public TrainingLot findOrCreateLot(Long trainerId, LocalDate date,
                                       Subject subject, Long groomId) {

        LocalTime notBefore = earliestStartOn(date);

        // ---------- BƯỚC 1 (STEP 1) ----------
        // Chỉ ghép vào lot còn SCHEDULED và chưa bắt đầu: lot đã COMPLETED hay đã chạy rồi không nhận thêm ngựa.
        // (Only join a lot that is still SCHEDULED and has not started: a COMPLETED or running lot takes no more horses.)
        List<TrainingLot> sameSubjectLots =
                lotRepository.findActiveLotsOfDayBySubject(trainerId, date, subject.getId()).stream()
                        .filter(lot -> lot.getStatus() == LotStatus.SCHEDULED)
                        .filter(lot -> !lot.getStartTime().isBefore(notBefore))
                        .toList();

        for (TrainingLot lot : sameSubjectLots) {
            long occupied = workoutRepository
                    .countByLotIdAndStatusNot(lot.getId(), WorkoutStatus.CANCELLED);

            if (occupied >= lot.getMaxCapacity()) {
                continue;                                   // BR-10: lot đã đầy (the lot is full)
            }
            if (groomId != null && workoutRepository
                    .existsByLotIdAndAssignedToIdAndStatusNot(
                            lot.getId(), groomId, WorkoutStatus.CANCELLED)) {
                continue;                                   // BR-09: Groom bận trong lot này (groom busy in this lot)
            }
            return lot;                                     // ghép chung, tốn 0 phút (shared, costs 0 minutes)
        }

        // ---------- BƯỚC 2 (STEP 2) ----------
        int duration = subject.getDurationMinutes();
        List<TrainingLot> dayLots = lotRepository.findActiveLotsOfDay(trainerId, date);
        LocalTime slot = findFirstFreeSlot(dayLots, duration, notBefore);

        // ---------- BƯỚC 3 (STEP 3) ----------
        if (slot == null) {
            throw scheduleFull(date);
        }

        TrainingLot lot = new TrainingLot();
        lot.setTrainerId(trainerId);
        lot.setSubjectId(subject.getId());
        lot.setLotDate(date);
        lot.setStartTime(slot);
        lot.setEndTime(slot.plusMinutes(duration));
        lot.setMaxCapacity(FarmSchedulePolicy.DEFAULT_LOT_CAPACITY);
        lot.setStatus(LotStatus.SCHEDULED);

        return lotRepository.saveAndFlush(lot);
    }

    private ApiException scheduleFull(LocalDate date) {
        return new ApiException(HttpStatus.CONFLICT, "SCHEDULE_FULL",
                "Your schedule on " + date.format(DAY_FORMAT) + " is full.");
    }

    /**
     * Giờ sớm nhất một lot của ngày này được bắt đầu: đầu khung giờ vàng, còn hôm nay thì không sớm hơn lúc này (làm tròn LÊN
     * mốc 15 phút), vì không xếp buổi tập vào một giờ đã trôi qua.
     * (The earliest a lot of this day may start: the start of the golden window, and for today no earlier than now (rounded UP
     * to the 15-minute step), because no workout is placed at a time that has already passed.)
     */
    LocalTime earliestStartOn(LocalDate date) {
        if (!date.equals(LocalDate.now())) {
            return FarmSchedulePolicy.GOLDEN_HOURS_START;
        }
        LocalTime now = LocalTime.now();
        if (!now.isBefore(FarmSchedulePolicy.GOLDEN_HOURS_END)) {
            return FarmSchedulePolicy.GOLDEN_HOURS_END;
        }
        LocalTime rounded = FarmSchedulePolicy.roundUpToLotStartStep(now);
        return rounded.isBefore(FarmSchedulePolicy.GOLDEN_HOURS_START) ? FarmSchedulePolicy.GOLDEN_HOURS_START : rounded;
    }

    /** Khóa lịch của Trainer đến hết transaction. (Locks the trainer schedule until the end of the transaction.) */
    @Transactional
    public void lockSchedule(Long trainerId) {
        lotRepository.lockSchedule(SCHEDULE_LOCK_NAMESPACE + trainerId);
    }

    // =================================================================
    // THUẬT TOÁN QUÉT KHE (FIRST-FIT) (Slot scanning algorithm)
    // =================================================================

    /**
     * Tìm giờ bắt đầu cho một lot dài {@code durationMinutes} phút, sao cho không đè lot nào và nằm trọn
     * trong khung giờ vàng. Trả về giờ bắt đầu, hoặc null nếu không còn khe nào đủ rộng.
     * (Finds a start time for a lot of {@code durationMinutes} minutes that overlaps no lot and lies fully
     * inside the golden window. Returns the start time, or null if no slot is wide enough.)
     *
     * VÌ SAO PHẢI SẮP XẾP TRƯỚC: con trỏ chỉ tiến, không lùi; danh sách lộn xộn sẽ làm nó nhảy qua 08:30 rồi
     * mới gặp lot ở 07:00 và tính sai khe.
     * (WHY SORT FIRST: the pointer only moves forward; an unsorted list would make it jump past 08:30 and only
     * then meet a lot at 07:00, computing the wrong slot.)
     *
     * VÌ SAO QUÉT KHE Ở GIỮA mà không "nối vào sau lot cuối": lỗ trống ở giữa SẼ xuất hiện khi một lot bị hủy
     * (ngựa chấn thương, trời mưa); chỉ biết nối đuôi thì hệ thống bỏ phí lỗ đó.
     * (WHY SCAN GAPS instead of "append after the last lot": gaps WILL appear when a lot is cancelled (injury,
     * rain); only appending would waste them.)
     *
     * HẠN CHẾ ĐÃ BIẾT: đây là first-fit, KHÔNG tối ưu (bài toán tối ưu là bin-packing, không đáng cho phạm vi đồ
     * án). Bù lại first-fit TẤT ĐỊNH (chạy lại cho kết quả y hệt, dễ unit test), O(n log n) và dễ giải thích.
     * (KNOWN LIMITATION: this is first-fit, NOT optimal (the optimal problem is bin-packing, not worth it for
     * this project). In return first-fit is DETERMINISTIC (re-running gives the same result, easy to unit test),
     * O(n log n) and easy to explain.)
     */
    LocalTime findFirstFreeSlot(List<TrainingLot> lotsOfDay, int durationMinutes) {
        return findFirstFreeSlot(lotsOfDay, durationMinutes, FarmSchedulePolicy.GOLDEN_HOURS_START);
    }

    /** Như trên nhưng không trả giờ sớm hơn {@code notBefore}. (As above, but never returns a time before {@code notBefore}.) */
    LocalTime findFirstFreeSlot(List<TrainingLot> lotsOfDay, int durationMinutes, LocalTime notBefore) {
        LocalTime cursor = notBefore.isBefore(FarmSchedulePolicy.GOLDEN_HOURS_START)
                ? FarmSchedulePolicy.GOLDEN_HOURS_START : notBefore;

        List<TrainingLot> sorted = lotsOfDay.stream()
                .sorted(Comparator.comparing(TrainingLot::getStartTime))
                .toList();

        for (TrainingLot lot : sorted) {
            long gap = Duration.between(cursor, lot.getStartTime()).toMinutes();
            if (gap >= durationMinutes) {
                return cursor;                       // khe ở giữa đủ rộng (the gap in between is wide enough)
            }
            if (lot.getEndTime().isAfter(cursor)) {
                // Nhảy qua lot này, con trỏ KHÔNG lùi; một bài 40 phút kết thúc 06:40 thì lot sau bắt đầu 06:45.
                // (Skip this lot, the pointer never moves back; a 40-minute subject ending at 06:40 puts the
                // next lot at 06:45.)
                cursor = FarmSchedulePolicy.roundUpToLotStartStep(lot.getEndTime());
            }
        }

        long tail = Duration.between(cursor, FarmSchedulePolicy.GOLDEN_HOURS_END).toMinutes();
        return tail >= durationMinutes ? cursor : null;
    }

    // =================================================================
    // XEM LỊCH LOT (Viewing the lot schedule)
    // =================================================================

    /**
     * Xem lịch lot theo khoảng ngày. Mỗi vai trò có một nghĩa "lịch của tôi" khác nhau:
     * (Views the lot schedule over a date range. Each role has a different meaning of "my schedule":)
     *   HEAD_TRAINER : các lot MÌNH ĐỨNG DẠY (the lots I TEACH)
     *   GROOM        : các lot MÌNH CÓ NGỰA phải dắt (the lots where I have a horse to lead)
     *   HORSE_OWNER  : các lot có NGỰA CỦA MÌNH; chỉ thấy tên ngựa của mình (the lots with MY horses; only
     *                  my own horse names are shown)
     *   CLUB_MANAGER : phải chỉ rõ trainerId (must specify a trainerId)
     *
     * @param includeCancelled chỉ áp dụng cho Trainer và Manager; Groom và Owner không bao giờ thấy lot đã hủy,
     *                         vì với họ lot đã hủy không còn là việc phải làm.
     *                         (only applies to trainers and managers; grooms and owners never see cancelled
     *                         lots, since for them a cancelled lot is no longer something to act on.)
     */
    @Transactional(readOnly = true)
    public List<TrainingLotResponse> getLots(AuthenticatedUser currentUser, Long trainerId,
                                             LocalDate from, LocalDate to, boolean includeCancelled) {
        if (from.isAfter(to)) {
            throw new IllegalArgumentException(
                    "Invalid date range: 'from' (" + from + ") is after 'to' (" + to + ")!");
        }

        Long userId = currentUser.getUserId();
        List<TrainingLotRow> rows = switch (currentUser.getRole()) {
            // LUÔN dùng id của chính mình, kể cả khi client truyền trainerId của người khác.
            // (ALWAYS use the caller own id, even when the client passes someone else trainerId.)
            case "HEAD_TRAINER" -> lotRepository.findTrainerLotRows(userId, from, to, includeCancelled);
            case "GROOM" -> lotRepository.findGroomLotRows(userId, from, to);
            case "HORSE_OWNER" -> lotRepository.findOwnerLotRows(userId, from, to);
            default -> {
                if (trainerId == null) {
                    throw new IllegalArgumentException(
                            "Missing 'trainerId' parameter: specify whose lot schedule you want to see!");
                }
                yield lotRepository.findTrainerLotRows(trainerId, from, to, includeCancelled);
            }
        };
        return toResponses(rows, ownerFilterOf(currentUser));
    }

    /**
     * Xem một lot. Không đủ quyền thì báo "không tìm thấy" để không lộ lot có tồn tại.
     * (Views one lot. Without access it is reported as "not found" so the lot existence is not revealed.)
     */
    @Transactional(readOnly = true)
    public TrainingLotResponse getLot(Long lotId, AuthenticatedUser currentUser) {
        TrainingLotRow row = lotRepository.findLotRowById(lotId).orElseThrow(() -> lotNotFound(lotId));
        if (!canViewLot(row, currentUser)) {
            throw lotNotFound(lotId);
        }
        return toResponses(List.of(row), ownerFilterOf(currentUser)).get(0);
    }

    private boolean canViewLot(TrainingLotRow row, AuthenticatedUser currentUser) {
        Long userId = currentUser.getUserId();
        return switch (currentUser.getRole()) {
            case "HEAD_TRAINER" -> row.trainerId().equals(userId);
            case "GROOM" -> workoutRepository.existsByLotIdAndAssignedToIdAndStatusNot(
                    row.lotId(), userId, WorkoutStatus.CANCELLED);
            case "HORSE_OWNER" -> workoutRepository.existsActiveWorkoutOfOwnerInLot(row.lotId(), userId);
            default -> true;
        };
    }

    /** Owner chỉ được thấy tên ngựa của mình; vai khác thì null (thấy tất cả). (Owners only see their own horse names; other roles get null = all.) */
    private Long ownerFilterOf(AuthenticatedUser currentUser) {
        return "HORSE_OWNER".equals(currentUser.getRole()) ? currentUser.getUserId() : null;
    }

    /**
     * Dựng response cho nhiều lot bằng đúng MỘT truy vấn lấy tên ngựa, thay vì 3 truy vấn mỗi lot.
     * (Builds responses for many lots with exactly ONE horse-name query, instead of 3 queries per lot.)
     *
     * @param ownerId nếu khác null thì chỉ liệt kê ngựa của owner đó (if not null, only that owner horses are listed)
     */
    private List<TrainingLotResponse> toResponses(List<TrainingLotRow> rows, Long ownerId) {
        if (rows.isEmpty()) {
            return List.of();
        }

        List<Long> lotIds = rows.stream().map(TrainingLotRow::lotId).toList();
        List<LotHorseNameRow> names = ownerId == null
                ? lotRepository.findActiveHorseNames(lotIds)
                : lotRepository.findActiveHorseNamesOfOwner(lotIds, ownerId);
        Map<Long, List<LotHorseResponse>> horsesByLot = names.stream().collect(Collectors.groupingBy(
                LotHorseNameRow::lotId,
                Collectors.mapping(row -> new LotHorseResponse(row.horseId(), row.horseName()), Collectors.toList())));

        return rows.stream()
                .map(row -> new TrainingLotResponse(row, horsesByLot.getOrDefault(row.lotId(), List.of())))
                .toList();
    }

    private RuntimeException lotNotFound(Long lotId) {
        return new RuntimeException("Lot #" + lotId + " not found");
    }

    // =================================================================
    // DỜI / HỦY LOT (Rescheduling / cancelling a lot)
    // =================================================================

    /**
     * Dời giờ một lot. Cả hàng ngựa trong lot dời theo, đúng thực tế: trời mưa thì cả lot cùng hoãn. Chỉ dời được lot còn
     * SCHEDULED và chưa diễn ra, tới một giờ chưa qua.
     * (Reschedules a lot. All horses in the lot move with it, as in reality: when it rains the whole lot is postponed. Only a
     * lot that is still SCHEDULED and has not taken place can move, to a time that has not passed.)
     */
    @Transactional
    public TrainingLotResponse rescheduleLot(Long lotId, LocalTime newStart, Long trainerId) {
        if (newStart == null) {
            throw new IllegalArgumentException("The new start time is required!");
        }
        lockSchedule(trainerId);                       // khóa TRƯỚC khi đọc lot (lock BEFORE reading the lot)
        TrainingLot lot = findOwnLot(lotId, trainerId);
        assertCanChange(lot, "rescheduled");

        if (!FarmSchedulePolicy.isOnLotStartStep(newStart)) {
            throw new IllegalArgumentException("A lot can only start at :00, :15, :30 or :45!");
        }

        Subject subject = subjectRepository.findById(lot.getSubjectId())
                .orElseThrow(() -> new RuntimeException("Subject of the lot not found"));

        LocalTime newEnd = newStart.plusMinutes(subject.getDurationMinutes());

        // BR-03
        if (!FarmSchedulePolicy.isWithinGoldenHours(newStart, newEnd)) {
            throw new IllegalArgumentException(String.format(
                    "The new time %s–%s is outside the golden window %s–%s!",
                    newStart, newEnd,
                    FarmSchedulePolicy.GOLDEN_HOURS_START, FarmSchedulePolicy.GOLDEN_HOURS_END));
        }
        if (newStart.isBefore(earliestStartOn(lot.getLotDate()))) {
            throw new IllegalArgumentException("The new time " + newStart + " has already passed!");
        }

        // BR-02 ở cấp lot: không đè lot khác của cùng Trainer trong ngày
        // (BR-02 at lot level: must not overlap another lot of the same trainer on that day)
        for (TrainingLot other : lotRepository.findActiveLotsOfDay(lot.getTrainerId(), lot.getLotDate())) {
            if (other.getId().equals(lot.getId())) {
                continue;
            }
            boolean overlap = newStart.isBefore(other.getEndTime())
                           && newEnd.isAfter(other.getStartTime());
            if (overlap) {
                throw new ApiException(HttpStatus.CONFLICT, "LOT_OVERLAP", String.format(
                        "The new time %s–%s overlaps another lot (%s–%s)!",
                        newStart, newEnd, other.getStartTime(), other.getEndTime()));
            }
        }

        lot.setStartTime(newStart);
        lot.setEndTime(newEnd);
        lotRepository.save(lot);
        return responseOf(lotId);
    }

    /**
     * Hủy lot và mọi buổi tập SCHEDULED bên trong; kế hoạch nào không còn buổi để tập thì được đóng.
     * (Cancels a lot and all SCHEDULED workouts inside it; a plan left with no session to hold is closed.)
     */
    @Transactional
    public TrainingLotResponse cancelLot(Long lotId, Long trainerId) {
        lockSchedule(trainerId);                       // khóa TRƯỚC khi đọc lot (lock BEFORE reading the lot)
        TrainingLot lot = findOwnLot(lotId, trainerId);
        assertCanChange(lot, "cancelled");

        List<TrainingWorkout> scheduled = workoutRepository.findByLotId(lotId).stream()
                .filter(w -> w.getStatus() == WorkoutStatus.SCHEDULED)
                .toList();
        scheduled.forEach(w -> w.setStatus(WorkoutStatus.CANCELLED));
        workoutRepository.saveAll(scheduled);
        workoutRepository.flush();   // để PlanCompletionService đếm đúng (so PlanCompletionService counts correctly)

        lot.setStatus(LotStatus.CANCELLED);
        lotRepository.save(lot);

        planCompletion.closeFinishedPlans(scheduled.stream()
                .map(TrainingWorkout::getPlanId)
                .collect(Collectors.toSet()));
        return responseOf(lotId);
    }

    private TrainingLot findOwnLot(Long lotId, Long trainerId) {
        TrainingLot lot = lotRepository.findById(lotId).orElseThrow(() -> lotNotFound(lotId));
        if (!lot.getTrainerId().equals(trainerId)) {
            throw new AccessDeniedException("You are not in charge of this lot!");
        }
        return lot;
    }

    /**
     * Chỉ lot còn SCHEDULED và chưa diễn ra mới đổi được: lot đã COMPLETED hay đã hủy là lịch sử, lot đã qua giờ kết thúc thì
     * buổi tập đã (hoặc đáng lẽ đã) xảy ra và kết quả phải được ghi chứ không phải hủy.
     * (Only a lot that is still SCHEDULED and has not taken place can change: a COMPLETED or cancelled lot is history, and a
     * lot past its end time has happened (or should have), so its results must be recorded rather than the lot cancelled.)
     */
    private void assertCanChange(TrainingLot lot, String action) {
        if (lot.getStatus() != LotStatus.SCHEDULED) {
            throw new ApiException(HttpStatus.CONFLICT, "LOT_NOT_SCHEDULED",
                    "The lot is already " + lot.getStatus() + " and cannot be " + action + "!");
        }
        if (LocalDateTime.of(lot.getLotDate(), lot.getEndTime()).isBefore(LocalDateTime.now())) {
            throw new ApiException(HttpStatus.CONFLICT, "LOT_ALREADY_HELD",
                    "This lot has already taken place and cannot be " + action + ". Record its results instead.");
        }
    }

    /** Response của một lot vừa được sửa, đọc lại sau khi lưu. (Response of a lot just modified, re-read after saving.) */
    private TrainingLotResponse responseOf(Long lotId) {
        TrainingLotRow row = lotRepository.findLotRowById(lotId).orElseThrow(() -> lotNotFound(lotId));
        return toResponses(List.of(row), null).get(0);
    }

    /**
     * Hủy lot NẾU không còn buổi tập nào hoạt động. Gọi sau khi hủy workout lẻ (ví dụ cascade chấn thương); nếu
     * không, khung giờ vàng bị chiếm bởi "lot ma": lot rỗng nhưng vẫn giữ khe giờ.
     * (Cancels the lot IF no active workout is left. Called after cancelling single workouts (for example the
     * injury cascade); otherwise the golden window stays occupied by a "ghost lot": empty but still holding a slot.)
     */
    @Transactional
    public void cancelLotIfEmpty(Long lotId) {
        long remaining = workoutRepository
                .countByLotIdAndStatusNot(lotId, WorkoutStatus.CANCELLED);
        if (remaining > 0) {
            return;
        }
        lotRepository.findById(lotId).ifPresent(lot -> {
            if (lot.getStatus() != LotStatus.CANCELLED) {
                lot.setStatus(LotStatus.CANCELLED);
                lotRepository.save(lot);
            }
        });
    }
}
