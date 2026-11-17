package com.rtms.backend.ai.tool.handler.manager;

import com.rtms.backend.ai.tool.DynamicToolRegistryService;
import com.rtms.backend.ai.tool.handler.AiToolHandler;
import com.rtms.backend.ai.tool.handler.AiToolSupport;
import com.rtms.backend.identity.entity.User;
import com.rtms.backend.identity.repository.TrainerProfileRepository;
import com.rtms.backend.identity.repository.UserRepository;
import com.rtms.backend.identity.repository.VeterinarianProfileRepository;
import com.rtms.backend.security.AuthenticatedUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class GetSystemStaffOverviewTool implements AiToolHandler {

    private final UserRepository userRepository;
    private final TrainerProfileRepository trainerProfileRepository;
    private final VeterinarianProfileRepository veterinarianProfileRepository;
    private final AiToolSupport support;

    @Autowired
    public GetSystemStaffOverviewTool(@Autowired(required = false) UserRepository userRepository,
                                      @Autowired(required = false) TrainerProfileRepository trainerProfileRepository,
                                      @Autowired(required = false) VeterinarianProfileRepository veterinarianProfileRepository,
                                      AiToolSupport support) {
        this.userRepository = userRepository;
        this.trainerProfileRepository = trainerProfileRepository;
        this.veterinarianProfileRepository = veterinarianProfileRepository;
        this.support = support;
    }

    @Override
    public String getToolName() {
        return "getSystemStaffOverview";
    }

    @Override
    public Object execute(Map<String, Object> arguments, AuthenticatedUser currentUser) {
        if (userRepository == null) {
            Map<String, Object> emptyResponse = new LinkedHashMap<>();
            emptyResponse.put("totalStaff", 0);
            emptyResponse.put("roleBreakdown", Collections.emptyMap());
            emptyResponse.put("staffList", Collections.emptyList());
            return emptyResponse;
        }
        List<User> users = userRepository.findAll();

        String roleFilter = arguments.get("role") != null ? arguments.get("role").toString().trim() : null;
        Boolean isActiveFilter = null;
        if (arguments.get("isActive") != null) {
            Object val = arguments.get("isActive");
            if (val instanceof Boolean b) {
                isActiveFilter = b;
            } else {
                isActiveFilter = Boolean.parseBoolean(val.toString().trim());
            }
        }
        String keyword = arguments.get("keyword") != null ? arguments.get("keyword").toString().trim().toLowerCase() : null;

        List<Map<String, Object>> result = new ArrayList<>();
        for (User u : users) {
            String roleName = u.getRole() != null ? u.getRole().getName() : "";

            if (roleFilter != null && !roleFilter.isBlank()) {
                String normFilter = DynamicToolRegistryService.normalizeRole(roleFilter);
                String normUserRole = DynamicToolRegistryService.normalizeRole(roleName);
                if (!normUserRole.equalsIgnoreCase(normFilter) && !roleName.equalsIgnoreCase(roleFilter)) {
                    continue;
                }
            } else {
                if ("HORSE_OWNER".equalsIgnoreCase(roleName)) {
                    continue;
                }
            }

            if (isActiveFilter != null && u.isActive() != isActiveFilter) {
                continue;
            }

            if (keyword != null && !keyword.isBlank()) {
                boolean matchName = u.getFullName() != null && u.getFullName().toLowerCase().contains(keyword);
                boolean matchEmail = u.getEmail() != null && u.getEmail().toLowerCase().contains(keyword);
                boolean matchPhone = u.getPhone() != null && u.getPhone().contains(keyword);
                if (!matchName && !matchEmail && !matchPhone) {
                    continue;
                }
            }

            String roleTitle = switch (roleName.toUpperCase()) {
                case "HEAD_TRAINER", "TRAINER" -> "Head Trainer";
                case "VETERINARIAN", "VET" -> "Veterinarian";
                case "GROOM", "STABLE_STAFF" -> "Stable Staff (Groom)";
                case "CLUB_MANAGER", "ADMIN" -> "Club Manager (Admin)";
                case "HORSE_OWNER", "OWNER" -> "Horse Owner";
                default -> roleName;
            };

            String specialtyInfo = support.resolveStaffSpecialty(u, roleName);

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("fullName", u.getFullName());
            item.put("role", roleName);
            item.put("roleTitle", roleTitle);
            item.put("email", u.getEmail());
            item.put("phone", (u.getPhone() != null && !u.getPhone().isBlank()) ? u.getPhone() : "Not updated");
            item.put("address", (u.getAddress() != null && !u.getAddress().isBlank()) ? u.getAddress() : "Not updated");
            item.put("joinedDate", u.getCreatedAt() != null ? u.getCreatedAt().toString() : "N/A");
            item.put("activeStatus", u.isActive() ? "Active" : "Locked");

            if (trainerProfileRepository != null && ("HEAD_TRAINER".equalsIgnoreCase(roleName) || "TRAINER".equalsIgnoreCase(roleName))) {
                trainerProfileRepository.findById(u.getId()).ifPresent(tp -> {
                    item.put("certificationNumber", tp.getCertificationNumber() != null ? tp.getCertificationNumber() : "N/A");
                });
            }
            if (veterinarianProfileRepository != null && ("VETERINARIAN".equalsIgnoreCase(roleName) || "VET".equalsIgnoreCase(roleName))) {
                veterinarianProfileRepository.findById(u.getId()).ifPresent(vp -> {
                    item.put("licenseNumber", vp.getLicenseNumber() != null ? vp.getLicenseNumber() : "N/A");
                    item.put("specialization", vp.getSpecialization() != null ? vp.getSpecialization() : "Clinical Veterinary Medicine");
                });
            }
            item.put("specialtyInfo", specialtyInfo);
            item.put("actionUrl", "/manager/staff");
            result.add(item);
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("totalStaff", result.size());

        Map<String, Integer> roleBreakdown = new LinkedHashMap<>();
        for (Map<String, Object> item : result) {
            String staffRole = (String) item.get("role");
            roleBreakdown.merge(staffRole, 1, Integer::sum);
        }
        response.put("roleBreakdown", roleBreakdown);
        response.put("staffList", result);
        return response;
    }
}
