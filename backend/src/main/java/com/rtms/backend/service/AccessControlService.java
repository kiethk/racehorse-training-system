package com.rtms.backend.service;

import com.rtms.backend.config.ApiException;
import com.rtms.backend.dto.AccessControlMatrixResponse;
import com.rtms.backend.dto.RolePermissionUpdateRequest;
import com.rtms.backend.entity.Permission;
import com.rtms.backend.entity.Role;
import com.rtms.backend.repository.PermissionRepository;
import com.rtms.backend.repository.RoleRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AccessControlService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    public AccessControlService(RoleRepository roleRepository, PermissionRepository permissionRepository) {
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
    }

    public AccessControlMatrixResponse getAccessControlMatrix() {
        List<Role> roles = roleRepository.findAll();
        List<Permission> permissions = permissionRepository.findAll();

        List<AccessControlMatrixResponse.RolePermissionSummary> roleSummaries = roles.stream()
                .map(role -> new AccessControlMatrixResponse.RolePermissionSummary(
                        role.getId(),
                        role.getName(),
                        role.getPermissions().stream().map(Permission::getCode).collect(Collectors.toList())
                ))
                .collect(Collectors.toList());

        List<AccessControlMatrixResponse.PermissionSummary> permissionSummaries = permissions.stream()
                .map(p -> new AccessControlMatrixResponse.PermissionSummary(p.getId(), p.getCode(), p.getDescription()))
                .collect(Collectors.toList());

        return new AccessControlMatrixResponse(roleSummaries, permissionSummaries);
    }

    @Transactional
    public AccessControlMatrixResponse.RolePermissionSummary updateRolePermissions(Long roleId, RolePermissionUpdateRequest request) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "ROLE_NOT_FOUND", "Role not found"));

        if (request.getPermissionCodes() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "Permission codes list cannot be null");
        }

        List<Permission> newPermissions = permissionRepository.findByCodeIn(request.getPermissionCodes());
        
        if (newPermissions.size() != request.getPermissionCodes().size()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "UNKNOWN_PERMISSION", "One or more requested permission codes are invalid");
        }

        if ("CLUB_MANAGER".equals(role.getName())) {
            boolean hasUserManage = newPermissions.stream().anyMatch(p -> "USER_MANAGE".equals(p.getCode()));
            if (!hasUserManage) {
                throw new ApiException(HttpStatus.CONFLICT, "LOCKED_OUT", "CLUB_MANAGER must retain USER_MANAGE permission to prevent lock-out.");
            }
        }

        role.setPermissions(new HashSet<>(newPermissions));
        role = roleRepository.save(role);

        return new AccessControlMatrixResponse.RolePermissionSummary(
                role.getId(),
                role.getName(),
                role.getPermissions().stream().map(Permission::getCode).collect(Collectors.toList())
        );
    }
}
