package com.rtms.backend.training.service;

import com.rtms.backend.horse.entity.Horse;
import com.rtms.backend.horse.repository.HorseRepository;
import com.rtms.backend.medical.repository.CareScheduleRepository;
import com.rtms.backend.training.enums.TrainingDecision;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


/**
 * Nơi DUY NHẤT đổi quyết định "ngựa có được tập không".
 * (The ONLY place that changes the "may the horse train" decision.)
 *
 * Trước đây 8 chỗ (Vet, Groom, Manager, sự cố...) tự đặt 3-4 trường khóa trên ngựa và không chỗ nào hủy kế
 * hoạch đang chạy: ngựa bị chặn mà buổi tập vẫn nằm trong session, Groom vẫn được giao dắt đi tập. Gom về đây để
 * chặn tập luôn kéo theo hủy buổi tập tương lai.
 * (Before, 8 places (vet, groom, manager, incidents...) set 3-4 lock fields on the horse themselves and none
 * cancelled running course runs: a horse was blocked while its workouts stayed in the session and the groom was still told
 * to lead it out. They are gathered here so blocking always cancels future workouts.)
 */
@Service
public class TrainingDecisionService {

    private final HorseRepository horseRepository;
    private final TrainingGroupService groupService;
    private final TrainingEligibility eligibility;

    public TrainingDecisionService(HorseRepository horseRepository,
                                   CareScheduleRepository careScheduleRepository,
                                   TrainingGroupService groupService) {
        this.horseRepository = horseRepository;
        this.groupService = groupService;
        this.eligibility = new TrainingEligibility(careScheduleRepository);
    }

    /**
     * Chặn tập và hủy mọi buổi tập chưa diễn ra (session rỗng bị hủy theo, course run đang chạy chuyển CANCELLED). Gọi lại
     * khi ngựa đã bị chặn là vô hại: lần hủy thứ hai không còn gì để hủy.
     * (Blocks training and cancels every workout not yet held (empty sessions are cancelled too, running course runs become
     * CANCELLED). Calling again on an already blocked horse is harmless: the second cancel has nothing left.)
     */
    @Transactional
    public void block(Horse horse, String reason) {
        horse.setTrainingDecision(TrainingDecision.BLOCKED);
        horse.setTrainingDecisionReason(reason);
        horseRepository.save(horse);
        // Ngựa bị chặn ra khỏi nhóm; các ngựa còn lại vẫn tập bình thường. (A blocked horse leaves its group; the others train on.)
        groupService.exitAnyGroup(horse.getId());
    }

    @Transactional
    public void allow(Horse horse) {
        horse.setTrainingDecision(TrainingDecision.ALLOWED);
        horse.setTrainingDecisionReason(null);
        horseRepository.save(horse);
    }

    /** Ném lỗi kèm lý do cụ thể nếu ngựa chưa tập được. (Throws an error with a concrete reason if the horse cannot train yet.) */
    public void assertCanTrain(Horse horse) {
        eligibility.assertCanTrain(horse);
    }

    /** Lý do nghỉ tập và hạn khám lại, ví dụ "Tendon strain (re-examination at 20/10/2026 14:00)". (The rest reason and re-examination time.) */
    public String describeRest(Horse horse) {
        return eligibility.describeRest(horse);
    }
}
