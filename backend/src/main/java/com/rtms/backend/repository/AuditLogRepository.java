package com.rtms.backend.repository;

import com.rtms.backend.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    @Query("""
            SELECT a FROM AuditLog a
            WHERE (:userId IS NULL OR a.user.id = :userId)
              AND (:httpMethod IS NULL OR a.httpMethod = :httpMethod)
              AND (:statusCode IS NULL OR a.statusCode = :statusCode)
              AND (:from IS NULL OR a.createdAt >= :from)
              AND (:to IS NULL OR a.createdAt <= :to)
              AND (:search IS NULL OR LOWER(a.requestPath) LIKE LOWER(CONCAT('%', :search, '%')))
            ORDER BY a.createdAt DESC
            """)
    Page<AuditLog> findWithFilters(
            @Param("userId") Long userId,
            @Param("httpMethod") String httpMethod,
            @Param("statusCode") Integer statusCode,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            @Param("search") String search,
            Pageable pageable
    );
}

