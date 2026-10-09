package com.rtms.backend.identity.service;

import com.rtms.backend.config.ApiException;
import com.rtms.backend.identity.dto.AccessControlMatrixResponse;
import com.rtms.backend.identity.dto.RolePermissionUpdateRequest;
import com.rtms.backend.identity.entity.Permission;
import com.rtms.backend.identity.entity.Role;
import com.rtms.backend.identity.repository.PermissionRepository;
import com.rtms.backend.identity.repository.RoleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AccessControlServiceTest {

    private RoleRepository roleRepository;
    private PermissionRepository permissionRepository;
    private AccessControlService accessControlService;

    @BeforeEach
    void setUp() {
        roleRepository = mock(RoleRepository.class);
        permissionRepository = mock(PermissionRepository.class);
        accessControlService = new AccessControlService(roleRepository, permissionRepository);
    }

    @Test
    void testGetAccessControlMatrix() {
        Role role = new Role();
        role.setId(1L);
        role.setName("CLUB_MANAGER");
        
        Permission perm = new Permission();
        perm.setId(1L);
        perm.setCode("USER_MANAGE");
        perm.setDescription("Manage users");
        
        role.setPermissions(Set.of(perm));

        when(roleRepository.findAll()).thenReturn(List.of(role));
        when(permissionRepository.findAll()).thenReturn(List.of(perm));

        AccessControlMatrixResponse response = accessControlService.getAccessControlMatrix();

        assertNotNull(response);
        assertEquals(1, response.getRoles().size());
        assertEquals("CLUB_MANAGER", response.getRoles().get(0).getRoleName());
        assertEquals(1, response.getRoles().get(0).getPermissionCodes().size());
        assertEquals("USER_MANAGE", response.getRoles().get(0).getPermissionCodes().get(0));

        assertEquals(1, response.getPermissions().size());
        assertEquals("USER_MANAGE", response.getPermissions().get(0).getCode());
    }

    @Test
    void testUpdateRolePermissions_Success() {
        Role role = new Role();
        role.setId(2L);
        role.setName("GROOM");
        
        Permission oldPerm = new Permission();
        oldPerm.setId(1L);
        oldPerm.setCode("OLD_PERM");
        role.setPermissions(new HashSet<>(Set.of(oldPerm)));

        Permission newPerm1 = new Permission();
        newPerm1.setId(2L);
        newPerm1.setCode("NEW_PERM_1");
        
        Permission newPerm2 = new Permission();
        newPerm2.setId(3L);
        newPerm2.setCode("NEW_PERM_2");

        when(roleRepository.findById(2L)).thenReturn(Optional.of(role));
        when(permissionRepository.findByCodeIn(List.of("NEW_PERM_1", "NEW_PERM_2")))
                .thenReturn(List.of(newPerm1, newPerm2));
        when(roleRepository.save(any(Role.class))).thenAnswer(i -> i.getArgument(0));

        RolePermissionUpdateRequest req = new RolePermissionUpdateRequest();
        req.setPermissionCodes(List.of("NEW_PERM_1", "NEW_PERM_2"));

        AccessControlMatrixResponse.RolePermissionSummary res = accessControlService.updateRolePermissions(2L, req);
        
        assertEquals(2, res.getPermissionCodes().size());
        assertTrue(res.getPermissionCodes().contains("NEW_PERM_1"));
        assertTrue(res.getPermissionCodes().contains("NEW_PERM_2"));
        assertFalse(res.getPermissionCodes().contains("OLD_PERM")); // replaced, not appended
        
        verify(roleRepository).save(role);
    }

    @Test
    void testUpdateRolePermissions_UnknownRoleRejected() {
        when(roleRepository.findById(99L)).thenReturn(Optional.empty());

        RolePermissionUpdateRequest req = new RolePermissionUpdateRequest();
        req.setPermissionCodes(List.of("ANY"));

        ApiException ex = assertThrows(ApiException.class, () -> accessControlService.updateRolePermissions(99L, req));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }

    @Test
    void testUpdateRolePermissions_UnknownPermissionRejected() {
        Role role = new Role();
        role.setId(2L);
        role.setName("GROOM");

        Permission newPerm1 = new Permission();
        newPerm1.setId(2L);
        newPerm1.setCode("KNOWN_PERM");

        when(roleRepository.findById(2L)).thenReturn(Optional.of(role));
        // Repo only returns 1 of the 2 requested
        when(permissionRepository.findByCodeIn(List.of("KNOWN_PERM", "UNKNOWN_PERM")))
                .thenReturn(List.of(newPerm1));

        RolePermissionUpdateRequest req = new RolePermissionUpdateRequest();
        req.setPermissionCodes(List.of("KNOWN_PERM", "UNKNOWN_PERM"));

        ApiException ex = assertThrows(ApiException.class, () -> accessControlService.updateRolePermissions(2L, req));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertTrue(ex.getMessage().contains("invalid"));
    }

    @Test
    void testUpdateRolePermissions_ClubManagerCannotLoseUserManage() {
        Role role = new Role();
        role.setId(1L);
        role.setName("CLUB_MANAGER");

        Permission otherPerm = new Permission();
        otherPerm.setId(3L);
        otherPerm.setCode("OTHER_PERM");

        when(roleRepository.findById(1L)).thenReturn(Optional.of(role));
        when(permissionRepository.findByCodeIn(List.of("OTHER_PERM")))
                .thenReturn(List.of(otherPerm));

        RolePermissionUpdateRequest req = new RolePermissionUpdateRequest();
        req.setPermissionCodes(List.of("OTHER_PERM")); // Missing USER_MANAGE

        ApiException ex = assertThrows(ApiException.class, () -> accessControlService.updateRolePermissions(1L, req));
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        assertTrue(ex.getMessage().contains("retain USER_MANAGE"));
    }

    @Test
    void testUpdateRolePermissions_EmptySetAllowedForNonManager() {
        Role role = new Role();
        role.setId(2L);
        role.setName("GROOM");
        
        when(roleRepository.findById(2L)).thenReturn(Optional.of(role));
        when(permissionRepository.findByCodeIn(List.of())).thenReturn(List.of());
        when(roleRepository.save(any(Role.class))).thenAnswer(i -> i.getArgument(0));

        RolePermissionUpdateRequest req = new RolePermissionUpdateRequest();
        req.setPermissionCodes(List.of());

        AccessControlMatrixResponse.RolePermissionSummary res = accessControlService.updateRolePermissions(2L, req);
        
        assertTrue(res.getPermissionCodes().isEmpty());
    }
}
