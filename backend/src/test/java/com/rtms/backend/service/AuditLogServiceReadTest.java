package com.rtms.backend.service;

import com.rtms.backend.config.ApiException;
import com.rtms.backend.dto.AuditLogResponse;
import com.rtms.backend.entity.AuditLog;
import com.rtms.backend.entity.Role;
import com.rtms.backend.entity.User;
import com.rtms.backend.repository.AuditLogRepository;
import com.rtms.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AuditLogServiceReadTest {

    private AuditLogRepository repository;
    private UserRepository userRepository;
    private AuditLogService service;

    @BeforeEach
    void setUp() {
        repository = mock(AuditLogRepository.class);
        userRepository = mock(UserRepository.class);
        service = new AuditLogService(repository, userRepository);
    }

    private AuditLog makeLog(Long id, User user, String method, String path, int status, LocalDateTime at) {
        AuditLog log = new AuditLog();
        log.setUser(user);
        log.setHttpMethod(method);
        log.setRequestPath(path);
        log.setStatusCode(status);
        return log;
    }

    private User makeUser(Long id, String name, String email, String roleName) {
        User user = new User();
        Role role = new Role();
        role.setName(roleName);
        user.setRole(role);
        return user;
    }

    // 1. Default request returns paginated logs
    @Test
    void defaultRequest_returnsPaginatedLogs() {
        AuditLog log = makeLog(1L, null, "POST", "/api/horses", 201, LocalDateTime.now());
        Page<AuditLog> page = new PageImpl<>(List.of(log));
        when(repository.findAll(any(org.springframework.data.jpa.domain.Specification.class), any(Pageable.class)))
                .thenReturn(page);

        Pageable pageable = PageRequest.of(0, 20);
        Page<AuditLogResponse> result = service.getAuditLogs(null, null, null, null, null, null, pageable);

        assertEquals(1, result.getTotalElements());
        verify(repository).findAll(any(org.springframework.data.jpa.domain.Specification.class), any(Pageable.class));
    }

    // 2. Sorted newest first — service forces createdAt DESC sort
    @Test
    void getAuditLogs_forcesSortByCreatedAtDesc() {
        when(repository.findAll(any(org.springframework.data.jpa.domain.Specification.class), any(Pageable.class)))
                .thenReturn(Page.empty());

        service.getAuditLogs(null, null, null, null, null, null, PageRequest.of(0, 20));

        verify(repository).findAll(
                (org.springframework.data.jpa.domain.Specification<AuditLog>) any(org.springframework.data.jpa.domain.Specification.class),
                argThat((Pageable p) -> p.getSort().getOrderFor("createdAt") != null
                        && p.getSort().getOrderFor("createdAt").isDescending())
        );
    }

    // 3. Filter by userId
    @Test
    void filterByUserId_passedToRepository() {
        when(repository.findAll(any(org.springframework.data.jpa.domain.Specification.class), any(Pageable.class)))
                .thenReturn(Page.empty());

        service.getAuditLogs(7L, null, null, null, null, null, PageRequest.of(0, 20));

        verify(repository).findAll(any(org.springframework.data.jpa.domain.Specification.class), any(Pageable.class));
    }

    // 4. Filter by httpMethod
    @Test
    void filterByHttpMethod_passedToRepository() {
        when(repository.findAll(any(org.springframework.data.jpa.domain.Specification.class), any(Pageable.class)))
                .thenReturn(Page.empty());

        service.getAuditLogs(null, "POST", null, null, null, null, PageRequest.of(0, 20));

        verify(repository).findAll(any(org.springframework.data.jpa.domain.Specification.class), any(Pageable.class));
    }

    // 5. Lowercase method input normalizes to uppercase
    @Test
    void filterByHttpMethod_lowercaseNormalized() {
        when(repository.findAll(any(org.springframework.data.jpa.domain.Specification.class), any(Pageable.class)))
                .thenReturn(Page.empty());

        service.getAuditLogs(null, "patch", null, null, null, null, PageRequest.of(0, 20));

        verify(repository).findAll(any(org.springframework.data.jpa.domain.Specification.class), any(Pageable.class));
    }

    // 6. Invalid method rejected with 400
    @Test
    void filterByInvalidHttpMethod_throws400() {
        ApiException ex = assertThrows(ApiException.class,
                () -> service.getAuditLogs(null, "GET", null, null, null, null, PageRequest.of(0, 20)));

        assertEquals(400, ex.getStatus().value());
    }

    // 7. Filter by statusCode
    @Test
    void filterByStatusCode_passedToRepository() {
        when(repository.findAll(any(org.springframework.data.jpa.domain.Specification.class), any(Pageable.class)))
                .thenReturn(Page.empty());

        service.getAuditLogs(null, null, 403, null, null, null, PageRequest.of(0, 20));

        verify(repository).findAll(any(org.springframework.data.jpa.domain.Specification.class), any(Pageable.class));
    }

    // 8. Filter by from
    @Test
    void filterByFrom_passedToRepository() {
        LocalDateTime from = LocalDateTime.now().minusDays(1);
        when(repository.findAll(any(org.springframework.data.jpa.domain.Specification.class), any(Pageable.class)))
                .thenReturn(Page.empty());

        service.getAuditLogs(null, null, null, from, null, null, PageRequest.of(0, 20));

        verify(repository).findAll(any(org.springframework.data.jpa.domain.Specification.class), any(Pageable.class));
    }

    // 9. Filter by to
    @Test
    void filterByTo_passedToRepository() {
        LocalDateTime to = LocalDateTime.now();
        when(repository.findAll(any(org.springframework.data.jpa.domain.Specification.class), any(Pageable.class)))
                .thenReturn(Page.empty());

        service.getAuditLogs(null, null, null, null, to, null, PageRequest.of(0, 20));

        verify(repository).findAll(any(org.springframework.data.jpa.domain.Specification.class), any(Pageable.class));
    }

    // 10. Search matches requestPath case-insensitively (search term passed to repo)
    @Test
    void filterBySearch_passedToRepository() {
        when(repository.findAll(any(org.springframework.data.jpa.domain.Specification.class), any(Pageable.class)))
                .thenReturn(Page.empty());

        service.getAuditLogs(null, null, null, null, null, "horses", PageRequest.of(0, 20));

        verify(repository).findAll(any(org.springframework.data.jpa.domain.Specification.class), any(Pageable.class));
    }

    // 11. Null audit user maps safely (no NPE)
    @Test
    void nullUser_mapsSafely() {
        AuditLog log = makeLog(1L, null, "DELETE", "/api/horses/1", 200, LocalDateTime.now());
        Page<AuditLog> page = new PageImpl<>(List.of(log));
        when(repository.findAll(any(org.springframework.data.jpa.domain.Specification.class), any(Pageable.class)))
                .thenReturn(page);

        Page<AuditLogResponse> result = service.getAuditLogs(null, null, null, null, null, null, PageRequest.of(0, 20));

        AuditLogResponse dto = result.getContent().get(0);
        assertNull(dto.getActorUserId());
        assertNull(dto.getActorName());
        assertNull(dto.getActorEmail());
        assertNull(dto.getActorRole());
    }

    // 12. Empty result returns empty page
    @Test
    void noMatch_returnsEmptyPage() {
        when(repository.findAll(any(org.springframework.data.jpa.domain.Specification.class), any(Pageable.class)))
                .thenReturn(Page.empty());

        Page<AuditLogResponse> result = service.getAuditLogs(null, null, null, null, null, null, PageRequest.of(0, 20));

        assertTrue(result.isEmpty());
    }
}
