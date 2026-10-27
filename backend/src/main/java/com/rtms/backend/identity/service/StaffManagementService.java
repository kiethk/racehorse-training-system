package com.rtms.backend.identity.service;
import com.rtms.backend.identity.entity.GroomProfile;
import com.rtms.backend.identity.entity.Role;
import com.rtms.backend.identity.entity.TrainerProfile;
import com.rtms.backend.identity.entity.User;
import com.rtms.backend.identity.entity.VeterinarianProfile;
import com.rtms.backend.identity.repository.GroomProfileRepository;
import com.rtms.backend.identity.repository.RoleRepository;
import com.rtms.backend.identity.repository.TrainerProfileRepository;
import com.rtms.backend.identity.repository.UserRepository;
import com.rtms.backend.identity.repository.VeterinarianProfileRepository;
import com.rtms.backend.stable.entity.Area;
import com.rtms.backend.stable.entity.StableStall;
import com.rtms.backend.stable.repository.AreaRepository;
import com.rtms.backend.stable.repository.StableStallRepository;
import com.rtms.backend.stable.service.AreaTrainerAssignmentPolicy;
import com.rtms.backend.stable.util.AreaCodeGenerator;
import jakarta.persistence.EntityManager;
import jakarta.validation.Validator;
import org.springframework.dao.DataIntegrityViolationException;


import com.rtms.backend.identity.dto.StaffCreationRequest;
import com.rtms.backend.identity.dto.StaffCreationResponse;
import com.rtms.backend.identity.dto.StaffDetailResponse;
import com.rtms.backend.identity.dto.StaffSummaryResponse;
import com.rtms.backend.identity.dto.StaffUpdateRequest;
import com.rtms.backend.stable.enums.AreaType;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.Locale;
import java.util.stream.Collectors;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.Objects;
import org.hibernate.exception.ConstraintViolationException;

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
    private final AreaTrainerAssignmentPolicy trainerAssignmentPolicy;
    private final EntityManager em;
    private final Validator validator;

    private static final List<String> STAFF_ROLES = Arrays.asList("GROOM", "VETERINARIAN", "HEAD_TRAINER");

    public StaffManagementService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            GroomProfileRepository groomProfileRepository,
            VeterinarianProfileRepository vetProfileRepository,
            TrainerProfileRepository trainerProfileRepository,
            AreaRepository areaRepository,
            StableStallRepository stableStallRepository,
            AreaTrainerAssignmentPolicy trainerAssignmentPolicy, EntityManager em, Validator validator) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.groomProfileRepository = groomProfileRepository;
        this.vetProfileRepository = vetProfileRepository;
        this.trainerProfileRepository = trainerProfileRepository;
        this.areaRepository = areaRepository;
        this.stableStallRepository = stableStallRepository;
        this.trainerAssignmentPolicy = trainerAssignmentPolicy;
        this.em = em;
        this.validator = validator;
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

                    // Invariant: Groom.trainer_id == Stall.Area.trainer_id
                    // If this Groom already owns stalls, every stall's area must belong to the new Trainer.
                    // Reject the reassignment with a clear error if any stall is outside the new Trainer's areas.
                    List<StableStall> ownedStalls = stableStallRepository.findByGroomId(user.getId());
                    if (!ownedStalls.isEmpty()) {
                        boolean mismatch = ownedStalls.stream().anyMatch(s -> {
                            Area stallArea = areaRepository.findById(s.getAreaId()).orElse(null);
                            return stallArea == null || !trainer.getId().equals(stallArea.getTrainerId());
                        });
                        if (mismatch) {
                            throw new ResponseStatusException(HttpStatus.CONFLICT,
                                    "Cannot reassign Trainer: Groom owns stalls that belong to the current Trainer's areas. " +
                                    "Unassign the stalls first, then change the Trainer.");
                        }
                    }

                    groomProfile.setTrainerId(trainer.getId());
                } else if (request.isTrainerIdProvided()) {
                    List<StableStall> ownedStalls = stableStallRepository.findByGroomId(user.getId());
                    if (!ownedStalls.isEmpty()) {
                        throw new ResponseStatusException(HttpStatus.CONFLICT,
                                "Cannot remove Trainer while Groom is assigned to stalls. Unassign the stalls first.");
                    }
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
     *   <li>HEAD_TRAINER: assigns up to 2 empty unassigned REGULAR areas in generated code order.</li>
     *   <li>GROOM: prioritizes horses without a Groom, then balances reporting groups by Groom
     *       count and free blocks. A REGULAR group belongs to an active Trainer; each Q Area is
     *       a group without a Trainer. No free block still creates an UNASSIGNED account.</li>
     * </ul>
     * The whole operation is transactional.
     */
    @Transactional
    public StaffCreationResponse createStaff(StaffCreationRequest request) {
        if (request == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Staff details are required");
        if (request.getRole() == null || !STAFF_ROLES.contains(request.getRole())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid or unauthorized role for staff creation");
        }

        // Normalize before validating, including callers such as the manual seed workflow.
        if (request.getEmail() != null) request.setEmail(request.getEmail().trim().toLowerCase(Locale.ROOT));
        var errors = validator.validate(request);
        if (!errors.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                errors.stream().sorted(Comparator.comparing(e -> e.getPropertyPath().toString())).findFirst().orElseThrow().getMessage());
        if (request.getPassword().getBytes(StandardCharsets.UTF_8).length > 72)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password must be at most 72 UTF-8 bytes");
        // Take the same lock as Manager Stable BEFORE reading allocation candidates.
        // Concurrent creations then see the previous committed assignment, not the same free block.
        if (!"VETERINARIAN".equals(request.getRole())) em.createNativeQuery(
                "SELECT 1 FROM pg_advisory_xact_lock(hashtextextended(:key,0))")
                .setParameter("key", "STABLE_ASSIGNMENTS_LOCK").getSingleResult();

        String normalizedEmail = request.getEmail();
        if (userRepository.findByEmail(normalizedEmail).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already in use");
        }

        Role role = roleRepository.findByName(request.getRole())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Role not found"));

        User user = new User();
        user.setFullName(request.getFullName().trim());
        user.setEmail(normalizedEmail);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setPhone(clean(request.getPhone()));
        user.setAddress(clean(request.getAddress()));
        user.setRole(role);
        user.setActive(true);
        try { user = userRepository.saveAndFlush(user); }
        catch (DataIntegrityViolationException ex) {
            for (Throwable cause=ex; cause!=null; cause=cause.getCause()) {
                if (cause instanceof ConstraintViolationException constraint
                        && "users_email_key".equals(constraint.getConstraintName()))
                    throw new ResponseStatusException(HttpStatus.CONFLICT,"Email already in use",ex);
            }
            throw ex;
        }

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

        // Auto-assign up to 2 unassigned REGULAR areas (A..Z, AA..).
        // Quarantine areas are excluded. Never fail if no area is available.
        List<Area> freeAreas = areaRepository.findUnassignedRegularAreasOrdered();

        List<Long>   areaIds   = new ArrayList<>();
        List<String> areaCodes = new ArrayList<>();

        for (Area area : freeAreas) {
            if (areaIds.size() == MAX_AREAS_PER_TRAINER) break;
            // Area rows are already locked. Lock their stalls before checking so an
            // arrival/allocation cannot turn an empty Area into an occupied one mid-assignment.
            var lockedStalls = stableStallRepository.findBlockForUpdate(area.getId(), 1, 12);
            if (lockedStalls.size() != 12 || !trainerAssignmentPolicy.check(area.getId()).canChange()) continue;
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
        Set<Long> occupiedStalls = ((List<?>) em.createNativeQuery(
                "SELECT current_stall_id FROM horses WHERE current_stall_id IS NOT NULL").getResultList())
                .stream().map(id -> ((Number) id).longValue()).collect(Collectors.toSet());
        List<User> activeTrainers = userRepository.findActiveHeadTrainers();
        
        User bestTrainer = null;
        int bestTrainerFreeBlocks = -1;
        long bestTrainerGroomCount = Long.MAX_VALUE;
        int bestOccupied = -1;
        
        Area bestAreaToAssign = null;
        List<StableStall> bestBlockToAssign = null;

        for (User trainer : activeTrainers) {
            List<Area> trainerAreas = new ArrayList<>(
                    areaRepository.findByTypeAndTrainerId(AreaType.REGULAR, trainer.getId()));
            
            if (trainerAreas.isEmpty()) continue;
            
            trainerAreas.sort((a, b) -> AreaCodeGenerator.compareCodes(a.getCode(), b.getCode()));

            int freeBlocks = 0;
            Area firstFreeArea = null;
            List<StableStall> firstFreeBlock = null;
            int firstOccupied = -1;

            for (Area area : trainerAreas) {
                for (int blockStart : BLOCK_STARTS) {
                    int blockEnd = blockStart + GROOM_BLOCK_SIZE - 1; // e.g. 1→3, 4→6
                    List<StableStall> block = stableStallRepository
                            .findByAreaIdAndStallNumberBetweenOrderByStallNumberAsc(area.getId(), blockStart, blockEnd);

                    if (block.size() == GROOM_BLOCK_SIZE && block.stream().allMatch(s -> s.getGroomId() == null)) {
                        freeBlocks++;
                        int occupied = (int) block.stream().filter(s -> occupiedStalls.contains(s.getId())).count();
                        if (occupied > firstOccupied) {
                            firstFreeBlock = block;
                            firstFreeArea = area;
                            firstOccupied = occupied;
                        }
                    }
                }
            }

            if (freeBlocks > 0) {
                long groomCount = groomProfileRepository.countByTrainerId(trainer.getId());
                
                boolean isBetter = false;
                if (firstOccupied > bestOccupied || firstOccupied == bestOccupied && groomCount < bestTrainerGroomCount) {
                    isBetter = true;
                } else if (firstOccupied == bestOccupied && groomCount == bestTrainerGroomCount) {
                    if (freeBlocks > bestTrainerFreeBlocks) {
                        isBetter = true;
                    } else if (freeBlocks == bestTrainerFreeBlocks) {
                        if (bestTrainer == null || trainer.getId() < bestTrainer.getId()) {
                            isBetter = true;
                        }
                    }
                }

                if (isBetter) {
                    bestTrainer = trainer;
                    bestTrainerGroomCount = groomCount;
                    bestTrainerFreeBlocks = freeBlocks;
                    bestAreaToAssign = firstFreeArea;
                    bestBlockToAssign = firstFreeBlock;
                    bestOccupied = firstOccupied;
                }
            }
        }

        // Quarantine is its own allocation group; its Grooms do not report to a Trainer.
        var quarantineAreas = new ArrayList<>(areaRepository.findByType(AreaType.QUARANTINE));
        quarantineAreas.sort((a,b) -> AreaCodeGenerator.compareCodes(a.getCode(),b.getCode()));
        for (Area area : quarantineAreas) {
            var rows = stableStallRepository.findByAreaId(area.getId());
            long load = rows.stream().map(StableStall::getGroomId).filter(Objects::nonNull).distinct().count();
            var free = new ArrayList<List<StableStall>>();
            for (int start : BLOCK_STARTS) {
                int end = start + 2;
                var block = rows.stream().filter(s -> s.getStallNumber() >= start && s.getStallNumber() <= end)
                        .sorted(Comparator.comparing(StableStall::getStallNumber)).toList();
                if (block.size()==3 && block.stream().allMatch(s -> s.getGroomId()==null)) free.add(block);
            }
            for (var block : free) {
                int occupied = (int) block.stream().filter(s -> occupiedStalls.contains(s.getId())).count();
                if (occupied > bestOccupied || occupied == bestOccupied && (load < bestTrainerGroomCount
                        || load == bestTrainerGroomCount && free.size() > bestTrainerFreeBlocks)) {
                    bestTrainer=null; bestAreaToAssign=area; bestBlockToAssign=block;
                    bestTrainerGroomCount=load; bestTrainerFreeBlocks=free.size(); bestOccupied=occupied;
                }
            }
        }

        GroomProfile groom = new GroomProfile();
        groom.setUserId(user.getId());

        if (bestAreaToAssign == null) {
            groom.setTrainerId(null);
            groomProfileRepository.save(groom);
            
            response.setTrainerId(null);
            response.setTrainerName(null);
            response.setAssignedAreaId(null);
            response.setAssignedAreaCode(null);
            response.setAssignedStallIds(new ArrayList<>());
            response.setAssignedStallCodes(new ArrayList<>());
            response.setAssignmentStatus("UNASSIGNED");
            response.setNoStallBlockAvailable(true);
            response.setProfileSummary("No unassigned three-stall block is available.");
            
            return response;
        }

        // ── Concurrency guard: reload & lock the chosen block inside this transaction ──
        int blockStart = bestBlockToAssign.get(0).getStallNumber();
        int blockEnd   = blockStart + GROOM_BLOCK_SIZE - 1;
        List<StableStall> lockedBlock = stableStallRepository.findBlockForUpdate(
                bestAreaToAssign.getId(), blockStart, blockEnd);
        em.refresh(bestAreaToAssign);
        for (StableStall stall : lockedBlock) em.refresh(stall);
        if (bestTrainer != null) {
            em.refresh(bestTrainer);
            if (!bestTrainer.isActive() || !bestTrainer.getId().equals(bestAreaToAssign.getTrainerId()))
                throw new ResponseStatusException(HttpStatus.CONFLICT,"Trainer assignment changed. Please retry.");
        }

        // Validate the locked snapshot: must still be a complete, unoccupied block
        if (lockedBlock.size() != GROOM_BLOCK_SIZE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Stall block was modified concurrently — please retry");
        }
        for (int i = 0; i < GROOM_BLOCK_SIZE; i++) {
            StableStall s = lockedBlock.get(i);
            if (s.getGroomId() != null) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Stall " + s.getStallCode() + " was taken concurrently — please retry");
            }
            if (s.getStallNumber() != blockStart + i) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Stall block is no longer consecutive — please retry");
            }
        }

        groom.setTrainerId(bestTrainer == null ? null : bestTrainer.getId());
        groomProfileRepository.save(groom);

        List<Long> stallIds = new ArrayList<>();
        List<String> stallCodes = new ArrayList<>();

        for (StableStall stall : lockedBlock) {
            stall.setGroomId(user.getId());
            stableStallRepository.save(stall);
            stallIds.add(stall.getId());
            stallCodes.add(stall.getStallCode());
        }

        response.setTrainerId(bestTrainer == null ? null : bestTrainer.getId());
        response.setTrainerName(bestTrainer == null ? null : bestTrainer.getFullName());
        response.setAssignedAreaType(bestAreaToAssign.getType());
        response.setAssignedAreaId(bestAreaToAssign.getId());
        response.setAssignedAreaCode(bestAreaToAssign.getCode());
        response.setAssignedStallIds(stallIds);
        response.setAssignedStallCodes(stallCodes);
        response.setAssignmentStatus("ASSIGNED");
        response.setNoStallBlockAvailable(false);
        response.setProfileSummary("Area: " + bestAreaToAssign.getCode() + " | Stalls: " + String.join(", ", stallCodes));

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
    private static String clean(String value) { return value == null || value.isBlank() ? null : value.trim(); }

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
