package com.rtms.backend.service;

import com.rtms.backend.dto.StaffCreationRequest;
import com.rtms.backend.dto.StaffCreationResponse;
import com.rtms.backend.dto.StaffDetailResponse;
import com.rtms.backend.dto.StaffSummaryResponse;
import com.rtms.backend.dto.StaffUpdateRequest;
import com.rtms.backend.entity.*;
import com.rtms.backend.enums.AreaType;
import com.rtms.backend.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class StaffManagementService {

    /** Maximum REGULAR areas one Trainer may manage. */
    static final int MAX_AREAS_PER_TRAINER = 2;

    /** Block size: each Groom owns exactly one consecutive block of this many stalls. */
    static final int GROOM_BLOCK_SIZE = 3;

    /** Block starts for a 12-stall area. Blocks: 1-3, 4-6, 7-9, 10-12. */
    static final int[] BLOCK_STARTS = {1, 4, 7, 10};

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final GroomProfileRepository groomProfileRepository;
    private final VeterinarianProfileRepository vetProfileRepository;
    private final TrainerProfileRepository trainerProfileRepository;
    private final AreaRepository areaRepository;
    private final StableStallRepository stableStallRepository;

    private static final List<String> STAFF_ROLES = Arrays.asList("GROOM", "VETERINARIAN", "HEAD_TRAINER");

    public StaffManagementService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            GroomProfileRepository groomProfileRepository,
            VeterinarianProfileRepository vetProfileRepository,
            TrainerProfileRepository trainerProfileRepository,
            AreaRepository areaRepository,
            StableStallRepository stableStallRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.groomProfileRepository = groomProfileRepository;
        this.vetProfileRepository = vetProfileRepository;
        this.trainerProfileRepository = trainerProfileRepository;
        this.areaRepository = areaRepository;
        this.stableStallRepository = stableStallRepository;
    }

    // ── READ ──────────────────────────────────────────────────────────────────

    public List<StaffSummaryResponse> getAllStaff() {
        return userRepository.findAll().stream()
                .filter(u -> u.getRole() != null && STAFF_ROLES.contains(u.getRole().getName()))
                .map(this::mapToSummary)
                .collect(Collectors.toList());
    }

    public StaffDetailResponse getStaffDetail(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (user.getRole() == null || !STAFF_ROLES.contains(user.getRole().getName())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not a staff account");
        }

        StaffDetailResponse response = new StaffDetailResponse();
        response.setId(user.getId());
        response.setFullName(user.getFullName());
        response.setEmail(user.getEmail());
        response.setPhone(user.getPhone());
        response.setAddress(user.getAddress());
        response.setRole(user.getRole().getName());
        response.setActive(user.isActive());
        response.setCreatedAt(user.getCreatedAt());

        switch (user.getRole().getName()) {
            case "GROOM":
                groomProfileRepository.findById(user.getId()).ifPresent(p -> {
                    StaffDetailResponse.GroomProfileDto dto = new StaffDetailResponse.GroomProfileDto();
                    dto.setTrainerId(p.getTrainerId());
                    response.setProfile(dto);
                });
                break;
            case "VETERINARIAN":
                vetProfileRepository.findById(user.getId()).ifPresent(p -> {
                    StaffDetailResponse.VeterinarianProfileDto dto = new StaffDetailResponse.VeterinarianProfileDto();
                    dto.setLicenseNumber(p.getLicenseNumber());
                    dto.setLicenseIssuedDate(p.getLicenseIssuedDate());
                    dto.setSpecialization(p.getSpecialization());
                    response.setProfile(dto);
                });
                break;
            case "HEAD_TRAINER":
                trainerProfileRepository.findById(user.getId()).ifPresent(p -> {
                    StaffDetailResponse.TrainerProfileDto dto = new StaffDetailResponse.TrainerProfileDto();
                    dto.setCertificationNumber(p.getCertificationNumber());
                    dto.setCertificationIssuedDate(p.getCertificationIssuedDate());
                    response.setProfile(dto);
                });
                break;
        }

        return response;
    }

    // ── UPDATE ────────────────────────────────────────────────────────────────

    @Transactional
    public StaffDetailResponse updateStaff(Long userId, StaffUpdateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (user.getRole() == null || !STAFF_ROLES.contains(user.getRole().getName())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not a staff account");
        }

        if (request.getFullName() != null && !request.getFullName().isBlank()) {
            user.setFullName(request.getFullName().trim());
        }
        user.setPhone(request.getPhone());
        user.setAddress(request.getAddress());
        userRepository.save(user);

        switch (user.getRole().getName()) {
            case "GROOM":
                GroomProfile groomProfile = groomProfileRepository.findById(user.getId())
                        .orElseGet(() -> { GroomProfile p = new GroomProfile(); p.setUserId(user.getId()); return p; });
                if (request.getTrainerId() != null) {
                    User trainer = userRepository.findById(request.getTrainerId())
                            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Trainer not found"));
                    if (!"HEAD_TRAINER".equals(trainer.getRole() != null ? trainer.getRole().getName() : "") || !trainer.isActive()) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid or inactive HEAD_TRAINER");
                    }
                    groomProfile.setTrainerId(trainer.getId());
                } else if (request.isTrainerIdProvided()) {
                    groomProfile.setTrainerId(null);
                }
                groomProfileRepository.save(groomProfile);
                break;
            case "VETERINARIAN":
                VeterinarianProfile vetProfile = vetProfileRepository.findById(user.getId())
                        .orElseGet(() -> { VeterinarianProfile p = new VeterinarianProfile(); p.setUserId(user.getId()); return p; });
                if (request.getLicenseNumber() != null && !request.getLicenseNumber().isBlank()) {
                    vetProfile.setLicenseNumber(request.getLicenseNumber().trim());
                }
                vetProfile.setLicenseIssuedDate(request.getLicenseIssuedDate());
                vetProfile.setSpecialization(request.getSpecialization());
                vetProfileRepository.save(vetProfile);
                break;
            case "HEAD_TRAINER":
                TrainerProfile trainerProfile = trainerProfileRepository.findById(user.getId())
                        .orElseGet(() -> { TrainerProfile p = new TrainerProfile(); p.setUserId(user.getId()); return p; });
                if (request.getCertificationNumber() != null && !request.getCertificationNumber().isBlank()) {
                    trainerProfile.setCertificationNumber(request.getCertificationNumber().trim());
                }
                trainerProfile.setCertificationIssuedDate(request.getCertificationIssuedDate());
                trainerProfileRepository.save(trainerProfile);
                break;
        }

        return getStaffDetail(userId);
    }

    // ── CREATE ────────────────────────────────────────────────────────────────

    /**
     * Creates a staff member and performs automatic assignments:
     * <ul>
     *   <li>HEAD_TRAINER: assigns up to 2 unassigned REGULAR areas (deterministic, code ASC)</li>
     *   <li>GROOM: requires trainerId, auto-assigns first free 3-stall block in Trainer's areas</li>
     * </ul>
     * The whole operation is transactional.
     */
    @Transactional
    public StaffCreationResponse createStaff(StaffCreationRequest request) {
        if (request.getRole() == null || !STAFF_ROLES.contains(request.getRole())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid or unauthorized role for staff creation");
        }

        String normalizedEmail = request.getEmail().toLowerCase().trim();
        if (userRepository.findByEmail(normalizedEmail).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already in use");
        }

        Role role = roleRepository.findByName(request.getRole())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Role not found"));

        User user = new User();
        user.setFullName(request.getFullName().trim());
        user.setEmail(normalizedEmail);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setPhone(request.getPhone());
        user.setAddress(request.getAddress());
        user.setRole(role);
        user.setActive(true);
        user = userRepository.save(user);

        StaffCreationResponse response = buildBaseResponse(user);

        switch (request.getRole()) {
            case "HEAD_TRAINER" -> response = createHeadTrainer(user, request, response);
            case "GROOM"        -> response = createGroom(user, request, response);
            case "VETERINARIAN" -> response = createVeterinarian(user, request, response);
        }

        return response;
    }

    // ── Status toggle ─────────────────────────────────────────────────────────

    @Transactional
    public StaffSummaryResponse updateStaffStatus(Long userId, boolean active) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (user.getRole() == null || !STAFF_ROLES.contains(user.getRole().getName())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot update status of non-staff account through this API");
        }

        user.setActive(active);
        user = userRepository.save(user);
        return mapToSummary(user);
    }

    // ── Private creation helpers ──────────────────────────────────────────────

    private StaffCreationResponse createHeadTrainer(User user, StaffCreationRequest request,
                                                     StaffCreationResponse response) {
        if (request.getCertificationNumber() == null || request.getCertificationNumber().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Certification number is required for Head Trainer");
        }
        TrainerProfile profile = new TrainerProfile();
        profile.setUserId(user.getId());
        profile.setCertificationNumber(request.getCertificationNumber().trim());
        profile.setCertificationIssuedDate(request.getCertificationIssuedDate());
        trainerProfileRepository.save(profile);

        // Auto-assign up to 2 unassigned REGULAR areas (deterministic: code ASC).
        // Quarantine areas are excluded. Never fail if no area is available.
        List<Area> freeAreas = areaRepository.findUnassignedRegularAreasOrdered();
        int toAssign = Math.min(freeAreas.size(), MAX_AREAS_PER_TRAINER);

        List<Long>   areaIds   = new ArrayList<>();
        List<String> areaCodes = new ArrayList<>();

        for (int i = 0; i < toAssign; i++) {
            Area area = freeAreas.get(i);
            area.setTrainerId(user.getId());
            areaRepository.save(area);
            areaIds.add(area.getId());
            areaCodes.add(area.getCode());
        }

        response.setAssignedAreaIds(areaIds);
        response.setAssignedAreaCodes(areaCodes);
        response.setProfileSummary("Cert: " + profile.getCertificationNumber()
                + (areaCodes.isEmpty() ? " | No areas assigned" : " | Areas: " + String.join(", ", areaCodes)));
        return response;
    }

    private StaffCreationResponse createGroom(User user, StaffCreationRequest request,
                                              StaffCreationResponse response) {
        if (request.getTrainerId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "trainerId is required for GROOM creation");
        }

        User trainer = userRepository.findById(request.getTrainerId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Trainer not found"));
        if (trainer.getRole() == null || !"HEAD_TRAINER".equals(trainer.getRole().getName()) || !trainer.isActive()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selected user is not an active HEAD_TRAINER");
        }

        GroomProfile groom = new GroomProfile();
        groom.setUserId(user.getId());
        groom.setTrainerId(trainer.getId());
        groomProfileRepository.save(groom);

        response.setTrainerId(trainer.getId());
        response.setTrainerName(trainer.getFullName());

        // Auto-assign first free 3-stall block in Trainer's REGULAR areas (code ASC, then block order).
        // If no block exists, Groom is still created with zero stall assignments.
        List<Area> trainerAreas = new ArrayList<>(
                areaRepository.findByTypeAndTrainerId(AreaType.REGULAR, trainer.getId()));
        trainerAreas.sort((a, b) -> a.getCode().compareTo(b.getCode())); // deterministic: area code ASC

        List<Long>   stallIds   = new ArrayList<>();
        List<String> stallCodes = new ArrayList<>();
        Long foundAreaId   = null;
        String foundAreaCode = null;

        outer:
        for (Area area : trainerAreas) {
            for (int blockStart : BLOCK_STARTS) {
                int blockEnd = blockStart + GROOM_BLOCK_SIZE - 1; // e.g. 1→3, 4→6
                List<StableStall> block = stableStallRepository
                        .findByAreaIdAndStallNumberBetweenOrderByStallNumberAsc(area.getId(), blockStart, blockEnd);

                if (block.size() == GROOM_BLOCK_SIZE && block.stream().allMatch(s -> s.getGroomId() == null)) {
                    // Found a fully free block — assign the groom to all 3 stalls
                    for (StableStall stall : block) {
                        stall.setGroomId(user.getId());
                        stableStallRepository.save(stall);
                        stallIds.add(stall.getId());
                        stallCodes.add(stall.getStallCode());
                    }
                    foundAreaId   = area.getId();
                    foundAreaCode = area.getCode();
                    break outer;
                }
            }
        }

        if (stallIds.isEmpty()) {
            response.setNoStallBlockAvailable(true);
            response.setProfileSummary("Trainer: " + trainer.getFullName() + " | No stall block available");
        } else {
            response.setAssignedAreaId(foundAreaId);
            response.setAssignedAreaCode(foundAreaCode);
            response.setAssignedStallIds(stallIds);
            response.setAssignedStallCodes(stallCodes);
            response.setNoStallBlockAvailable(false);
            response.setProfileSummary("Trainer: " + trainer.getFullName() + " | Stalls: " + String.join(", ", stallCodes));
        }

        return response;
    }

    private StaffCreationResponse createVeterinarian(User user, StaffCreationRequest request,
                                                      StaffCreationResponse response) {
        if (request.getLicenseNumber() == null || request.getLicenseNumber().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "License number is required for Veterinarian");
        }
        VeterinarianProfile vet = new VeterinarianProfile();
        vet.setUserId(user.getId());
        vet.setLicenseNumber(request.getLicenseNumber().trim());
        vet.setLicenseIssuedDate(request.getLicenseIssuedDate());
        vet.setSpecialization(request.getSpecialization());
        vetProfileRepository.save(vet);
        response.setProfileSummary("License: " + vet.getLicenseNumber());
        return response;
    }

    // ── Mapping helpers ───────────────────────────────────────────────────────

    private StaffCreationResponse buildBaseResponse(User user) {
        StaffCreationResponse r = new StaffCreationResponse();
        r.setUserId(user.getId());
        r.setFullName(user.getFullName());
        r.setEmail(user.getEmail());
        r.setPhone(user.getPhone());
        r.setAddress(user.getAddress());
        r.setRole(user.getRole() != null ? user.getRole().getName() : null);
        r.setActive(user.isActive());
        r.setCreatedAt(user.getCreatedAt());
        return r;
    }

    private StaffSummaryResponse mapToSummary(User user) {
        StaffSummaryResponse summary = new StaffSummaryResponse();
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
                case "GROOM":
                    groomProfileRepository.findById(user.getId()).ifPresent(p ->
                            summary.setProfileSummary(p.getTrainerId() != null
                                    ? "Trainer ID: " + p.getTrainerId()
                                    : "No Trainer Assigned"));
                    break;
                case "VETERINARIAN":
                    vetProfileRepository.findById(user.getId()).ifPresent(p ->
                            summary.setProfileSummary("License: " + p.getLicenseNumber()));
                    break;
                case "HEAD_TRAINER":
                    trainerProfileRepository.findById(user.getId()).ifPresent(p ->
                            summary.setProfileSummary("Cert: " + p.getCertificationNumber()));
                    break;
            }
        }

        return summary;
    }
}
