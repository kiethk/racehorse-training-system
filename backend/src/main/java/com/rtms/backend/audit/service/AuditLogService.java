package com.rtms.backend.audit.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rtms.backend.audit.dto.DomainAuditEvent;
import com.rtms.backend.config.ApiException;
import com.rtms.backend.audit.dto.AuditLogResponse;
import com.rtms.backend.audit.entity.AuditLog;
import com.rtms.backend.identity.entity.User;
import com.rtms.backend.audit.repository.AuditLogRepository;
import com.rtms.backend.identity.repository.UserRepository;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class AuditLogService {

    private static final Logger logger = LoggerFactory.getLogger(AuditLogService.class);

    private static final Set<String> VALID_WRITE_METHODS = Set.of("POST", "PUT", "PATCH", "DELETE");
    private static final int MAX_PAGE_SIZE = 50;
    private static final int DEFAULT_PAGE_SIZE = 20;

    private static final Map<String, List<String>> MODULE_ENTITY_MAP = Map.of(
            "ADMISSIONS", List.of("AdmissionApplication", "Admission"),
            "MEDICAL", List.of("CareSchedule", "HealthRecord", "TreatmentPlan", "PreventiveCareSchedule", "IncidentReport", "Medical"),
            "STABLE", List.of("StableStall", "Area", "GroomDailyTask", "Stable"),
            "HORSES", List.of("Horse", "Horses"),
            "STAFF", List.of("User", "Staff", "Account"),
            "TRAINING", List.of("Course", "HorseTrainingPlan", "Subject", "TrainingLot", "TrainingWorkout", "TrainingRule", "Training"),
            "RACING", List.of("RaceRegistration", "Racing")
    );

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;
    private final AuditActivityResolver activityResolver;
    private final ObjectMapper objectMapper;

    @Autowired
    public AuditLogService(AuditLogRepository auditLogRepository,
                           UserRepository userRepository,
                           AuditActivityResolver activityResolver,
                           ObjectMapper objectMapper) {
        this.auditLogRepository = auditLogRepository;
        this.userRepository = userRepository;
        this.activityResolver = activityResolver;
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
    }

    public AuditLogService(AuditLogRepository auditLogRepository,
                           UserRepository userRepository,
                           AuditActivityResolver activityResolver) {
        this(auditLogRepository, userRepository, activityResolver, new ObjectMapper());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordDomainEvent(DomainAuditEvent event) {
        if (event == null) return;
        try {
            AuditLog log = new AuditLog();

            if (event.userId() != null) {
                User user = userRepository.findById(event.userId()).orElse(null);
                log.setUser(user);
            }

            log.setAction(event.action());
            log.setEntityName(event.entityName());
            log.setEntityId(event.entityId());
            log.setHttpMethod("DOMAIN");
            log.setRequestPath(event.targetUrl());
            log.setStatusCode(200);

            Map<String, Object> payload = new LinkedHashMap<>();
            if (event.summary() != null) payload.put("summary", event.summary());
            if (event.reason() != null) payload.put("reason", event.reason());
            if (event.module() != null) payload.put("module", event.module());
            if (event.targetUrl() != null) payload.put("targetUrl", event.targetUrl());
            if (event.before() != null) payload.put("before", event.before());
            if (event.after() != null) payload.put("after", event.after());

            String jsonDetails = objectMapper.writeValueAsString(payload);
            log.setDetails(jsonDetails);

            auditLogRepository.save(log);

            // Mark request context to prevent duplicate Tier-1 logging
            markCurrentRequestAudited();
        } catch (Exception e) {
            logger.error("Failed to persist domain audit log for action {}: {}", event.action(), e.getMessage(), e);
        }
    }

    private void markCurrentRequestAudited() {
        try {
            var attributes = RequestContextHolder.getRequestAttributes();
            if (attributes instanceof ServletRequestAttributes servletAttrs) {
                HttpServletRequest req = servletAttrs.getRequest();
                req.setAttribute("DOMAIN_AUDIT_LOGGED", Boolean.TRUE);
            }
        } catch (Exception e) {
            // Ignore if invoked outside of a servlet request context (e.g. async/background jobs)
        }
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
        return getAuditLogs(userId, httpMethod, statusCode, from, to, search, null, null, pageable);
    }

    @Transactional(readOnly = true)
    public Page<AuditLogResponse> getAuditLogs(
            Long userId,
            String httpMethod,
            Integer statusCode,
            LocalDateTime from,
            LocalDateTime to,
            String search,
            String module,
            String role,
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
        String targetModule = (module != null && !module.isBlank() && !"ALL".equalsIgnoreCase(module.trim())) ? module.trim().toUpperCase() : null;
        String targetRole = (role != null && !role.isBlank() && !"ALL".equalsIgnoreCase(role.trim())) ? role.trim().toUpperCase() : null;

        Page<AuditLog> page = auditLogRepository.findAll((root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            
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
            if (targetModule != null) {
                var moduleJson = cb.like(cb.lower(root.get("details")), "%\"module\":\"" + targetModule.toLowerCase() + "\"%");
                var entityList = MODULE_ENTITY_MAP.get(targetModule);
                var entityPredicate = (entityList != null && !entityList.isEmpty())
                        ? root.get("entityName").in(entityList)
                        : cb.disjunction();

                Predicate pathPredicate = switch (targetModule) {
                    case "ADMISSIONS" -> cb.like(cb.lower(root.get("requestPath")), "%/admissions%");
                    case "MEDICAL" -> cb.or(
                            cb.like(cb.lower(root.get("requestPath")), "%/care-schedules%"),
                            cb.like(cb.lower(root.get("requestPath")), "%/health%"),
                            cb.like(cb.lower(root.get("requestPath")), "%/preventive%"),
                            cb.like(cb.lower(root.get("requestPath")), "%/treatment%"),
                            cb.like(cb.lower(root.get("requestPath")), "%/incidents%"),
                            cb.like(cb.lower(root.get("requestPath")), "%/vet/%")
                    );
                    case "STABLE" -> cb.or(
                            cb.like(cb.lower(root.get("requestPath")), "%/stalls%"),
                            cb.like(cb.lower(root.get("requestPath")), "%/areas%"),
                            cb.like(cb.lower(root.get("requestPath")), "%/groom-daily-tasks%")
                    );
                    case "HORSES" -> cb.like(cb.lower(root.get("requestPath")), "%/horses%");
                    case "STAFF" -> cb.or(
                            cb.like(cb.lower(root.get("requestPath")), "%/staff%"),
                            cb.like(cb.lower(root.get("requestPath")), "%/users%"),
                            cb.like(cb.lower(root.get("requestPath")), "%/accounts%")
                    );
                    case "TRAINING" -> cb.or(
                            cb.like(cb.lower(root.get("requestPath")), "%/training%"),
                            cb.like(cb.lower(root.get("requestPath")), "%/courses%"),
                            cb.like(cb.lower(root.get("requestPath")), "%/subjects%"),
                            cb.like(cb.lower(root.get("requestPath")), "%/lots%"),
                            cb.like(cb.lower(root.get("requestPath")), "%/workouts%")
                    );
                    case "RACING" -> cb.like(cb.lower(root.get("requestPath")), "%/race-registrations%");
                    default -> cb.disjunction();
                };

                predicates.add(cb.or(moduleJson, entityPredicate, pathPredicate));
            }
            if (targetRole != null) {
                var userJoin = root.join("user", JoinType.LEFT);
                var roleJoin = userJoin.join("role", JoinType.LEFT);
                predicates.add(cb.equal(cb.upper(roleJoin.get("name")), targetRole));
            }
            if (searchTerm != null) {
                var pathLike = cb.like(cb.lower(root.get("requestPath")), "%" + searchTerm + "%");
                var actionLike = cb.like(cb.lower(root.get("action")), "%" + searchTerm + "%");
                var detailsLike = cb.like(cb.lower(root.get("details")), "%" + searchTerm + "%");
                var entityLike = cb.like(cb.lower(root.get("entityName")), "%" + searchTerm + "%");
                var userJoin = root.join("user", JoinType.LEFT);
                var userNameLike = cb.like(cb.lower(userJoin.get("fullName")), "%" + searchTerm + "%");
                predicates.add(cb.or(pathLike, actionLike, detailsLike, entityLike, userNameLike));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
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
            dto.setModule(AuditLogResponse.resolveModule(dto.getEntityName(), dto.getDetails(), log.getRequestPath(), dto.getAction()));
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

