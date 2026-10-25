package com.rtms.backend.training.service;

import com.rtms.backend.security.AuthenticatedUser;
import com.rtms.backend.training.dto.DashboardHorseRow;
import com.rtms.backend.training.repository.HorseTrainingPlanRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Các chiến mã mà một vai trò được xem trên dashboard của mình. Dùng chung cho mọi biểu đồ và bảng của dashboard để
 * mỗi vai trò chỉ có MỘT định nghĩa "ngựa của tôi".
 * (The horses a role may see on its dashboard. Shared by every chart and table of the dashboard so each role has
 * exactly ONE definition of "my horses".)
 *
 *   HORSE_OWNER  : ngựa của chính mình (own horses)
 *   HEAD_TRAINER : ngựa trong các khu mình phụ trách (horses in the areas they manage)
 *   vai khác     : toàn bộ ngựa đã nhập trại (all admitted horses): Manager, Vet
 */
@Service
public class HorseScopeService {

    private final HorseTrainingPlanRepository planRepository;

    public HorseScopeService(HorseTrainingPlanRepository planRepository) {
        this.planRepository = planRepository;
    }

    public List<DashboardHorseRow> horsesOf(AuthenticatedUser currentUser) {
        return switch (currentUser.getRole()) {
            case "HORSE_OWNER" -> planRepository.findDashboardHorsesOfOwner(currentUser.getUserId());
            case "HEAD_TRAINER" -> planRepository.findDashboardHorses(currentUser.getUserId());
            default -> planRepository.findAllDashboardHorses();
        };
    }

    public List<Long> horseIdsOf(AuthenticatedUser currentUser) {
        return horsesOf(currentUser).stream().map(DashboardHorseRow::horseId).toList();
    }
}
