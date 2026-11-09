package com.rtms.backend.training.service;

import com.rtms.backend.security.AuthenticatedUser;
import com.rtms.backend.training.dto.SessionDetailResponse;
import com.rtms.backend.training.dto.SessionSummaryResponse;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Các buổi của nhóm: đọc lịch theo khoảng ngày và chi tiết một buổi (mỗi vai thấy dữ liệu của mình), cùng dời giờ và hủy buổi của
 * Trainer. Dời giờ và hủy theo MỘT luật: chỉ làm được khi buổi CHƯA bắt đầu. Buổi đã bắt đầu thì chạy đến hết, sau đó chỉ còn việc
 * ghi kết quả (xem TrainingWorkoutService).
 * (The group's Sessions: the schedule over a date range and one Session's detail (each role sees its own data), and the Trainer's
 * move and cancel. Move and cancel follow ONE rule: only before the Session has STARTED. A Session that has started runs to its end,
 * after which only recording results remains (see TrainingWorkoutService).)
 */
public interface TrainingSessionService {

    /**
     * Lịch hôm nay và lịch tuần dùng CHUNG truy vấn này. Trainer thấy các buổi của mình, Groom các buổi có ngựa mình dắt, Owner các
     * buổi có ngựa của mình, Quản lý tất cả.
     * (Today and Week share THIS query. A Trainer sees their own Sessions, a Groom the Sessions with a horse they lead, an Owner the
     * Sessions with one of their horses, a Manager all.)
     *
     * @param groupId         giữ lại một nhóm, hoặc null cho tất cả (keeps one group, or null for all)
     * @param needsResultOnly chỉ giữ buổi còn Workout đã HELD thiếu kết quả (keeps only Sessions with a HELD Workout missing results)
     */
    List<SessionSummaryResponse> getSessions(LocalDate from, LocalDate to, Long groupId, boolean needsResultOnly,
                                             AuthenticatedUser currentUser);

    SessionDetailResponse getSession(Long sessionId, AuthenticatedUser currentUser);

    /** Dời giờ bắt đầu; giờ mới phải đúng mốc 15 phút, trong Golden Window và không chồng buổi nào khác của Trainer. (Moves the start; the new time must be on the 15-minute step, inside the Golden Window and overlap no other Session of the Trainer.) */
    SessionSummaryResponse reschedule(Long sessionId, LocalTime newStartTime, AuthenticatedUser currentUser);

    /** Hủy cả buổi (mưa, sân đóng) cùng mọi Workout còn SCHEDULED của nó; không dạy bù. (Cancels the whole Session and its SCHEDULED Workouts; no make-up.) */
    SessionSummaryResponse cancel(Long sessionId, AuthenticatedUser currentUser);
}
