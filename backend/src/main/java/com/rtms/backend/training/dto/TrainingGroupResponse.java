package com.rtms.backend.training.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * Một nhóm cùng thành viên và khóa đang chạy hoặc sắp chạy (nếu có). currentCourseRunStatus rỗng khi nhóm đang rảnh.
 * (A group with its members and the course it is running or about to run (if any). currentCourseRunStatus is null when the
 * group is free.)
 */
public record TrainingGroupResponse(Long id,
                                    String name,
                                    Long trainerId,
                                    List<GroupMemberResponse> members,
                                    String currentCourseName,
                                    String currentCourseRunStatus,
                                    LocalDate currentCourseRunStartDate,
                                    LocalDate currentCourseRunEndDate,
                                    boolean disbanded) {
}
