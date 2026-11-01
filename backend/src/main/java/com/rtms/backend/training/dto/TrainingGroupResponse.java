package com.rtms.backend.training.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * Một nhóm cùng thành viên và khóa đang chạy hoặc sắp chạy (nếu có). currentPlanStatus rỗng khi nhóm đang rảnh.
 * (A group with its members and the course it is running or about to run (if any). currentPlanStatus is null when the
 * group is free.)
 */
public record TrainingGroupResponse(Long id,
                                    String name,
                                    Long trainerId,
                                    List<GroupMemberResponse> members,
                                    String currentCourseName,
                                    String currentPlanStatus,
                                    LocalDate currentPlanEndDate,
                                    boolean disbanded) {
}
