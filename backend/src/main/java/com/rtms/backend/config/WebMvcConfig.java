package com.rtms.backend.config;

import com.rtms.backend.interceptor.HttpAuditInterceptor;
import com.rtms.backend.audit.service.AuditLogService;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final AuditLogService auditLogService;

    public WebMvcConfig(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new HttpAuditInterceptor(auditLogService));
    }
}
