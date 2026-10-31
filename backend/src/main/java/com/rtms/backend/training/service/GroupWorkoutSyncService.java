package com.rtms.backend.training.service;

import com.rtms.backend.training.entity.TrainingGroupMember;

/**
 * Giữ Workout khớp với thành viên của nhóm khi nhóm đang chạy một khóa: ngựa vào giữa khóa chỉ có Workout cho các buổi CHƯA bắt đầu,
 * ngựa ra (rút, chuyển nhóm, bị Thú y chặn tập) bị hủy các Workout của buổi chưa bắt đầu và nhóm cùng các ngựa còn lại không đổi.
 * (Keeps Workouts matching the members of a group while it runs a course: a horse that joins mid-course only gets Workouts for the
 * Sessions that have NOT started, and a horse that leaves (withdrawn, moved, blocked by the Vet) has its not-yet-started Workouts
 * cancelled while the group and the other horses stay as they are.)
 */
public interface GroupWorkoutSyncService {

    /**
     * Nhóm đang có khóa chưa đóng mà không còn buổi nào chưa bắt đầu thì không nhận thêm ngựa, vì ngựa đó sẽ không có buổi nào để học.
     * (A group with an unfinished course but no Session left that has not started takes no new horse, since it would have nothing to attend.)
     */
    void assertCanJoin(Long groupId);

    /** Tạo Workout cho thành viên mới trong các buổi chưa bắt đầu của khóa chưa đóng. (Creates the new member's Workouts in the not-yet-started Sessions of unfinished courses.) */
    int createFutureWorkouts(TrainingGroupMember member);

    /**
     * Cho thành viên ra khỏi nhóm: hủy Workout của các buổi chưa bắt đầu, hủy buổi nào không còn Workout nào, xóa tư cách thành viên và
     * để kế hoạch tự đóng nếu không còn buổi nào chờ chạy.
     * (Takes a member out of the group: cancels its Workouts in not-yet-started Sessions, cancels any Session left with none, deletes
     * the membership and lets the plan close by itself if no Session is left to run.)
     */
    void exitGroup(TrainingGroupMember member);

    /**
     * Đổi Groom của MỘT chiến mã cho các buổi chưa bắt đầu và cho tư cách thành viên nhóm; null khi gỡ Groom khỏi chuồng. Buổi đã diễn
     * ra giữ Groom cũ vì đó là lịch sử.
     * (Changes the Groom of ONE horse for its not-yet-started Sessions and for its group membership; null when the Groom is removed
     * from the stall. Sessions already held keep the old Groom, which is history.)
     *
     * @return số Workout đã chuyển (number of Workouts moved)
     * @throws IllegalStateException nếu hai thành viên của một nhóm sẽ cùng Groom, BR-09 (if two members of a group would share a Groom)
     */
    int reassignFutureWorkoutsToGroom(Long horseId, Long newGroomId);

    /**
     * Như trên cho NHIỀU ngựa cùng lúc; kiểm tra phân bổ cuối cùng TRƯỚC khi đổi bất cứ thứ gì nên hoán đổi Groom giữa hai ngựa vẫn
     * được. Giá trị null nghĩa là gỡ Groom.
     * (Like the above for MANY horses at once; the final allocation is validated BEFORE anything changes, so swapping Grooms between
     * two horses works. A null value removes the Groom.)
     */
    int reassignFutureWorkoutsToGrooms(java.util.Map<Long, Long> groomByHorse);

    /** Như exitGroup cho ngựa bất kỳ nhóm nào; không làm gì nếu ngựa không ở nhóm nào. (Like exitGroup for a horse in any group; does nothing if it is in none.) */
    void exitAnyGroup(Long horseId);
}
