package com.rtms.backend.service;

import com.rtms.backend.config.FarmSchedulePolicy;
import com.rtms.backend.entity.Subject;
import com.rtms.backend.entity.TrainingLot;
import com.rtms.backend.entity.TrainingWorkout;
import com.rtms.backend.enums.LotStatus;
import com.rtms.backend.enums.WorkoutStatus;
import com.rtms.backend.repository.SubjectRepository;
import com.rtms.backend.repository.TrainingLotRepository;
import com.rtms.backend.repository.TrainingWorkoutRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;

/**
 * Quản lý vòng đời LOT và thuật toán xếp khe giờ.
 *
 * NGUYÊN LÝ: ưu tiên GHÉP CHUNG trước, MỞ MỚI sau.
 *   - Ghép một con ngựa vào lot có sẵn tốn 0 phút khung giờ vàng.
 *   - Mở lot mới tốn T phút.
 * Nhờ đó khung giờ vàng chỉ bị tiêu hao theo SỐ BÀI TẬP KHÁC NHAU trong ngày,
 * chứ không theo số ngựa — đó là lý do cơ chế lot cho phép một Trainer quản lý
 * hàng chục chiến mã thay vì 4 con như mô hình 1 workout = 1 khung giờ.
 */
@Service
public class TrainingLotService {

    private final TrainingLotRepository lotRepository;
    private final TrainingWorkoutRepository workoutRepository;
    private final SubjectRepository subjectRepository;

    public TrainingLotService(TrainingLotRepository lotRepository,
                              TrainingWorkoutRepository workoutRepository,
                              SubjectRepository subjectRepository) {
        this.lotRepository = lotRepository;
        this.workoutRepository = workoutRepository;
        this.subjectRepository = subjectRepository;
    }

    // =================================================================
    // TÌM-HOẶC-TẠO LOT
    // =================================================================

    /**
     * BƯỚC 1 — Duyệt các lot (trainer, ngày, BÀI TẬP) theo giờ tăng dần.
     *           Một lot PHÙ HỢP khi thoả CẢ HAI:
     *             (a) số ngựa hiện có < sức chứa        [BR-10]
     *             (b) chưa có ngựa nào của Groom này      [BR-09]
     *           -> lấy lot phù hợp ĐẦU TIÊN.
     *
     * BƯỚC 2 — Không có lot nào phù hợp: mở lot mới ở khe trống đầu tiên.
     *           (Lot mới luôn thoả (a) và (b) vì đang rỗng.)
     *
     * BƯỚC 3 — Hết khe: ném lỗi.
     *
     * LƯU Ý QUAN TRỌNG về điều kiện (b): phải gộp vào bước tìm lot, KHÔNG
     * tách thành bước kiểm tra riêng sau khi đã chọn lot. Nếu tách, tình huống
     * "Groom An có 2 con cùng khoá cùng ngày bắt đầu" sẽ bị từ chối oan, trong
     * khi thực tế hoàn toàn xếp được: con thứ hai chỉ cần vào một lot CÙNG BÀI
     * nhưng KHÁC GIỜ.
     */
    @Transactional
    public TrainingLot findOrCreateLot(Long trainerId, LocalDate date,
                                       Subject subject, Long groomId) {

        // ---------- BƯỚC 1 ----------
        List<TrainingLot> sameSubjectLots =
                lotRepository.findActiveLotsOfDayBySubject(trainerId, date, subject.getId());

        for (TrainingLot lot : sameSubjectLots) {
            long occupied = workoutRepository
                    .countByLotIdAndStatusNot(lot.getId(), WorkoutStatus.CANCELLED);

            if (occupied >= lot.getMaxCapacity()) {
                continue;                                   // BR-10: lot đã đầy
            }
            if (groomId != null && workoutRepository
                    .existsByLotIdAndAssignedToIdAndStatusNot(
                            lot.getId(), groomId, WorkoutStatus.CANCELLED)) {
                continue;                                   // BR-09: Groom bận trong lot này
            }
            return lot;                                     // ghép chung — tốn 0 phút
        }

        // ---------- BƯỚC 2 ----------
        int duration = subject.getDurationMinutes();
        List<TrainingLot> dayLots = lotRepository.findActiveLotsOfDay(trainerId, date);
        LocalTime slot = findFirstFreeSlot(dayLots, duration);

        // ---------- BƯỚC 3 ----------
        if (slot == null) {
            long usedMinutes = dayLots.stream()
                    .mapToLong(l -> Duration.between(l.getStartTime(), l.getEndTime()).toMinutes())
                    .sum();
            throw new IllegalStateException(String.format(
                    "Ngày %s lịch của bạn đã kín. Bài '%s' cần %d phút nhưng khung giờ vàng "
                  + "%s–%s (%d phút) đã dùng %d phút cho %d lot, không còn khe trống nào đủ rộng. "
                  + "Hãy đổi ngày bắt đầu hoặc chọn khoá có bài tập ngắn hơn.",
                    date, subject.getName(), duration,
                    FarmSchedulePolicy.GOLDEN_HOURS_START, FarmSchedulePolicy.GOLDEN_HOURS_END,
                    FarmSchedulePolicy.goldenWindowMinutes(), usedMinutes, dayLots.size()));
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

    // =================================================================
    // THUẬT TOÁN QUÉT KHE (FIRST-FIT)
    // =================================================================

    /**
     * Tìm giờ bắt đầu cho một lot dài {@code durationMinutes} phút, sao cho
     * không đè lot nào và nằm trọn trong khung giờ vàng.
     *
     * @return giờ bắt đầu, hoặc null nếu không còn khe nào đủ rộng.
     *
     * VÌ SAO PHẢI SẮP XẾP TRƯỚC: con trỏ chỉ tiến về phía trước, không lùi.
     * Danh sách lộn xộn sẽ khiến nó nhảy qua 08:30 rồi mới gặp lot ở 07:00 —
     * lúc đó không quay lại được và sẽ tính sai khe.
     *
     * VÌ SAO QUÉT KHE Ở GIỮA mà không đơn giản "nối vào sau lot cuối":
     * lỗ trống ở giữa SẼ xuất hiện khi một lot bị huỷ (ngựa chấn thương,
     * trời mưa). Nếu chỉ biết nối đuôi, hệ thống bỏ phí lỗ đó.
     *
     * HẠN CHẾ ĐÃ BIẾT (nên ghi vào báo cáo): đây là first-fit, KHÔNG tối ưu.
     * Cùng một tập ngựa, tạo plan theo thứ tự khác nhau có thể cho số lot khác
     * nhau. Ví dụ khung 240 phút: xếp bài 45' trước rồi 90' -> còn 105'; xếp
     * 90' trước rồi 45' -> cũng còn 105'; nhưng với bộ khác thì kết quả lệch.
     * Vẫn chấp nhận vì: bài toán tối ưu là dạng bin-packing (độ phức tạp cao,
     * lời giải không đáng cho phạm vi đồ án); first-fit thì TẤT ĐỊNH (chạy lại
     * cho kết quả y hệt, dễ viết unit test), O(n log n), và dễ giải thích.
     * Trong vận hành thật Trainer cũng xếp tuần tự chứ không giải tối ưu toàn cục.
     */
    LocalTime findFirstFreeSlot(List<TrainingLot> lotsOfDay, int durationMinutes) {
        LocalTime cursor = FarmSchedulePolicy.GOLDEN_HOURS_START;

        List<TrainingLot> sorted = lotsOfDay.stream()
                .sorted(Comparator.comparing(TrainingLot::getStartTime))
                .toList();

        for (TrainingLot lot : sorted) {
            long gap = Duration.between(cursor, lot.getStartTime()).toMinutes();
            if (gap >= durationMinutes) {
                return cursor;                       // khe ở giữa đủ rộng
            }
            if (lot.getEndTime().isAfter(cursor)) {
                cursor = lot.getEndTime();           // nhảy qua lot này; con trỏ KHÔNG lùi
            }
        }

        long tail = Duration.between(cursor, FarmSchedulePolicy.GOLDEN_HOURS_END).toMinutes();
        return tail >= durationMinutes ? cursor : null;
    }

    // =================================================================
    // DỜI / HUỶ LOT
    // =================================================================

    /**
     * Dời giờ một lot. Cả hàng ngựa trong lot dời theo — đúng thực tế: trời
     * mưa thì cả lot cùng hoãn, không ai hoãn lẻ một con.
     */
    @Transactional
    public TrainingLot rescheduleLot(Long lotId, LocalTime newStart, Long trainerId) {
        TrainingLot lot = lotRepository.findById(lotId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy lot #" + lotId));

        if (!lot.getTrainerId().equals(trainerId)) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Bạn không phụ trách lot này!");
        }
        if (lot.getStatus() == LotStatus.CANCELLED || lot.getStatus() == LotStatus.COMPLETED) {
            throw new IllegalStateException(
                    "Lot đã " + lot.getStatus() + ", không thể dời giờ!");
        }

        Subject subject = subjectRepository.findById(lot.getSubjectId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bài tập của lot"));

        LocalTime newEnd = newStart.plusMinutes(subject.getDurationMinutes());

        // BR-03
        if (!FarmSchedulePolicy.isWithinGoldenHours(newStart, newEnd)) {
            throw new IllegalStateException(String.format(
                    "Khung giờ mới %s–%s nằm ngoài khung giờ vàng %s–%s!",
                    newStart, newEnd,
                    FarmSchedulePolicy.GOLDEN_HOURS_START, FarmSchedulePolicy.GOLDEN_HOURS_END));
        }

        // BR-02 ở cấp lot: không đè lot khác của cùng Trainer trong ngày
        for (TrainingLot other : lotRepository.findActiveLotsOfDay(lot.getTrainerId(), lot.getLotDate())) {
            if (other.getId().equals(lot.getId())) {
                continue;
            }
            boolean overlap = newStart.isBefore(other.getEndTime())
                           && newEnd.isAfter(other.getStartTime());
            if (overlap) {
                throw new IllegalStateException(String.format(
                        "Khung giờ mới %s–%s đè lên lot #%d (%s–%s) của bạn!",
                        newStart, newEnd, other.getId(),
                        other.getStartTime(), other.getEndTime()));
            }
        }

        lot.setStartTime(newStart);
        lot.setEndTime(newEnd);
        return lotRepository.save(lot);
    }

    /** Huỷ lot và toàn bộ buổi tập SCHEDULED bên trong. */
    @Transactional
    public TrainingLot cancelLot(Long lotId, Long trainerId) {
        TrainingLot lot = lotRepository.findById(lotId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy lot #" + lotId));

        if (!lot.getTrainerId().equals(trainerId)) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Bạn không phụ trách lot này!");
        }

        for (TrainingWorkout w : workoutRepository.findByLotId(lotId)) {
            if (w.getStatus() == WorkoutStatus.SCHEDULED) {
                w.setStatus(WorkoutStatus.CANCELLED);
                workoutRepository.save(w);
            }
        }

        lot.setStatus(LotStatus.CANCELLED);
        return lotRepository.save(lot);
    }

    /**
     * Huỷ lot NẾU không còn buổi tập nào hoạt động.
     *
     * Gọi sau khi huỷ workout lẻ (ví dụ cascade chấn thương). Nếu không làm,
     * khung giờ vàng sẽ bị chiếm bởi "lot ma" — lot rỗng nhưng vẫn giữ khe giờ,
     * khiến ngựa khác không xếp được lịch.
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

    // =================================================================
    // ĐỌC
    // =================================================================

    public List<TrainingLot> getLots(Long trainerId, LocalDate from, LocalDate to) {
        return lotRepository
                .findByTrainerIdAndLotDateBetweenOrderByLotDateAscStartTimeAsc(trainerId, from, to);
    }

    public TrainingLot findLotById(Long lotId) {
        return lotRepository.findById(lotId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy lot #" + lotId));
    }

    public int countOccupied(Long lotId) {
        return (int) workoutRepository.countByLotIdAndStatusNot(lotId, WorkoutStatus.CANCELLED);
    }
}
