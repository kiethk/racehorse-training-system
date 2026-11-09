package com.rtms.backend.training.service;

import com.rtms.backend.security.AuthenticatedUser;
import com.rtms.backend.training.dto.HorseAttendanceResponse;

import java.util.List;

/**
 * Lịch sử Course Run của một chiến mã, suy ra từ chính các Workout của nó (Workout, rồi Session, rồi Course Run, gom theo kế hoạch),
 * nên ngựa đổi nhóm bao nhiêu lần cũng có đủ lịch sử, mỗi kế hoạch một dòng.
 * (A horse's Course Run history, derived from its own Workouts (Workout, then Session, then Course Run, grouped by course run), so a horse
 * that changed groups keeps its whole history, one line per course run.)
 */
public interface HorseAttendanceService {

    /** Mới nhất trước. (Newest first.) */
    List<HorseAttendanceResponse> getHistory(Long horseId, AuthenticatedUser currentUser);
}
