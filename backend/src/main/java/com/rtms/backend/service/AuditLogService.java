package com.rtms.backend.service;

import com.rtms.backend.entity.AuditLog;
import com.rtms.backend.entity.User;
import com.rtms.backend.repository.AuditLogRepository;
import com.rtms.backend.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditLogService {

    private static final Logger logger = LoggerFactory.getLogger(AuditLogService.class);

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
}
