package com.rtms.backend.training.service;

import com.rtms.backend.config.ApiException;
import com.rtms.backend.horse.entity.Horse;
import com.rtms.backend.horse.repository.HorseRepository;
import com.rtms.backend.security.AuthenticatedUser;
import com.rtms.backend.security.Roles;
import com.rtms.backend.stable.repository.StableStallRepository;
import com.rtms.backend.training.repository.CourseRunRepository;
import com.rtms.backend.training.repository.projection.DashboardHorseRow;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Chỗ DUY NHẤT trả lời "vai trò này được xem ngựa nào": danh sách ngựa trên dashboard của mỗi vai trò, và việc một người dùng có
 * được xem dữ liệu hiệu suất của một chiến mã hay không. Biểu đồ thể lực, cảnh báo, sở trường, dashboard và phân tích đều dùng
 * chung nên mọi nơi theo cùng một luật.
 * (The ONE place that answers "which horses may this role see": the horses on each role's dashboard, and whether a user may view
 * the performance data of one horse. The fitness chart, alerts, specialty, dashboard and analytics all share it so every screen
 * follows the same rule.)
 *
 *   HORSE_OWNER  : ngựa của chính mình (own horses)
 *   HEAD_TRAINER : ngựa trong các khu mình phụ trách (horses in the areas they manage)
 *   GROOM        : ngựa trong các chuồng mình chăm (horses in the stalls they look after)
 *   vai khác     : toàn bộ ngựa đã nhập trại (all admitted horses): Manager, Vet
 */
@Service
public class TrainingAccessPolicy {

    private final HorseRepository horseRepository;
    private final CourseRunRepository courseRunRepository;
    private final StableStallRepository stallRepository;

    public TrainingAccessPolicy(HorseRepository horseRepository,
                                CourseRunRepository courseRunRepository,
                                StableStallRepository stallRepository) {
        this.horseRepository = horseRepository;
        this.courseRunRepository = courseRunRepository;
        this.stallRepository = stallRepository;
    }

    /** Các chiến mã trên dashboard của người dùng. (The horses on the user's dashboard.) */
    public List<DashboardHorseRow> horsesOf(AuthenticatedUser currentUser) {
        return switch (currentUser.getRole()) {
            case Roles.HORSE_OWNER -> courseRunRepository.findDashboardHorsesOfOwner(currentUser.getUserId());
            case Roles.HEAD_TRAINER -> courseRunRepository.findDashboardHorses(currentUser.getUserId());
            default -> courseRunRepository.findAllDashboardHorses();
        };
    }

    public List<Long> horseIdsOf(AuthenticatedUser currentUser) {
        return horsesOf(currentUser).stream().map(DashboardHorseRow::horseId).toList();
    }

    /**
     * Owner chỉ xem ngựa của mình; Trainer chỉ xem ngựa trong khu mình phụ trách; Groom chỉ xem ngựa trong chuồng mình
     * chăm; vai khác theo quyền HORSE_VIEW.
     * (An owner sees only their own horses; a trainer only horses in their areas; a groom only horses in the stalls
     * they look after; other roles follow HORSE_VIEW.)
     */
    public void assertCanViewHorse(Long horseId, AuthenticatedUser currentUser) {
        Horse horse = horseRepository.findById(horseId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "HORSE_NOT_FOUND", "Horse not found!"));

        if (Roles.HORSE_OWNER.equals(currentUser.getRole())
                && !currentUser.getUserId().equals(horse.getOwnerId())) {
            throw new AccessDeniedException("You can only view horses you own");
        }
        if (Roles.HEAD_TRAINER.equals(currentUser.getRole())
                && !courseRunRepository.existsHorseInTrainerArea(horseId, currentUser.getUserId())) {
            throw new AccessDeniedException("You can only view horses in the areas you manage");
        }
        if (Roles.GROOM.equals(currentUser.getRole())
                && (horse.getCurrentStallId() == null
                    || !stallRepository.existsByIdAndGroomId(horse.getCurrentStallId(), currentUser.getUserId()))) {
            throw new AccessDeniedException("You can only view horses in the stalls you look after");
        }
    }
}
