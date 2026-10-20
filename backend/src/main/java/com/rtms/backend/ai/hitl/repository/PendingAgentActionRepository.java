package com.rtms.backend.ai.hitl.repository;

import com.rtms.backend.ai.hitl.enums.ActionStatus;
import com.rtms.backend.ai.hitl.model.PendingAgentAction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PendingAgentActionRepository extends JpaRepository<PendingAgentAction, Long> {

    Optional<PendingAgentAction> findByActionToken(String actionToken);

    List<PendingAgentAction> findByUserIdAndStatus(Long userId, ActionStatus status);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE PendingAgentAction a SET a.status = :newStatus WHERE a.id = :id AND a.status = :expectedStatus")
    int updateStatusIfStatusIs(@Param("id") Long id,
                               @Param("newStatus") ActionStatus newStatus,
                               @Param("expectedStatus") ActionStatus expectedStatus);
}
