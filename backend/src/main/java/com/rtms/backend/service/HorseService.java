package com.rtms.backend.service;
import com.rtms.backend.dto.CreateHorseRequest;
import com.rtms.backend.dto.HorseAlertResponse;
import com.rtms.backend.dto.HorseFitnessTrendItemResponse;
import com.rtms.backend.dto.UpdateHorseStatusRequest;
import com.rtms.backend.entity.Horse;
import com.rtms.backend.enums.HorseStatus;
import com.rtms.backend.repository.HorseRepository;
import com.rtms.backend.config.GlobalExceptionHandler;
import com.rtms.backend.security.AuthenticatedUser;
import com.rtms.backend.entity.Area;
import com.rtms.backend.entity.StableStall;
import com.rtms.backend.enums.StallStatus;
import com.rtms.backend.repository.AreaRepository;
import com.rtms.backend.repository.StableStallRepository;
import com.rtms.backend.service.HorseTrainingPlanService;
import java.time.LocalDate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class HorseService {

    private final HorseRepository horseRepository;
    private final StableStallRepository stableStallRepository;
    private final HorseTrainingPlanService trainingPlanService;
    private final AreaRepository areaRepository;

    public HorseService(HorseRepository horseRepository,
                        StableStallRepository stableStallRepository,
                        @org.springframework.context.annotation.Lazy HorseTrainingPlanService trainingPlanService,
                        AreaRepository areaRepository) {
        this.horseRepository = horseRepository;
        this.stableStallRepository = stableStallRepository;
        this.trainingPlanService = trainingPlanService;
        this.areaRepository = areaRepository;
    }

    /** Giữ chữ ký 3 tham số cho các lời gọi đã có. */
    public List<Horse> getAllHorses(AuthenticatedUser currentUser,
                                    Boolean mine,
                                    HorseStatus status) {
        return getAllHorses(currentUser, mine, null, status);
    }

    /**
     * @param mine       true + vai trò HEAD_TRAINER -> chỉ ngựa trong khu mình
     *                   phụ trách. Suy qua Horse.currentStallId ->
     *                   StableStall.areaId -> Area.trainerId, không cần thêm
     *                   cột nào vào bảng horses.
     * @param unassigned true -> chỉ ngựa CHƯA được xếp chuồng. Dùng cho hộp
     *                   thoại "xếp ngựa vào chuồng" trên sơ đồ chuồng trại.
     * @param status     lọc theo trạng thái, ví dụ ELIGIBLE để bỏ ngựa
     *                   CANDIDATE khỏi danh sách ghi danh.
     *
     * VÌ SAO mine VÀ unassigned PHẢI LOẠI TRỪ NHAU:
     * "khu của tôi" được suy ra TỪ chuồng của con ngựa. Ngựa chưa có chuồng
     * thì không thuộc khu nào, nên nó không bao giờ thoả được mine=true.
     * Nếu lồng hai điều kiện lại, kết quả LUÔN rỗng — đúng lỗi mà màn hình
     * xếp chuồng đã gặp: hộp thoại hiện "Chiến mã sẵn sàng (0)" vĩnh viễn.
     * Vì vậy unassigned=true thì bỏ qua hẳn nhánh mine.
     *
     * Mọi tham số đều TUỲ CHỌN: không truyền thì hành vi y hệt trước, nên
     * không màn hình nào của actor khác bị ảnh hưởng.
     */
    public List<Horse> getAllHorses(AuthenticatedUser currentUser,
                                    Boolean mine,
                                    Boolean unassigned,
                                    HorseStatus status) {

        List<Horse> horses = "HORSE_OWNER".equals(currentUser.getRole())
                ? horseRepository.findByOwnerId(currentUser.getUserId())
                : horseRepository.findAll();

        if (Boolean.TRUE.equals(unassigned)) {
            horses = horses.stream()
                    .filter(h -> h.getCurrentStallId() == null)
                    .toList();

        } else if (Boolean.TRUE.equals(mine) && "HEAD_TRAINER".equals(currentUser.getRole())) {
            Long trainerId = currentUser.getUserId();

            // Nạp MỘT LẦN các khu của Trainer này, rồi MỘT LẦN các chuồng thuộc
            // các khu đó. Không findById trong vòng lặp -> tránh N+1.
            Set<Long> myAreaIds = areaRepository.findAll().stream()
                    .filter(a -> trainerId.equals(a.getTrainerId()))
                    .map(Area::getId)
                    .collect(Collectors.toSet());

            Set<Long> myStallIds = stableStallRepository.findAll().stream()
                    .filter(s -> myAreaIds.contains(s.getAreaId()))
                    .map(StableStall::getId)
                    .collect(Collectors.toSet());

            horses = horses.stream()
                    .filter(h -> h.getCurrentStallId() != null
                              && myStallIds.contains(h.getCurrentStallId()))
                    .toList();
        }

        if (status != null) {
            horses = horses.stream()
                    .filter(h -> h.getCurrentStatus() == status)
                    .toList();
        }

        return horses;
    }

    public List<Horse> getAllHorses(AuthenticatedUser currentUser) {
        return getAllHorses(currentUser, null, null);
    }

    public Horse createHorse(CreateHorseRequest request) {
        Horse horse = new Horse();
        horse.setName(request.getName());
        horse.setBreed(request.getBreed());
        horse.setDateOfBirth(request.getDateOfBirth());
        horse.setStableLocation(request.getStableLocation());
        horse.setOwnerId(request.getOwnerId());
        // currentStatus KHONG set o day - de mac dinh "ELIGIBLE" theo gia tri default
        // trong Entity/DB
        return horseRepository.save(horse);
    }

    public Horse getHorseById(Long id, AuthenticatedUser currentUser) {
        Horse horse = horseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Horse not found with id: " + id));
        if ("HORSE_OWNER".equals(currentUser.getRole())) {
            if (!currentUser.getUserId().equals(horse.getOwnerId())) {
                throw new AccessDeniedException("You can only view horses you own");
            }
        }
        return horse;
    }

    @Transactional
    public Horse updateHorseStatus(Long id, UpdateHorseStatusRequest request) {
        Horse horse = horseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Horse not found with id: " + id));

        // Cập nhật trạng thái mới
        HorseStatus newStatus = HorseStatus.valueOf(request.getStatus());
        horse.setCurrentStatus(newStatus);
        Horse saved = horseRepository.save(horse);

        // A non-eligible administrative status may lock training, but changing
        // the status back to ELIGIBLE never clears a Vet lock. Only a veterinary
        // APPROVED decision is allowed to do that.
        if (newStatus != HorseStatus.ELIGIBLE) {
            horse.setTrainingLocked(true);
            horse.setTrainingLockReason("Horse status changed to " + newStatus);
            horse.setTrainingLockUpdatedAt(java.time.LocalDateTime.now());
            trainingPlanService.cancelFutureTrainingForHorse(
                    saved.getId(),
                    "Chiến mã chuyển sang trạng thái " + newStatus);
        }

        return saved;
    }

    /**
     * Xếp chiến mã vào chuồng, hoặc GỠ khỏi chuồng khi stallId == null.
     *
     * Bỏ trống stallId nghĩa là gỡ — nhất quán với
     * PUT /api/stalls/{id}/assign-groom vốn đã bỏ trống groomId là gỡ.
     */
    @Transactional
    public Horse assignStall(Long horseId, Long stallId) {
        if (horseId == null) {
            throw new RuntimeException("Horse ID is required");
        }

        Horse horse = horseRepository.findById(horseId)
                .orElseThrow(() -> new RuntimeException("Horse not found with id: " + horseId));

        // ---- GỠ NGỰA KHỎI CHUỒNG ----
        if (stallId == null) {
            if (horse.getCurrentStallId() == null) {
                return horse;                      // vốn đã không ở chuồng nào
            }

            // Buổi tập tương lai thành "chưa phân công". Không có người phụ
            // trách thì không thể vướng BR-09, nên hàm này không ném lỗi.
            trainingPlanService.reassignFutureWorkoutsToGroom(horseId, null);

            stableStallRepository.findById(horse.getCurrentStallId()).ifPresent(oldStall -> {
                oldStall.setStatus(StallStatus.AVAILABLE);
                stableStallRepository.save(oldStall);
            });

            horse.setCurrentStallId(null);
            return horseRepository.save(horse);
        }

        StableStall newStall = stableStallRepository.findById(stallId)
                .orElseThrow(() -> new RuntimeException("Stable stall not found with id: " + stallId));

        // ---- BR-07: một chuồng chỉ chứa một chiến mã ----
        //
        // Trước đây chỉ có ràng buộc UNIQUE ở tầng DB chặn việc này. Nó ném
        // DataIntegrityViolationException, mà GlobalExceptionHandler map
        // RuntimeException thành 404 — người dùng nhận thông báo SQL thô kèm
        // mã lỗi sai hẳn. Kiểm ở đây để trả lỗi nghiệp vụ đọc được.
        horseRepository.findByCurrentStallId(stallId).ifPresent(occupant -> {
            if (!occupant.getId().equals(horseId)) {
                throw new IllegalStateException(String.format(
                        "Chuồng '%s' đang có chiến mã '%s'. Hãy chuyển con đó đi trước!",
                        newStall.getStallCode(), occupant.getName()));
            }
        });

        // ---- Đồng bộ Groom cho các buổi tập tương lai ----
        //
        // Chuồng mới có thể do Groom khác phụ trách. Không đồng bộ thì buổi tập
        // vẫn treo ở Groom của chuồng CŨ.
        // Gọi TRƯỚC khi ghi, để vướng BR-09 thì rollback toàn bộ.
        if (!stallId.equals(horse.getCurrentStallId())) {
            trainingPlanService.reassignFutureWorkoutsToGroom(horseId, newStall.getGroomId());
        }

        // Nếu ngựa trước đó đã ở chuồng khác thì giải phóng chuồng cũ về AVAILABLE
        if (horse.getCurrentStallId() != null && !horse.getCurrentStallId().equals(stallId)) {
            stableStallRepository.findById(horse.getCurrentStallId()).ifPresent(oldStall -> {
                oldStall.setStatus(StallStatus.AVAILABLE);
                stableStallRepository.save(oldStall);
            });
        }

        newStall.setStatus(StallStatus.OCCUPIED);
        stableStallRepository.save(newStall);

        horse.setCurrentStallId(stallId);
        return horseRepository.save(horse);
    }

    public List<HorseFitnessTrendItemResponse> getFitnessTrend(Long horseId, LocalDate from, LocalDate to) {
        return trainingPlanService.getFitnessTrend(horseId, from, to);
    }

    public List<HorseAlertResponse> getHorseAlerts(Long horseId) {
        return trainingPlanService.getHorseAlerts(horseId);
    }
}
