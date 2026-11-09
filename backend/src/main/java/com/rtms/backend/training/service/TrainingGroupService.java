package com.rtms.backend.training.service;

import com.rtms.backend.security.AuthenticatedUser;
import com.rtms.backend.training.dto.CreateTrainingGroupRequest;
import com.rtms.backend.training.dto.GroupCandidateResponse;
import com.rtms.backend.training.dto.RenameTrainingGroupRequest;
import com.rtms.backend.training.dto.TrainingGroupResponse;

import java.util.List;

/**
 * Quản lý Training Group: nhóm chiến mã bền vững của một Trainer. Mọi luật thành viên (BR-10, khu của Trainer, được
 * phép tập) nằm ở đây và cũng được DB giữ.
 * (Management of Training Groups: the lasting groups of horses of one Trainer. Every membership rule (BR-10, the
 * Trainer's area, allowed to train) lives here and is also held by the DB.)
 */
public interface TrainingGroupService {

    /** Nhóm của Trainer; vai khác được xem (Quản lý) thấy mọi nhóm. (A Trainer's groups; other viewing roles see all.) */
    List<TrainingGroupResponse> getGroups(AuthenticatedUser currentUser);

    /** Các nhóm đã giải tán, chỉ để xem. (Disbanded groups, read-only.) */
    List<TrainingGroupResponse> getDisbandedGroups(AuthenticatedUser currentUser);

    TrainingGroupResponse createGroup(CreateTrainingGroupRequest request, AuthenticatedUser currentUser);

    TrainingGroupResponse renameGroup(Long groupId, RenameTrainingGroupRequest request, AuthenticatedUser currentUser);

    /**
     * Giải tán nhóm trong MỘT giao dịch: hủy mọi kế hoạch chưa đóng theo luật Cancel plan, cho mọi ngựa rời nhóm rồi đánh dấu Disbanded;
     * nhóm chưa từng có kế hoạch thì bị xóa hẳn.
     * (Disbands the group in ONE transaction: cancels every unfinished plan with the Cancel plan rules, takes every horse out, then
     * marks the group Disbanded; a group that never had a plan is deleted.)
     */
    void disbandGroup(Long groupId, AuthenticatedUser currentUser);

    TrainingGroupResponse addHorse(Long groupId, Long horseId, AuthenticatedUser currentUser);

    TrainingGroupResponse removeHorse(Long groupId, Long horseId, AuthenticatedUser currentUser);

    /** Đưa ngựa từ nhóm hiện tại sang nhóm đích trong một giao dịch. (Moves a horse from its group to the target in one transaction.) */
    TrainingGroupResponse moveHorse(Long targetGroupId, Long horseId, AuthenticatedUser currentUser);

    /** Các chiến mã trong khu của Trainer (chưa ở trong nhóm này) và vì sao con nào chưa vào được. (Horses of the Trainer's areas and why some cannot join.) */
    List<GroupCandidateResponse> getCandidates(Long groupId, AuthenticatedUser currentUser);
}
