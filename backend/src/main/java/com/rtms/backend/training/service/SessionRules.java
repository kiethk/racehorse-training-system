package com.rtms.backend.training.service;

import com.rtms.backend.config.ApiException;
import com.rtms.backend.security.AuthenticatedUser;
import com.rtms.backend.training.entity.TrainingSession;
import com.rtms.backend.training.enums.SessionStatus;
import com.rtms.backend.training.enums.WorkoutStatus;
import com.rtms.backend.training.repository.TrainingSessionRepository;
import com.rtms.backend.training.repository.TrainingWorkoutRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * Các luật về một buổi mà nhiều service cùng dùng: buổi của Trainer này, "chỉ khi buổi chưa bắt đầu", và "buổi hết Workout hiệu lực
 * thì tự hủy". Mỗi luật nằm ở MỘT chỗ để hủy buổi, hủy một Workout và ngựa rời nhóm không thể lệch nhau.
 * (The Session rules several services share: a Session of this Trainer, "only before the Session starts", and "a Session left with
 * no effective Workout cancels itself". Each rule lives in ONE place so cancelling a Session, cancelling a Workout and a horse
 * leaving a group can never disagree.)
 */
@Component
class SessionRules {

    private final TrainingSessionRepository sessionRepository;
    private final TrainingWorkoutRepository workoutRepository;
    private final Clock clock;

    SessionRules(TrainingSessionRepository sessionRepository, TrainingWorkoutRepository workoutRepository, Clock clock) {
        this.sessionRepository = sessionRepository;
        this.workoutRepository = workoutRepository;
        this.clock = clock;
    }

    /** Buổi của Trainer này; của người khác báo "không tìm thấy" để không lộ nó tồn tại. (A Session of this Trainer; another Trainer's is "not found".) */
    TrainingSession findOwnSession(Long sessionId, AuthenticatedUser currentUser) {
        return sessionRepository.findById(sessionId)
                .filter(found -> found.getTrainerId().equals(currentUser.getUserId()))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "SESSION_NOT_FOUND", "Session not found!"));
    }

    /** Luật chung: chỉ buổi còn SCHEDULED và CHƯA bắt đầu mới đổi được. (The shared rule: only a SCHEDULED Session that has NOT started can change.) */
    void assertNotStarted(TrainingSession session) {
        if (session.getStatus() == SessionStatus.CANCELLED) {
            throw new ApiException(HttpStatus.CONFLICT, "SESSION_CANCELLED", "This session was already cancelled!");
        }
        if (session.getStatus() == SessionStatus.HELD || !LocalDateTime.now(clock).isBefore(session.startsAt())) {
            throw new ApiException(HttpStatus.CONFLICT, "SESSION_ALREADY_STARTED",
                    "This session has already started, so it can no longer be changed!");
        }
    }

    /**
     * Buổi không còn Workout nào hiệu lực thì tự hủy.
     * (A Session with no effective Workout left cancels itself.)
     *
     * @return true nếu buổi vừa bị hủy (true when the Session was just cancelled)
     */
    boolean cancelIfEmpty(TrainingSession session) {
        if (workoutRepository.countBySessionIdAndStatusNot(session.getId(), WorkoutStatus.CANCELLED) == 0) {
            session.setStatus(SessionStatus.CANCELLED);
            sessionRepository.saveAndFlush(session);
            return true;
        }
        return false;
    }
}
