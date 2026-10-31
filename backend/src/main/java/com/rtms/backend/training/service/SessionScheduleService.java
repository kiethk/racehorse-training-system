package com.rtms.backend.training.service;

import com.rtms.backend.security.AuthenticatedUser;
import com.rtms.backend.training.dto.ScheduleSessionResponse;
import com.rtms.backend.training.dto.SessionDetailResponse;

import java.time.LocalDate;
import java.util.List;

/**
 * Đọc lịch các buổi của nhóm theo khoảng ngày. Lịch hôm nay và lịch tuần dùng CHUNG một truy vấn này; mỗi vai thấy dữ liệu của mình:
 * Trainer các buổi của mình, Groom các buổi có ngựa mình dắt, Owner các buổi có ngựa của mình, Quản lý tất cả.
 * (Reads the schedule of group Sessions over a date range. Today and Week share THIS one query; each role sees its own data: a Trainer
 * their own Sessions, a Groom the Sessions with a horse they lead, an Owner the Sessions with one of their horses, a Manager all.)
 */
public interface SessionScheduleService {

    /**
     * @param groupId          giữ lại một nhóm, hoặc null cho tất cả (keeps one group, or null for all)
     * @param needsResultOnly  chỉ giữ buổi còn Workout đã HELD thiếu kết quả (keeps only Sessions with a HELD Workout missing results)
     */
    List<ScheduleSessionResponse> getSessions(LocalDate from, LocalDate to, Long groupId, boolean needsResultOnly,
                                              AuthenticatedUser currentUser);

    SessionDetailResponse getSession(Long sessionId, AuthenticatedUser currentUser);
}
