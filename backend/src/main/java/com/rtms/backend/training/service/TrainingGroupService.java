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
     * Giải tán nhóm trong MỘT giao dịch: hủy mọi kế hoạch chưa đóng theo luật Cancel course run, cho mọi ngựa rời nhóm rồi đánh dấu Disbanded;
     * nhóm chưa từng có kế hoạch thì bị xóa hẳn.
     * (Disbands the group in ONE transaction: cancels every unfinished course run with the Cancel course run rules, takes every horse out, then
     * marks the group Disbanded; a group that never had a course run is deleted.)
     */
    void disbandGroup(Long groupId, AuthenticatedUser currentUser);

    TrainingGroupResponse addHorse(Long groupId, Long horseId, AuthenticatedUser currentUser);

    TrainingGroupResponse removeHorse(Long groupId, Long horseId, AuthenticatedUser currentUser);

    /** Đưa ngựa từ nhóm hiện tại sang nhóm đích trong một giao dịch. (Moves a horse from its group to the target in one transaction.) */
    TrainingGroupResponse moveHorse(Long targetGroupId, Long horseId, AuthenticatedUser currentUser);

    /** Các chiến mã trong khu của Trainer (chưa ở trong nhóm này) và vì sao con nào chưa vào được. (Horses of the Trainer's areas and why some cannot join.) */
    List<GroupCandidateResponse> getCandidates(Long groupId, AuthenticatedUser currentUser);
    /** Cho ngựa rời nhóm hiện tại (nếu có): hủy Workout của buổi chưa bắt đầu. Dùng khi Thú y chặn tập hoặc ngựa không còn hoạt động. (Takes a horse out of its current group, if any, cancelling its not-yet-started Workouts. Used when the Vet blocks training or the horse is no longer active.) */
    void exitAnyGroup(Long horseId);

    /** Đổi Groom của MỘT chiến mã cho các buổi chưa bắt đầu và cho tư cách thành viên; null khi gỡ Groom khỏi chuồng. Buổi đã diễn ra giữ Groom cũ vì đó là lịch sử. (Changes the Groom of ONE horse for its not-yet-started Sessions and its membership; null when the Groom is removed from the stall. Sessions already held keep the old Groom, which is history.) */
    int reassignFutureWorkoutsToGroom(Long horseId, Long newGroomId);

    /** Như trên cho NHIỀU ngựa cùng lúc; giá trị null nghĩa là gỡ Groom. (Like the above for MANY horses at once; a null value removes the Groom.) */
    int reassignFutureWorkoutsToGrooms(java.util.Map<Long, Long> groomByHorse);
}
