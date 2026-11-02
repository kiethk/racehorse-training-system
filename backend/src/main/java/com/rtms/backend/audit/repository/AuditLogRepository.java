package com.rtms.backend.audit.repository;

import com.rtms.backend.audit.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long>, JpaSpecificationExecutor<AuditLog> {

    long countByCreatedAtBefore(LocalDateTime cutoff);

    long countByCreatedAtBetween(LocalDateTime from, LocalDateTime to);

    Optional<AuditLog> findTopByOrderByCreatedAtAsc();

    List<AuditLog> findByCreatedAtBeforeOrderByCreatedAtAsc(LocalDateTime cutoff);

    List<AuditLog> findByCreatedAtBetweenOrderByCreatedAtAsc(LocalDateTime from, LocalDateTime to);

    @Modifying
    @Query("DELETE FROM AuditLog a WHERE a.createdAt < :cutoff")
    int deleteByCreatedAtBefore(@Param("cutoff") LocalDateTime cutoff);

    @Modifying
    @Query("DELETE FROM AuditLog a WHERE a.createdAt >= :from AND a.createdAt <= :to")
    int deleteByCreatedAtBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
}
