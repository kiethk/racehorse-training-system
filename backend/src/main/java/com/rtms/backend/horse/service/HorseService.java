package com.rtms.backend.horse.service;
import com.rtms.backend.config.GlobalExceptionHandler;
import com.rtms.backend.training.service.TrainingWorkoutService;


import com.rtms.backend.horse.dto.CreateHorseRequest;
import com.rtms.backend.horse.entity.Horse;
import com.rtms.backend.stable.entity.StableStall;
import com.rtms.backend.horse.enums.HorseStatus;
import com.rtms.backend.stable.enums.StallStatus;
import com.rtms.backend.horse.repository.HorseRepository;
import com.rtms.backend.stable.repository.StableStallRepository;
import com.rtms.backend.security.AuthenticatedUser;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.rtms.backend.stable.entity.Area;
import com.rtms.backend.stable.repository.AreaRepository;

import com.rtms.backend.config.ApiException;
import com.rtms.backend.horse.dto.HorseStatusChangeRequest;
import com.rtms.backend.medical.entity.CareSchedule;
import com.rtms.backend.medical.enums.CareScheduleStatus;
import com.rtms.backend.medical.repository.CareScheduleRepository;
import com.rtms.backend.notification.service.NotificationService;
import com.rtms.backend.stable.entity.GroomDailyTask;
import com.rtms.backend.stable.repository.GroomDailyTaskRepository;
import com.rtms.backend.training.entity.HorseTrainingPlan;
import com.rtms.backend.training.entity.TrainingWorkout;
import com.rtms.backend.training.enums.TrainingDecision;
import com.rtms.backend.training.enums.TrainingPlanStatus;
import com.rtms.backend.training.enums.WorkoutStatus;
import com.rtms.backend.training.repository.HorseTrainingPlanRepository;
import com.rtms.backend.training.repository.TrainingWorkoutRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class HorseService {

    private static final String GROOM_ROLE = "GROOM";

    private final HorseRepository horseRepository;
    private final StableStallRepository stableStallRepository;
    private final TrainingWorkoutService workoutService;
    private final AreaRepository areaRepository;
    private final TrainingWorkoutRepository workoutRepository;
    private final HorseTrainingPlanRepository planRepository;
    private final CareScheduleRepository careScheduleRepository;
    private final GroomDailyTaskRepository groomDailyTaskRepository;
    private final NotificationService notificationService;

    public HorseService(HorseRepository horseRepository,
                        StableStallRepository stableStallRepository,
                        TrainingWorkoutService workoutService,
                        AreaRepository areaRepository) {
        this(horseRepository, stableStallRepository, workoutService, areaRepository,
                null, null, null, null, null);
    }

    @Autowired
    public HorseService(HorseRepository horseRepository,
                        StableStallRepository stableStallRepository,
                        TrainingWorkoutService workoutService,
                        AreaRepository areaRepository,
                        TrainingWorkoutRepository workoutRepository,
                        HorseTrainingPlanRepository planRepository,
                        CareScheduleRepository careScheduleRepository,
                        GroomDailyTaskRepository groomDailyTaskRepository,
                        NotificationService notificationService) {
        this.horseRepository = horseRepository;
        this.stableStallRepository = stableStallRepository;
        this.workoutService = workoutService;
        this.areaRepository = areaRepository;
        this.workoutRepository = workoutRepository;
        this.planRepository = planRepository;
        this.careScheduleRepository = careScheduleRepository;
        this.groomDailyTaskRepository = groomDailyTaskRepository;
        this.notificationService = notificationService;
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

        // Groom chỉ thấy ngựa đang ở trong chuồng mình chăm, áp dụng trước mọi bộ lọc khác.
        // (A groom sees only horses in the stalls they look after, before any other filter.)
        if (GROOM_ROLE.equals(currentUser.getRole())) {
            Set<Long> ownStallIds = stableStallRepository.findByGroomId(currentUser.getUserId()).stream()
                    .map(StableStall::getId)
                    .collect(Collectors.toSet());
            horses = horses.stream()
                    .filter(h -> h.getCurrentStallId() != null && ownStallIds.contains(h.getCurrentStallId()))
                    .toList();
        }

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

        return horses.stream()
                .sorted(Comparator.comparing(
                        Horse::getCreatedAt,
                        Comparator.nullsLast(Comparator.naturalOrder())
                ).reversed().thenComparing(Comparator.comparing(Horse::getId).reversed()))
                .toList();
    }

    public List<Horse> getAllHorses(AuthenticatedUser currentUser) {
        return getAllHorses(currentUser, null, null);
    }

    public Horse createHorse(CreateHorseRequest request) {
        if (request.getSex() == null) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST, "Sex is required (MALE or FEMALE)");
        }
        Horse horse = new Horse();
        horse.setName(request.getName());
        horse.setBreed(request.getBreed());
        horse.setSex(request.getSex());
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
        if (GROOM_ROLE.equals(currentUser.getRole())
                && (horse.getCurrentStallId() == null
                    || !stableStallRepository.existsByIdAndGroomId(horse.getCurrentStallId(), currentUser.getUserId()))) {
            throw new AccessDeniedException("You can only view horses in the stalls you look after");
        }
        return horse;
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
            workoutService.reassignFutureWorkoutsToGroom(horseId, null);

            stableStallRepository.findById(horse.getCurrentStallId()).ifPresent(oldStall -> {
                oldStall.setStatus(StallStatus.AVAILABLE);
                stableStallRepository.save(oldStall);
            });

            horse.setCurrentStallId(null);
            return horseRepository.save(horse);
        }

        StableStall newStall = stableStallRepository.findActiveByIdForUpdate(stallId)
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
            workoutService.reassignFutureWorkoutsToGroom(horseId, newStall.getGroomId());
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

    /**
     * Updates a horse's lifecycle status (ELIGIBLE <-> INACTIVE) with full cascade safety.
     * - Admin (CLUB_MANAGER) has full authority.
     * - Groom (GROOM) can only report ELIGIBLE -> INACTIVE for horses currently assigned to their stall.
     */
    @Transactional
    public Horse changeHorseStatus(Long horseId, HorseStatusChangeRequest request, AuthenticatedUser currentUser) {
        Horse horse = horseRepository.findById(horseId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "HORSE_NOT_FOUND", "Horse not found with id: " + horseId));

        HorseStatus currentStatus = horse.getCurrentStatus();
        HorseStatus targetStatus = request.status();

        if (currentStatus == targetStatus) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "NO_STATUS_CHANGE", "Horse already has status " + targetStatus);
        }

        // 1. Role & Permission checks
        String role = currentUser.getRole();
        if ("CLUB_MANAGER".equals(role)) {
            // Manager has full authority over permitted transitions
        } else if ("GROOM".equals(role)) {
            // Groom can only report ELIGIBLE -> INACTIVE for horses in their stall
            if (currentStatus != HorseStatus.ELIGIBLE || targetStatus != HorseStatus.INACTIVE) {
                throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN",
                        "Grooms are only permitted to report a horse as INACTIVE.");
            }
            if (horse.getCurrentStallId() == null ||
                    !stableStallRepository.existsByIdAndGroomId(horse.getCurrentStallId(), currentUser.getUserId())) {
                throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN",
                        "You are only permitted to update horses assigned to your stalls.");
            }
        } else {
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "You do not have permission to change horse status.");
        }

        // 2. Status Transition validation: only ELIGIBLE <-> INACTIVE
        if ((currentStatus != HorseStatus.ELIGIBLE && currentStatus != HorseStatus.INACTIVE) ||
                (targetStatus != HorseStatus.ELIGIBLE && targetStatus != HorseStatus.INACTIVE)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_STATUS_TRANSITION",
                    "Manual status changes are only permitted between ELIGIBLE and INACTIVE. Candidate and Rejected horses must follow the admission process.");
        }

        String effectiveReason = request.resolveEffectiveReason();
        String reasonCode = request.resolveReasonCode();
        if (effectiveReason.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "REASON_REQUIRED", "Reason is required when changing horse status.");
        }

        // 3. Cascade operations when transitioning to INACTIVE
        if (targetStatus == HorseStatus.INACTIVE) {
            // 3.1. Vacate stall & set to AVAILABLE
            if (horse.getCurrentStallId() != null) {
                stableStallRepository.findById(horse.getCurrentStallId()).ifPresent(stall -> {
                    stall.setStatus(StallStatus.AVAILABLE);
                    stableStallRepository.save(stall);
                });
                horse.setCurrentStallId(null);
            }

            // 3.2. Cancel upcoming workouts
            if (workoutRepository != null) {
                List<TrainingWorkout> futureWorkouts = workoutRepository
                        .findFutureScheduledByHorsesForUpdate(List.of(horseId), LocalDate.now());
                if (!futureWorkouts.isEmpty()) {
                    for (TrainingWorkout w : futureWorkouts) {
                        w.setStatus(WorkoutStatus.CANCELLED);
                    }
                    workoutRepository.saveAll(futureWorkouts);
                }
            }

            // 3.3. Cancel active and upcoming training plans
            if (planRepository != null) {
                List<HorseTrainingPlan> activePlans = planRepository
                        .findByHorseIdAndStatusInOrderByEndDateDesc(horseId, List.of(TrainingPlanStatus.ACTIVE, TrainingPlanStatus.UPCOMING));
                if (!activePlans.isEmpty()) {
                    activePlans.forEach(p -> p.setStatus(TrainingPlanStatus.CANCELLED));
                    planRepository.saveAll(activePlans);
                }
            }

            // 3.4. Cancel pending and scheduled medical appointments
            if (careScheduleRepository != null) {
                List<CareSchedule> pendingCares = careScheduleRepository.findByHorseIdAndStatusIn(
                        horseId, List.of(CareScheduleStatus.REQUESTED, CareScheduleStatus.SCHEDULED));
                if (!pendingCares.isEmpty()) {
                    for (CareSchedule cs : pendingCares) {
                        cs.setStatus(CareScheduleStatus.CANCELLED);
                        cs.setCancelReason("Horse marked INACTIVE: " + effectiveReason);
                    }
                    careScheduleRepository.saveAll(pendingCares);
                }
            }

            // 3.5. Block training
            horse.setTrainingDecision(TrainingDecision.BLOCKED);
            horse.setTrainingDecisionReason(effectiveReason);

            // 3.6. Clean uncompleted groom daily tasks
            if (groomDailyTaskRepository != null) {
                List<GroomDailyTask> uncompletedTasks = groomDailyTaskRepository.findByHorseIdAndIsCompletedFalse(horseId);
                if (!uncompletedTasks.isEmpty()) {
                    groomDailyTaskRepository.deleteAll(uncompletedTasks);
                }
            }

            // 3.7. Notify horse owner
            if (notificationService != null && horse.getOwnerId() != null) {
                notificationService.createNotification(
                        horse.getOwnerId(),
                        "Horse Status Updated: " + horse.getName(),
                        String.format("Your horse %s has been marked as INACTIVE. Reason: %s", horse.getName(), effectiveReason),
                        "HORSE_STATUS_CHANGE",
                        "HORSE",
                        horse.getId(),
                        null
                );
            }
        } else if (targetStatus == HorseStatus.ELIGIBLE) {
            // Restore to ELIGIBLE: assign to a regular stall. Take the shared stall-row locks first so the
            // fair pick cannot race another allocation for the same "least loaded" groom.
            stableStallRepository.lockAdmissionCapacityStallsForUpdate();
            StableStall regularStall;
            if (request.stallId() != null) {
                regularStall = stableStallRepository
                        .findAvailableRegularStallByIdForUpdate(request.stallId())
                        .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "STALL_UNAVAILABLE",
                                "The selected regular stall is no longer available."));
            } else {
                regularStall = stableStallRepository
                        .findFairAvailableStallForUpdate("REGULAR")
                        .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "NO_AVAILABLE_STALL",
                                "Cannot restore horse: No regular stalls are currently available. Please free a stall before restoring this horse."));
            }

            regularStall.setStatus(StallStatus.OCCUPIED);
            stableStallRepository.save(regularStall);

            horse.setCurrentStallId(regularStall.getId());

            // Reassign future workouts to the new stall's groom
            if (workoutService != null && regularStall.getGroomId() != null) {
                workoutService.reassignFutureWorkoutsToGroom(horseId, regularStall.getGroomId());
            }

            horse.setTrainingDecision(TrainingDecision.ALLOWED);
            horse.setTrainingDecisionReason(null);
        }

        // 4. Update status, statusReason, and statusReasonCode
        horse.setCurrentStatus(targetStatus);
        horse.setStatusReason(effectiveReason);
        horse.setStatusReasonCode(reasonCode);
        return horseRepository.save(horse);
    }
}
