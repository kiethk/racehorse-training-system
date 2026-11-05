package com.rtms.backend.training.service;

import com.rtms.backend.security.AuthenticatedUser;
import com.rtms.backend.training.dto.HorseAttendanceResponse;

import java.util.List;

/**
 * Lịch sử Group Plan của một chiến mã, suy ra từ chính các Workout của nó (Workout, rồi Session, rồi Group Plan, gom theo kế hoạch),
 * nên ngựa đổi nhóm bao nhiêu lần cũng có đủ lịch sử, mỗi kế hoạch một dòng.
 * (A horse's Group Plan history, derived from its own Workouts (Workout, then Session, then Group Plan, grouped by plan), so a horse
 * that changed groups keeps its whole history, one line per plan.)
 */
public interface HorseAttendanceService {

    /** Mới nhất trước. (Newest first.) */
    List<HorseAttendanceResponse> getHistory(Long horseId, AuthenticatedUser currentUser);
}
