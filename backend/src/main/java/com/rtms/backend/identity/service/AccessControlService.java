package com.rtms.backend.identity.service;

import com.rtms.backend.config.ApiException;
import com.rtms.backend.identity.dto.AccessControlMatrixResponse;
import com.rtms.backend.identity.dto.RolePermissionUpdateRequest;
import com.rtms.backend.identity.entity.Permission;
import com.rtms.backend.identity.entity.Role;
import com.rtms.backend.identity.repository.PermissionRepository;
import com.rtms.backend.identity.repository.RoleRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
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

        Set<String> distinctCodes = new LinkedHashSet<>(request.getPermissionCodes());
        List<Permission> newPermissions = permissionRepository.findByCodeIn(new ArrayList<>(distinctCodes));
        
        if (newPermissions.size() != distinctCodes.size()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "UNKNOWN_PERMISSION", "One or more requested permission codes are invalid");
        }

        if ("CLUB_MANAGER".equals(role.getName())) {
            boolean hasUserManage = newPermissions.stream().anyMatch(p -> "USER_MANAGE".equals(p.getCode()));
            if (!hasUserManage) {
                throw new ApiException(HttpStatus.CONFLICT, "LOCKED_OUT", "CLUB_MANAGER must retain USER_MANAGE permission to prevent lock-out.");
            }
        } else {
            boolean hasUserManage = newPermissions.stream().anyMatch(p -> "USER_MANAGE".equals(p.getCode()));
            if (hasUserManage) {
                throw new ApiException(HttpStatus.FORBIDDEN, "PRIVILEGE_ESCALATION", "Only CLUB_MANAGER role is permitted to hold USER_MANAGE permission.");
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

    @Transactional
    public List<AccessControlMatrixResponse.RolePermissionSummary> updateBulkRolePermissions(
            List<com.rtms.backend.identity.dto.BulkRolePermissionUpdateRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "Requests list cannot be empty");
        }

        // Phase 1: Validate all requested roles and permissions before mutating ANY state
        Map<Role, Set<Permission>> plan = new LinkedHashMap<>();

        for (com.rtms.backend.identity.dto.BulkRolePermissionUpdateRequest req : requests) {
            if (req.getRoleId() == null) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "Role ID is required");
            }
            Role role = roleRepository.findById(req.getRoleId())
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "ROLE_NOT_FOUND", "Role not found: " + req.getRoleId()));

            if (req.getPermissionCodes() == null) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "Permission codes list cannot be null for role " + role.getName());
            }

            Set<String> distinctCodes = new LinkedHashSet<>(req.getPermissionCodes());
            List<Permission> permissions = distinctCodes.isEmpty()
                    ? Collections.emptyList()
                    : permissionRepository.findByCodeIn(new ArrayList<>(distinctCodes));

            if (permissions.size() != distinctCodes.size()) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "UNKNOWN_PERMISSION", "One or more requested permission codes are invalid for role " + role.getName());
            }

            if ("CLUB_MANAGER".equals(role.getName())) {
                boolean hasUserManage = permissions.stream().anyMatch(p -> "USER_MANAGE".equals(p.getCode()));
                if (!hasUserManage) {
                    throw new ApiException(HttpStatus.CONFLICT, "LOCKED_OUT", "CLUB_MANAGER must retain USER_MANAGE permission to prevent lock-out.");
                }
            } else {
                boolean hasUserManage = permissions.stream().anyMatch(p -> "USER_MANAGE".equals(p.getCode()));
                if (hasUserManage) {
                    throw new ApiException(HttpStatus.FORBIDDEN, "PRIVILEGE_ESCALATION", "Only CLUB_MANAGER role is permitted to hold USER_MANAGE permission. Role " + role.getName() + " cannot hold USER_MANAGE.");
                }
            }

            plan.put(role, new HashSet<>(permissions));
        }

        // Phase 2: Apply and save all together in one atomic transaction
        List<AccessControlMatrixResponse.RolePermissionSummary> summaries = new ArrayList<>();
        for (Map.Entry<Role, Set<Permission>> entry : plan.entrySet()) {
            Role role = entry.getKey();
            role.setPermissions(entry.getValue());
            role = roleRepository.save(role);
            summaries.add(new AccessControlMatrixResponse.RolePermissionSummary(
                    role.getId(),
                    role.getName(),
                    role.getPermissions().stream().map(Permission::getCode).collect(Collectors.toList())
            ));
        }

        return summaries;
    }
}
