package com.rtms.backend.training.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Một buổi trên lịch của Trainer, Groom hoặc Owner. horseCount và needsResultCount chỉ đếm những ngựa người xem được phép thấy;
 * needsResultCount là số Workout đã HELD mà chưa có kết quả.
 * groupId, groupName and groupDisbanded are empty for an Owner or a Groom, who know horses and not groups; horseNames (the horses the
 * viewer may see, not cancelled) stand in for the group name. trainerName is the Trainer who leads the Session, so a view that mixes several Trainers (an Owner's or a Groom's) can tell them apart.
 * (A Session on the schedule of a Trainer, Groom or Owner. horseCount and needsResultCount only count the horses the viewer may see;
 * needsResultCount is the number of HELD Workouts that still have no result.)
 */
public record SessionSummaryResponse(Long id,
                                      Long courseRunId,
                                      Long groupId,
                                      String groupName,
                                      boolean groupDisbanded,
                                      String courseName,
                                      String exerciseName,
                                      LocalDate date,
                                      LocalTime startTime,
                                      LocalTime endTime,
                                      String status,
                                      int horseCount,
                                      int needsResultCount,
                                      Long trainerId,
                                      String trainerName,
                                      List<String> horseNames) {
}
