package com.rtms.backend.service;

import com.rtms.backend.dto.CreateHorseRequest;
import com.rtms.backend.dto.UpdateHorseStatusRequest;
import com.rtms.backend.entity.Horse;
import com.rtms.backend.entity.StableStall;
import com.rtms.backend.enums.HorseStatus;
import com.rtms.backend.enums.StallStatus;
import com.rtms.backend.repository.HorseRepository;
import com.rtms.backend.repository.StableStallRepository;
import com.rtms.backend.security.AuthenticatedUser;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.rtms.backend.entity.Area;
import com.rtms.backend.repository.AreaRepository;

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

    /**
     * @param mine   true + vai trò HEAD_TRAINER -> chỉ ngựa trong khu mình phụ trách.
     *               Suy qua Horse.currentStallId -> StableStall.areaId -> Area.trainerId,
     *               không cần thêm cột nào vào bảng horses.
     * @param status lọc theo trạng thái, ví dụ ELIGIBLE để bỏ ngựa CANDIDATE
     *               khỏi danh sách ghi danh.
     *
     * Tham số TUỲ CHỌN: không truyền thì hành vi y hệt trước, nên không
     * màn hình nào của actor khác bị ảnh hưởng.
     */
    public List<Horse> getAllHorses(AuthenticatedUser currentUser,
                                    Boolean mine,
                                    HorseStatus status) {

        List<Horse> horses = "HORSE_OWNER".equals(currentUser.getRole())
                ? horseRepository.findByOwnerId(currentUser.getUserId())
                : horseRepository.findAll();

        if (Boolean.TRUE.equals(mine) && "HEAD_TRAINER".equals(currentUser.getRole())) {
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

        // ===== THÊM MỚI: cascade huỷ huấn luyện =====
        // Mọi trạng thái khác ELIGIBLE đều là khoá huấn luyện (khớp với
        // InjuryRecordService.getTrainingLockStatus).
        if (newStatus != HorseStatus.ELIGIBLE) {
            trainingPlanService.cancelFutureTrainingForHorse(
                    saved.getId(),
                    "Chiến mã chuyển sang trạng thái " + newStatus);
        }

        return saved;
    }

    @Transactional
    public Horse assignStall(Long horseId, Long stallId) {
        if (horseId == null) {
            throw new RuntimeException("Horse ID is required");
        }
        if (stallId == null) {
            throw new RuntimeException("Stall ID is required");
        }

        Horse horse = horseRepository.findById(horseId)
                .orElseThrow(() -> new RuntimeException("Horse not found with id: " + horseId));

        StableStall newStall = stableStallRepository.findById(stallId)
                .orElseThrow(() -> new RuntimeException("Stable stall not found with id: " + stallId));

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

}