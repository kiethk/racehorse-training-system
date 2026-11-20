package com.rtms.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashSet;
import java.util.Set;

@Configuration
@ConfigurationProperties(prefix = "app.security")
public class SuperAdminProperties {

    private Set<Long> superAdminIds = new HashSet<>(Set.of(1L));

    public Set<Long> getSuperAdminIds() {
        return superAdminIds;
    }

    public void setSuperAdminIds(Set<Long> superAdminIds) {
        this.superAdminIds = superAdminIds != null ? superAdminIds : new HashSet<>();
    }

    public boolean isSuperAdmin(Long userId) {
        return userId != null && superAdminIds != null && superAdminIds.contains(userId);
    }
}
