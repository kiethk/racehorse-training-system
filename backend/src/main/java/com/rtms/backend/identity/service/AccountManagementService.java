package com.rtms.backend.identity.service;

import com.rtms.backend.horse.entity.Horse;
import com.rtms.backend.horse.repository.HorseRepository;
import com.rtms.backend.identity.dto.*;
import com.rtms.backend.identity.entity.Role;
import com.rtms.backend.identity.entity.User;
import com.rtms.backend.identity.repository.GroomProfileRepository;
import com.rtms.backend.identity.repository.RoleRepository;
import com.rtms.backend.identity.repository.TrainerProfileRepository;
import com.rtms.backend.identity.repository.UserRepository;
import com.rtms.backend.identity.repository.VeterinarianProfileRepository;
import com.rtms.backend.stable.entity.Area;
import com.rtms.backend.stable.entity.StableStall;
import com.rtms.backend.stable.enums.AreaType;
import com.rtms.backend.stable.repository.AreaRepository;
import com.rtms.backend.stable.repository.StableStallRepository;
import com.rtms.backend.admission.enums.AdmissionStatus;
import com.rtms.backend.admission.repository.AdmissionApplicationRepository;
import com.rtms.backend.horse.enums.HorseStatus;
import com.rtms.backend.identity.entity.GroomProfile;
import com.rtms.backend.medical.enums.CareScheduleStatus;
import com.rtms.backend.medical.repository.CareScheduleRepository;
import com.rtms.backend.notification.service.EmailService;
import com.rtms.backend.notification.service.NotificationService;
import jakarta.validation.Validator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.stream.Collectors;
import java.nio.charset.StandardCharsets;

@Service
public class AccountManagementService {

    private static final List<String> STAFF_ROLES = Arrays.asList("GROOM", "VETERINARIAN", "HEAD_TRAINER");

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final HorseRepository horseRepository;
    private final PasswordEncoder passwordEncoder;
    private final StaffManagementService staffManagementService;
    private final GroomProfileRepository groomProfileRepository;
    private final VeterinarianProfileRepository vetProfileRepository;
    private final TrainerProfileRepository trainerProfileRepository;
    private final Validator validator;
    private final AreaRepository areaRepository;
    private final StableStallRepository stableStallRepository;
    private final CareScheduleRepository careScheduleRepository;
    private final AdmissionApplicationRepository admissionApplicationRepository;
    private final NotificationService notificationService;
    private final EmailService emailService;

    public AccountManagementService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            HorseRepository horseRepository,
            PasswordEncoder passwordEncoder,
            StaffManagementService staffManagementService,
            GroomProfileRepository groomProfileRepository,
            VeterinarianProfileRepository vetProfileRepository,
            TrainerProfileRepository trainerProfileRepository,
            Validator validator) {
        this(userRepository, roleRepository, horseRepository, passwordEncoder, staffManagementService,
                groomProfileRepository, vetProfileRepository, trainerProfileRepository, validator, null, null, null, null, null, null);
    }

    public AccountManagementService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            HorseRepository horseRepository,
            PasswordEncoder passwordEncoder,
            StaffManagementService staffManagementService,
            GroomProfileRepository groomProfileRepository,
            VeterinarianProfileRepository vetProfileRepository,
            TrainerProfileRepository trainerProfileRepository,
            Validator validator,
            AreaRepository areaRepository,
            StableStallRepository stableStallRepository) {
        this(userRepository, roleRepository, horseRepository, passwordEncoder, staffManagementService,
                groomProfileRepository, vetProfileRepository, trainerProfileRepository, validator, areaRepository, stableStallRepository, null, null, null, null);
    }

    public AccountManagementService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            HorseRepository horseRepository,
            PasswordEncoder passwordEncoder,
            StaffManagementService staffManagementService,
            GroomProfileRepository groomProfileRepository,
            VeterinarianProfileRepository vetProfileRepository,
            TrainerProfileRepository trainerProfileRepository,
            Validator validator,
            AreaRepository areaRepository,
            StableStallRepository stableStallRepository,
            CareScheduleRepository careScheduleRepository,
            AdmissionApplicationRepository admissionApplicationRepository,
            NotificationService notificationService) {
        this(userRepository, roleRepository, horseRepository, passwordEncoder, staffManagementService,
                groomProfileRepository, vetProfileRepository, trainerProfileRepository, validator, areaRepository, stableStallRepository,
                careScheduleRepository, admissionApplicationRepository, notificationService, null);
    }

    @Autowired
    public AccountManagementService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            HorseRepository horseRepository,
            PasswordEncoder passwordEncoder,
            StaffManagementService staffManagementService,
            GroomProfileRepository groomProfileRepository,
            VeterinarianProfileRepository vetProfileRepository,
            TrainerProfileRepository trainerProfileRepository,
            Validator validator,
            AreaRepository areaRepository,
            StableStallRepository stableStallRepository,
            CareScheduleRepository careScheduleRepository,
            AdmissionApplicationRepository admissionApplicationRepository,
            NotificationService notificationService,
            @Autowired(required = false) EmailService emailService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.horseRepository = horseRepository;
        this.passwordEncoder = passwordEncoder;
        this.staffManagementService = staffManagementService;
        this.groomProfileRepository = groomProfileRepository;
        this.vetProfileRepository = vetProfileRepository;
        this.trainerProfileRepository = trainerProfileRepository;
        this.validator = validator;
        this.areaRepository = areaRepository;
        this.stableStallRepository = stableStallRepository;
        this.careScheduleRepository = careScheduleRepository;
        this.admissionApplicationRepository = admissionApplicationRepository;
        this.notificationService = notificationService;
        this.emailService = emailService;
    }

    // ── READ ALL ─────────────────────────────────────────────────────────────

    public List<AccountSummaryResponse> getAllAccounts() {
        // Query grouped horse counts once to completely eliminate N+1 queries
        Map<Long, Long> horseCountsByOwner = new HashMap<>();
        if (horseRepository != null) {
            List<Object[]> groupedCounts = horseRepository.countHorsesGroupedByOwner();
            if (groupedCounts != null) {
                for (Object[] row : groupedCounts) {
                    if (row.length >= 2 && row[0] != null && row[1] != null) {
                        Long ownerId = ((Number) row[0]).longValue();
                        Long count = ((Number) row[1]).longValue();
                        horseCountsByOwner.put(ownerId, count);
                    }
                }
            }
        }

        // Query areas and stalls in batch to eliminate N+1 queries
        Map<Long, Area> areaById = new HashMap<>();
        Map<Long, List<String>> managedAreasByTrainer = new HashMap<>();
        if (areaRepository != null) {
            List<Area> allAreas = areaRepository.findAll();
            for (Area area : allAreas) {
                areaById.put(area.getId(), area);
                if (area.getTrainerId() != null) {
                    managedAreasByTrainer.computeIfAbsent(area.getTrainerId(), k -> new ArrayList<>()).add("Area " + area.getCode());
                }
            }
        }

        Map<Long, List<StableStall>> stallsByGroom = new HashMap<>();
        if (stableStallRepository != null) {
            List<StableStall> allStalls = stableStallRepository.findAll();
            for (StableStall stall : allStalls) {
                if (stall.getGroomId() != null) {
                    stallsByGroom.computeIfAbsent(stall.getGroomId(), k -> new ArrayList<>()).add(stall);
                }
            }
        }

        List<User> allUsers = userRepository.findAll().stream()
                .sorted(Comparator.comparing(
                        User::getCreatedAt,
                        Comparator.nullsLast(Comparator.naturalOrder())
                ).reversed().thenComparing(Comparator.comparing(User::getId).reversed()))
                .toList();

        Map<Long, String> userNameById = allUsers.stream()
                .collect(Collectors.toMap(User::getId, User::getFullName, (existing, replacement) -> existing));

        List<AccountSummaryResponse> result = new ArrayList<>(allUsers.size());

        for (User user : allUsers) {
            if (user.getRole() == null) continue;
            AccountSummaryResponse summary = mapToAccountSummary(
                    user, horseCountsByOwner, areaById, managedAreasByTrainer, stallsByGroom, userNameById
            );
            result.add(summary);
        }

        return result;
    }

    // ── READ DETAIL ──────────────────────────────────────────────────────────

    public AccountDetailResponse getAccountDetail(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (user.getRole() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User has no assigned role");
        }

        String roleName = user.getRole().getName();
        AccountDetailResponse response = new AccountDetailResponse();
        response.setId(user.getId());
        response.setFullName(user.getFullName());
        response.setEmail(user.getEmail());
        response.setPhone(user.getPhone());
        response.setAddress(user.getAddress());
        response.setRole(roleName);
        response.setActive(user.isActive());
        response.setCreatedAt(user.getCreatedAt());

        if (STAFF_ROLES.contains(roleName)) {
            StaffDetailResponse staffDetail = staffManagementService.getStaffDetail(userId);
            response.setProfile(staffDetail.getProfile());

            if ("GROOM".equals(roleName)) {
                if (stableStallRepository != null) {
                    List<StableStall> stalls = stableStallRepository.findByGroomId(userId);
                    if (!stalls.isEmpty()) {
                        response.setAssignmentStatus("ASSIGNED");
                        List<String> codes = stalls.stream().map(StableStall::getStallCode).sorted().toList();
                        response.setAssignedStallCodes(codes);
                        Long areaId = stalls.get(0).getAreaId();
                        if (areaRepository != null && areaId != null) {
                            areaRepository.findById(areaId).ifPresent(area -> {
                                response.setAssignedAreaId(area.getId());
                                response.setAssignedAreaCode(area.getCode());
                                response.setAssignedAreaName(area.getName());
                                response.setAssignedAreaType(area.getType().name());
                                if (area.getType() == AreaType.REGULAR && area.getTrainerId() != null) {
                                    response.setTrainerId(area.getTrainerId());
                                    userRepository.findById(area.getTrainerId())
                                            .ifPresent(t -> response.setTrainerName(t.getFullName()));
                                }
                            });
                        }
                    } else {
                        response.setAssignmentStatus("UNASSIGNED");
                    }
                }
            } else if ("HEAD_TRAINER".equals(roleName)) {
                if (areaRepository != null) {
                    List<Area> regularAreas = areaRepository.findByTypeAndTrainerId(AreaType.REGULAR, userId);
                    response.setManagedAreaCodes(regularAreas.stream().map(Area::getCode).sorted().toList());
                }
            }
        } else if ("HORSE_OWNER".equals(roleName)) {
            List<Horse> ownedHorses = horseRepository.findByOwnerId(userId).stream()
                    .sorted(Comparator.comparing(
                            Horse::getCreatedAt,
                            Comparator.nullsLast(Comparator.naturalOrder())
                    ).reversed().thenComparing(Comparator.comparing(Horse::getId).reversed()))
                    .toList();

            Map<Long, String> stallCodeById = Map.of();
            if (stableStallRepository != null) {
                List<Long> stallIds = ownedHorses.stream()
                        .map(Horse::getCurrentStallId)
                        .filter(Objects::nonNull)
                        .distinct()
                        .toList();
                if (!stallIds.isEmpty()) {
                    stallCodeById = stableStallRepository.findAllById(stallIds).stream()
                            .filter(s -> s.getStallCode() != null)
                            .collect(Collectors.toMap(StableStall::getId, StableStall::getStallCode, (a, b) -> a));
                }
            }

            final Map<Long, String> finalStallMap = stallCodeById;
            List<AccountDetailResponse.OwnedHorseSummary> horseSummaries = ownedHorses.stream()
                    .map(h -> {
                        String stallLocation = null;
                        if (h.getCurrentStallId() != null) {
                            stallLocation = finalStallMap.get(h.getCurrentStallId());
                        }
                        if (stallLocation == null || stallLocation.isBlank()) {
                            stallLocation = h.getStableLocation();
                        }
                        return new AccountDetailResponse.OwnedHorseSummary(
                                h.getId(),
                                h.getName(),
                                h.getRegistrationNumber(),
                                h.getCurrentStatus() != null ? h.getCurrentStatus().name() : null,
                                stallLocation
                        );
                    })
                    .collect(Collectors.toList());
            response.setOwnedHorses(horseSummaries);
        }

        return response;
    }

    // ── CREATE ───────────────────────────────────────────────────────────────

    @Transactional
    public AccountCreationResponse createAccount(AccountCreationRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Account details are required");
        }

        if (request.getEmail() != null) {
            request.setEmail(request.getEmail().trim().toLowerCase(Locale.ROOT));
        }

        var violations = validator.validate(request);
        if (!violations.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    violations.stream().sorted(Comparator.comparing(v -> v.getPropertyPath().toString())).findFirst().orElseThrow().getMessage());
        }

        if (request.getPassword().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password must be at most 72 UTF-8 bytes");
        }

        String normalizedEmail = request.getEmail();
        if (userRepository.findByEmail(normalizedEmail).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already in use");
        }

        String roleName = request.getRole();
        if (STAFF_ROLES.contains(roleName)) {
            StaffCreationResponse staffResponse = staffManagementService.createStaff(request.toStaffCreationRequest());
            if (emailService != null) {
                emailService.sendStaffWelcomeEmail(staffResponse.getEmail(), staffResponse.getFullName(), staffResponse.getRole(), request.getPassword());
            }
            return copyToAccountCreationResponse(staffResponse);
        }

        // Handle HORSE_OWNER or CLUB_MANAGER
        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Role not found: " + roleName));

        User user = new User();
        user.setFullName(request.getFullName().trim());
        user.setEmail(normalizedEmail);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setPhone(clean(request.getPhone()));
        user.setAddress(clean(request.getAddress()));
        user.setRole(role);
        user.setActive(true);
        user = userRepository.save(user);

        if (emailService != null) {
            if ("HORSE_OWNER".equals(roleName)) {
                emailService.sendOwnerWelcomeEmail(user.getEmail(), user.getFullName());
            } else {
                emailService.sendStaffWelcomeEmail(user.getEmail(), user.getFullName(), role.getName(), request.getPassword());
            }
        }

        AccountCreationResponse response = new AccountCreationResponse();
        response.setUserId(user.getId());
        response.setFullName(user.getFullName());
        response.setEmail(user.getEmail());
        response.setPhone(user.getPhone());
        response.setAddress(user.getAddress());
        response.setRole(role.getName());
        response.setActive(user.isActive());
        response.setCreatedAt(user.getCreatedAt());
        response.setAssignmentStatus("ASSIGNED");
        response.setProfileSummary("HORSE_OWNER".equals(roleName) ? "Horse Owner Account" : "Club Administrator Account");

        return response;
    }

    // ── UPDATE ───────────────────────────────────────────────────────────────

    @Transactional
    public AccountDetailResponse updateAccount(Long userId, StaffUpdateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (user.getRole() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User has no role");
        }

        String roleName = user.getRole().getName();
        if (STAFF_ROLES.contains(roleName)) {
            staffManagementService.updateStaff(userId, request);
            return getAccountDetail(userId);
        }

        // Owner or Club Manager update
        if (request.getFullName() != null && !request.getFullName().isBlank()) {
            user.setFullName(request.getFullName().trim());
        }
        user.setPhone(clean(request.getPhone()));
        user.setAddress(clean(request.getAddress()));
        userRepository.save(user);

        return getAccountDetail(userId);
    }

    // ── STATUS TOGGLE (WITH ROLE-SPECIFIC DEACTIVATION PRECHECKS) ─────────────

    @Transactional
    public AccountSummaryResponse updateAccountStatus(Long currentUserId, Long targetUserId, boolean active) {
        // Enforce self-lockout protection: An administrator cannot deactivate their own account!
        if (currentUserId != null && currentUserId.equals(targetUserId) && !active) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You cannot deactivate your own administrator account.");
        }

        User user = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (!active && user.isActive()) {
            validateDeactivationPrerequisites(user, currentUserId);
        }

        user.setActive(active);
        user = userRepository.save(user);

        if (!active && user.getRole() != null && "CLUB_MANAGER".equals(user.getRole().getName())) {
            notifyManagersOfDeactivation(user, currentUserId);
        }

        // Fetch single owner count if owner
        Map<Long, Long> singleCountMap = new HashMap<>();
        if (user.getRole() != null && "HORSE_OWNER".equals(user.getRole().getName())) {
            long count = horseRepository.findByOwnerId(user.getId()).size();
            singleCountMap.put(user.getId(), count);
        }

        return mapToAccountSummary(user, singleCountMap);
    }

    private void validateDeactivationPrerequisites(User user, Long currentUserId) {
        Long targetUserId = user.getId();
        String roleName = user.getRole() != null ? user.getRole().getName() : "";

        // 1. Self-lockout check
        if (currentUserId != null && currentUserId.equals(targetUserId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You cannot deactivate your own administrator account.");
        }

        switch (roleName) {
            case "CLUB_MANAGER": {
                long activeManagers = userRepository.countActiveClubManagers();
                if (activeManagers <= 1) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT,
                            "Cannot deactivate the last active Club Manager in the system.");
                }
                break;
            }
            case "GROOM": {
                if (stableStallRepository != null) {
                    List<StableStall> assignedStalls = stableStallRepository.findByGroomId(targetUserId);
                    if (!assignedStalls.isEmpty()) {
                        String stallCodes = assignedStalls.stream()
                                .map(StableStall::getStallCode)
                                .sorted()
                                .collect(Collectors.joining(", "));
                        throw new ResponseStatusException(HttpStatus.CONFLICT,
                                "Cannot deactivate Groom while they are assigned to stalls: [" + stallCodes + "]. Please unassign or reassign these stalls first.");
                    }
                }
                break;
            }
            case "HEAD_TRAINER": {
                if (areaRepository != null) {
                    List<Area> regularAreas = areaRepository.findByTypeAndTrainerId(AreaType.REGULAR, targetUserId);
                    if (!regularAreas.isEmpty()) {
                        String areaCodes = regularAreas.stream()
                                .map(Area::getCode)
                                .sorted()
                                .collect(Collectors.joining(", "));
                        throw new ResponseStatusException(HttpStatus.CONFLICT,
                                "Cannot deactivate Head Trainer while they are assigned to areas: [" + areaCodes + "]. Please reassign or unassign these areas first.");
                    }
                }
                // When trainer manages no regular areas, auto-unassign subordinate reporting grooms
                if (groomProfileRepository != null) {
                    List<GroomProfile> reportingGrooms = groomProfileRepository.findByTrainerId(targetUserId);
                    for (GroomProfile groom : reportingGrooms) {
                        groom.setTrainerId(null);
                        groomProfileRepository.save(groom);
                    }
                }
                break;
            }
            case "HORSE_OWNER": {
                if (admissionApplicationRepository != null) {
                    boolean hasPendingAdmissions = admissionApplicationRepository.existsByOwnerIdAndStatusNotIn(
                            targetUserId,
                            List.of(AdmissionStatus.APPROVED, AdmissionStatus.REJECTED)
                    );
                    if (hasPendingAdmissions) {
                        throw new ResponseStatusException(HttpStatus.CONFLICT,
                                "Cannot deactivate Horse Owner while they have pending admission applications in progress. Please approve, reject, or cancel these applications first.");
                    }
                }

                if (horseRepository != null) {
                    List<Horse> horses = horseRepository.findByOwnerId(targetUserId);
                    List<String> activeHorses = horses.stream()
                            .filter(h -> h.getCurrentStallId() != null || h.getCurrentStatus() == HorseStatus.ELIGIBLE)
                            .map(Horse::getName)
                            .distinct()
                            .sorted()
                            .toList();

                    if (!activeHorses.isEmpty()) {
                        throw new ResponseStatusException(HttpStatus.CONFLICT,
                                "Cannot deactivate Horse Owner while they have horses active in the facility: [" +
                                        String.join(", ", activeHorses) + "]. Please discharge or transfer these horses first.");
                    }
                }
                break;
            }
            case "VETERINARIAN": {
                if (careScheduleRepository != null) {
                    boolean hasPendingSchedules = careScheduleRepository.existsByVeterinarianIdAndStatusIn(
                            targetUserId,
                            List.of(CareScheduleStatus.REQUESTED, CareScheduleStatus.SCHEDULED, CareScheduleStatus.IN_PROGRESS)
                    );
                    if (hasPendingSchedules) {
                        throw new ResponseStatusException(HttpStatus.CONFLICT,
                                "Cannot deactivate Veterinarian while they have pending or in-progress care schedules. Please reassign their schedules before deactivating.");
                    }
                }
                break;
            }
        }
    }

    private void notifyManagersOfDeactivation(User deactivatedUser, Long currentUserId) {
        if (notificationService == null) return;
        String actorName = "A Club Manager";
        if (currentUserId != null) {
            actorName = userRepository.findById(currentUserId)
                    .map(User::getFullName)
                    .orElse("A Club Manager");
        }

        List<User> activeManagers = userRepository.findActiveClubManagers();
        for (User manager : activeManagers) {
            if (manager.getId().equals(deactivatedUser.getId())) {
                continue;
            }
            notificationService.createNotification(
                    manager.getId(),
                    "Manager Account Deactivated",
                    "Manager account " + deactivatedUser.getFullName() + " was deactivated by " + actorName + ".",
                    "SECURITY_ALERT",
                    "USER",
                    deactivatedUser.getId(),
                    "manager-deactivation-" + deactivatedUser.getId() + "-" + System.currentTimeMillis()
            );
        }
    }

    // ── PRIVATE MAPPING HELPERS ──────────────────────────────────────────────

    private static String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private AccountSummaryResponse mapToAccountSummary(User user, Map<Long, Long> horseCountsByOwner) {
        return mapToAccountSummary(user, horseCountsByOwner, Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap());
    }

    private AccountSummaryResponse mapToAccountSummary(
            User user,
            Map<Long, Long> horseCountsByOwner,
            Map<Long, Area> areaById,
            Map<Long, List<String>> managedAreasByTrainer,
            Map<Long, List<StableStall>> stallsByGroom,
            Map<Long, String> userNameById) {
        AccountSummaryResponse summary = new AccountSummaryResponse();
        summary.setUserId(user.getId());
        summary.setFullName(user.getFullName());
        summary.setEmail(user.getEmail());
        summary.setPhone(user.getPhone());
        summary.setAddress(user.getAddress());
        summary.setRole(user.getRole() != null ? user.getRole().getName() : null);
        summary.setActive(user.isActive());
        summary.setCreatedAt(user.getCreatedAt());

        if (summary.getRole() != null) {
            switch (summary.getRole()) {
                case "GROOM": {
                    List<StableStall> stalls = stallsByGroom.getOrDefault(user.getId(), Collections.emptyList());
                    if (!stalls.isEmpty()) {
                        List<String> stallCodes = stalls.stream()
                                .map(StableStall::getStallCode)
                                .sorted()
                                .toList();
                        summary.setAssignmentStatus("ASSIGNED");
                        summary.setAssignedStallCodes(stallCodes);

                        Area area = areaById.get(stalls.get(0).getAreaId());
                        if (area != null) {
                            summary.setAssignedAreaCode(area.getCode());
                            summary.setAssignedAreaType(area.getType().name());
                            if (area.getType() == AreaType.QUARANTINE) {
                                summary.setProfileSummary("Quarantine: " + String.join(", ", stallCodes));
                            } else {
                                String trainerName = area.getTrainerId() != null ? userNameById.get(area.getTrainerId()) : null;
                                summary.setTrainerName(trainerName);
                                summary.setProfileSummary("Stalls: " + String.join(", ", stallCodes) +
                                        (trainerName != null ? " · Trainer: " + trainerName : " · No Trainer"));
                            }
                        } else {
                            summary.setProfileSummary("Stalls: " + String.join(", ", stallCodes));
                        }
                    } else {
                        summary.setAssignmentStatus("UNASSIGNED");
                        summary.setProfileSummary("Unassigned (No Stalls)");
                    }
                    break;
                }
                case "HEAD_TRAINER": {
                    List<String> areas = managedAreasByTrainer.getOrDefault(user.getId(), Collections.emptyList());
                    if (!areas.isEmpty()) {
                        summary.setManagedAreaCodes(areas);
                        summary.setProfileSummary("Managed: " + String.join(", ", areas));
                    } else {
                        summary.setProfileSummary("Unassigned (No Areas)");
                    }
                    break;
                }
                case "VETERINARIAN": {
                    vetProfileRepository.findById(user.getId()).ifPresent(p -> {
                        String spec = p.getSpecialization();
                        if (spec != null && !spec.isBlank()) {
                            summary.setProfileSummary(spec + " · Lic: " + p.getLicenseNumber());
                        } else {
                            summary.setProfileSummary("License: " + p.getLicenseNumber());
                        }
                    });
                    break;
                }
                case "HORSE_OWNER": {
                    long owned = horseCountsByOwner.getOrDefault(user.getId(), 0L);
                    summary.setOwnedHorsesCount(owned);
                    summary.setProfileSummary(owned == 1 ? "1 horse owned" : owned + " horses owned");
                    break;
                }
                case "CLUB_MANAGER": {
                    summary.setProfileSummary("Club Administrator");
                    break;
                }
            }
        }

        return summary;
    }

    private AccountCreationResponse copyToAccountCreationResponse(StaffCreationResponse staffResponse) {
        AccountCreationResponse r = new AccountCreationResponse();
        r.setUserId(staffResponse.getUserId());
        r.setFullName(staffResponse.getFullName());
        r.setEmail(staffResponse.getEmail());
        r.setPhone(staffResponse.getPhone());
        r.setAddress(staffResponse.getAddress());
        r.setRole(staffResponse.getRole());
        r.setActive(staffResponse.isActive());
        r.setCreatedAt(staffResponse.getCreatedAt());
        r.setProfileSummary(staffResponse.getProfileSummary());
        r.setAssignmentStatus(staffResponse.getAssignmentStatus());
        r.setAssignedAreaType(staffResponse.getAssignedAreaType());
        r.setAssignedAreaIds(staffResponse.getAssignedAreaIds());
        r.setAssignedAreaCodes(staffResponse.getAssignedAreaCodes());
        r.setTrainerId(staffResponse.getTrainerId());
        r.setTrainerName(staffResponse.getTrainerName());
        r.setAssignedAreaId(staffResponse.getAssignedAreaId());
        r.setAssignedAreaCode(staffResponse.getAssignedAreaCode());
        r.setAssignedStallIds(staffResponse.getAssignedStallIds());
        r.setAssignedStallCodes(staffResponse.getAssignedStallCodes());
        r.setNoStallBlockAvailable(staffResponse.getNoStallBlockAvailable());
        return r;
    }
}
