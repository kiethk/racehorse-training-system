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
    private final AuditActivityResolver activityResolver;

    public AuditLogService(AuditLogRepository auditLogRepository,
                           UserRepository userRepository,
                           AuditActivityResolver activityResolver) {
        this.auditLogRepository = auditLogRepository;
        this.userRepository = userRepository;
        this.activityResolver = activityResolver;
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

            // Resolve business activity details
            var activity = activityResolver.resolve(httpMethod, requestPath, statusCode);
            log.setAction(activity.action());
            log.setEntityName(activity.entityName());
            log.setEntityId(activity.entityId());
            log.setDetails(activity.details());

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
                var pathLike = cb.like(cb.lower(root.get("requestPath")), "%" + searchTerm + "%");
                var actionLike = cb.like(cb.lower(root.get("action")), "%" + searchTerm + "%");
                var detailsLike = cb.like(cb.lower(root.get("details")), "%" + searchTerm + "%");
                var entityLike = cb.like(cb.lower(root.get("entityName")), "%" + searchTerm + "%");
                var userJoin = root.join("user", jakarta.persistence.criteria.JoinType.LEFT);
                var userNameLike = cb.like(cb.lower(userJoin.get("fullName")), "%" + searchTerm + "%");
                predicates.add(cb.or(pathLike, actionLike, detailsLike, entityLike, userNameLike));
            }
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        }, boundedPageable);

        return page.map(log -> {
            AuditLogResponse dto = AuditLogResponse.from(log);
            if (dto.getDetails() == null || dto.getDetails().isBlank() || dto.getAction() == null || dto.getAction().isBlank()) {
                var resolved = activityResolver.resolve(
                        log.getHttpMethod(),
                        log.getRequestPath(),
                        log.getStatusCode() != null ? log.getStatusCode() : 200
                );
                if (dto.getAction() == null || dto.getAction().isBlank()) {
                    dto.setAction(resolved.action());
                }
                if (dto.getEntityName() == null || dto.getEntityName().isBlank()) {
                    dto.setEntityName(resolved.entityName());
                }
                if (dto.getEntityId() == null) {
                    dto.setEntityId(resolved.entityId());
                }
                if (dto.getDetails() == null || dto.getDetails().isBlank()) {
                    dto.setDetails(resolved.details());
                }
            }
            if (dto.getAction() != null) {
                dto.setAction(formatAction(dto.getAction()));
            }
            return dto;
        });
    }

    private String formatAction(String action) {
        if (action == null || action.isBlank()) {
            return "System Operation";
        }
        if ("CARE_SCHEDULE_AUTO_ASSIGNED".equalsIgnoreCase(action)) {
            return "Care Schedule Auto Assigned";
        }
        if ("CARE_SCHEDULE_RESCHEDULED".equalsIgnoreCase(action)) {
            return "Reschedule Medical Care";
        }
        if (action.contains("_") && action.equals(action.toUpperCase())) {
            String[] parts = action.toLowerCase().split("_");
            StringBuilder sb = new StringBuilder();
            for (String part : parts) {
                if (!part.isEmpty()) {
                    if (!sb.isEmpty()) sb.append(" ");
                    sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
                }
            }
            return sb.toString();
        }
        return action;
    }
}

