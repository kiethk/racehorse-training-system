package com.rtms.backend.audit.service;

import com.rtms.backend.config.ApiException;
import com.rtms.backend.audit.dto.AuditLogResponse;
import com.rtms.backend.audit.entity.AuditLog;
import com.rtms.backend.identity.entity.User;
import com.rtms.backend.audit.repository.AuditLogRepository;
import com.rtms.backend.identity.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;

@Service
public class AuditLogService {

    private static final Logger logger = LoggerFactory.getLogger(AuditLogService.class);

    private static final Set<String> VALID_WRITE_METHODS = Set.of("POST", "PUT", "PATCH", "DELETE");
    private static final int MAX_PAGE_SIZE = 50;
    private static final int DEFAULT_PAGE_SIZE = 20;

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    public AuditLogService(AuditLogRepository auditLogRepository, UserRepository userRepository) {
        this.auditLogRepository = auditLogRepository;
        this.userRepository = userRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordHttpRequest(Long userId, String httpMethod, String requestPath, int statusCode) {
        try {
            AuditLog log = new AuditLog();

            if (userId != null) {
                User user = userRepository.findById(userId).orElse(null);
                log.setUser(user);
            }

            log.setHttpMethod(httpMethod);
            log.setRequestPath(requestPath);
            log.setStatusCode(statusCode);
            // action, entityName, entityId left null for baseline HTTP audit

            auditLogRepository.save(log);
        } catch (Exception e) {
            logger.error("Failed to persist audit log for {} {}: {}", httpMethod, requestPath, e.getMessage(), e);
        }
    }

    @Transactional(readOnly = true)
    public Page<AuditLogResponse> getAuditLogs(
            Long userId,
            String httpMethod,
            Integer statusCode,
            LocalDateTime from,
            LocalDateTime to,
            String search,
            Pageable pageable
    ) {
        // Validate httpMethod filter
        String normalizedMethod = null;
        if (httpMethod != null && !httpMethod.isBlank()) {
            normalizedMethod = httpMethod.trim().toUpperCase();
            if (!VALID_WRITE_METHODS.contains(normalizedMethod)) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
                        "Invalid httpMethod filter. Accepted values: POST, PUT, PATCH, DELETE");
            }
        }

        // Bound page size
        int boundedSize = Math.max(1, Math.min(pageable.getPageSize(), MAX_PAGE_SIZE));
        Pageable boundedPageable = PageRequest.of(
                Math.max(0, pageable.getPageNumber()),
                boundedSize,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        String searchTerm = (search != null && !search.isBlank()) ? search.trim().toLowerCase() : null;
        String finalMethod = normalizedMethod;

        Page<AuditLog> page = auditLogRepository.findAll((root, query, cb) -> {
            java.util.List<jakarta.persistence.criteria.Predicate> predicates = new java.util.ArrayList<>();
            
            if (userId != null) {
                predicates.add(cb.equal(root.get("user").get("id"), userId));
            }
            if (finalMethod != null) {
                predicates.add(cb.equal(root.get("httpMethod"), finalMethod));
            }
            if (statusCode != null) {
                predicates.add(cb.equal(root.get("statusCode"), statusCode));
            }
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), from));
            }
            if (to != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), to));
            }
            if (searchTerm != null) {
                predicates.add(cb.like(cb.lower(root.get("requestPath")), "%" + searchTerm + "%"));
            }
            
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        }, boundedPageable);

        return page.map(AuditLogResponse::from);
    }
}

