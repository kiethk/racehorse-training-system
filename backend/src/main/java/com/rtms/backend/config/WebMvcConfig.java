package com.rtms.backend.config;
import com.rtms.backend.service.AuditLogService;
import com.rtms.backend.interceptor.HttpAuditInterceptor;
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
