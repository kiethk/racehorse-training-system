package com.rtms.backend.controller;

import com.rtms.backend.service.CareScheduleService;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CareScheduleReadAuthorizationTest {
    @Configuration
    @EnableMethodSecurity
    static class Config {
        @Bean CareScheduleService service() { return mock(CareScheduleService.class); }
        @Bean CareScheduleController controller(CareScheduleService service) {
            return new CareScheduleController(service);
        }
    }

    @Test
    void ownerCannotReadStaffClinicalSchedules() {
        try (var context = new AnnotationConfigApplicationContext(Config.class)) {
            var controller = context.getBean(CareScheduleController.class);
            SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                    "owner", "", List.of(new SimpleGrantedAuthority("ROLE_OWNER"))));
            assertThrows(AccessDeniedException.class, () -> controller.getById(1L));
            assertThrows(AccessDeniedException.class,
                    () -> controller.list(null, null, null, null, null, PageRequest.of(0, 10)));
            verifyNoInteractions(context.getBean(CareScheduleService.class));
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    void staffWithVetViewPermissionCanReadSchedules() {
        try (var context = new AnnotationConfigApplicationContext(Config.class)) {
            var controller = context.getBean(CareScheduleController.class);
            var service = context.getBean(CareScheduleService.class);
            var page = PageRequest.of(0, 10);
            when(service.listSchedules(null, null, null, null, 2L, page)).thenReturn(Page.empty());
            SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                    "vet", "", List.of(new SimpleGrantedAuthority("VET_EXAM_VIEW"))));
            assertDoesNotThrow(() -> controller.getById(1L));
            assertDoesNotThrow(() -> controller.list(null, null, null, null, 2L, page));
            verify(service).getScheduleDetail(1L);
            verify(service).listSchedules(null, null, null, null, 2L, page);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}
