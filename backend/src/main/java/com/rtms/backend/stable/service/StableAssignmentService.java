package com.rtms.backend.stable.service;

import com.rtms.backend.horse.entity.Horse;
import com.rtms.backend.horse.repository.HorseRepository;
import com.rtms.backend.identity.entity.GroomProfile;
import com.rtms.backend.identity.entity.User;
import com.rtms.backend.identity.repository.GroomProfileRepository;
import com.rtms.backend.identity.repository.UserRepository;
import com.rtms.backend.stable.dto.*;
import com.rtms.backend.stable.entity.Area;
import com.rtms.backend.stable.entity.StableStall;
import com.rtms.backend.stable.enums.AreaType;
import com.rtms.backend.stable.repository.AreaRepository;
import com.rtms.backend.stable.repository.StableStallRepository;
import com.rtms.backend.training.service.TrainingWorkoutService;
import jakarta.persistence.EntityManager;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class StableAssignmentService {
    public static final int MAX_REGULAR_AREAS_PER_TRAINER = 2;
    public static final int STALLS_PER_BLOCK = 3;
    public static final int BLOCKS_PER_AREA = 4;

    private final AreaRepository areas;
    private final StableStallRepository stalls;
    private final UserRepository users;
    private final GroomProfileRepository profiles;
    private final HorseRepository horses;
    private final TrainingWorkoutService workouts;
    private final EntityManager em;
    private final AreaTrainerAssignmentPolicy trainerPolicy;

    public StableAssignmentService(AreaRepository areas, StableStallRepository stalls,
                                   UserRepository users, GroomProfileRepository profiles,
                                   HorseRepository horses, TrainingWorkoutService workouts, EntityManager em,
                                   AreaTrainerAssignmentPolicy trainerPolicy) {
        this.areas = areas;
        this.stalls = stalls;
        this.users = users;
        this.profiles = profiles;
        this.horses = horses;
        this.workouts = workouts;
        this.em = em;
        this.trainerPolicy = trainerPolicy;
    }

    public List<AreaTrainerChangeCheck> getTrainerAssignmentChecks() { return trainerPolicy.checks(); }

    @Transactional(rollbackFor = Exception.class)
    public StableAssignmentResponse applyAssignments(BatchStableAssignmentRequest request) {
        if (request == null) throw bad("Assignment request is required");
        var trainerEdits = request.getTrainerAssignments();
        var groomEdits = request.getGroomAssignments();
        Set<Long> trainerTargets = new TreeSet<>();
        Set<Block> blockTargets = new HashSet<>();
        for (var edit : trainerEdits) {
            if (edit == null || edit.getAreaId() == null) throw bad("areaId is required");
            if (!trainerTargets.add(edit.getAreaId())) throw bad("Duplicate trainer target");
            if (!edit.isExpectedTrainerIdSet()) throw bad("expectedTrainerId is required, including explicit null");
        }
        for (var edit : groomEdits) {
            if (edit == null || edit.getAreaId() == null) throw bad("areaId is required");
            Integer number = edit.getBlockNumber();
            if (number == null || number < 1 || number > BLOCKS_PER_AREA) throw bad("blockNumber must be 1..4");
            if (!blockTargets.add(new Block(edit.getAreaId(), number))) throw bad("Duplicate groom block target");
            baseline(edit); // Reject omitted or malformed baselines before taking locks.
        }
        if (trainerEdits.isEmpty() && groomEdits.isEmpty()) {
            return new StableAssignmentResponse(List.of(), List.of(), 0, "No assignments to update");
        }

        // Serialize across application instances. Failure MUST abort the transaction.
        em.createNativeQuery("SELECT 1 FROM pg_advisory_xact_lock(hashtextextended(:key, 0))")
                .setParameter("key", "STABLE_ASSIGNMENTS_LOCK").getSingleResult();
        // The farm is small: lock the allocation snapshot in stable order. Row locks also
        // coordinate with existing Staff block allocation and horse/stall writers.
        var allAreas = areas.findAllForAssignmentUpdate();
        var allStalls = stalls.findAllForAssignmentUpdate();
        Map<Long, Area> areaById = allAreas.stream().collect(Collectors.toMap(Area::getId, a -> a));
        Map<Block, List<StableStall>> blocks = allStalls.stream().collect(Collectors.groupingBy(StableAssignmentService::blockOf));
        blocks.values().forEach(b -> b.sort(Comparator.comparing(StableStall::getStallNumber)));
        Set<Long> targetAreaIds = new TreeSet<>(trainerTargets);
        blockTargets.forEach(b -> targetAreaIds.add(b.areaId()));
        for (Long id : targetAreaIds) {
            Area area = areaById.get(id);
            if (area == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Area #" + id + " not found");
            // A legacy entry point may have loaded an entity before waiting for our lock.
            em.refresh(area);
        }
        for (Block target : blockTargets) {
            List<StableStall> block = blocks.getOrDefault(target, List.of());
            if (block.size() != STALLS_PER_BLOCK) throw bad("Block must contain exactly three stalls");
            for (int i = 0; i < STALLS_PER_BLOCK; i++) {
                StableStall stall = block.get(i);
                em.refresh(stall);
                if (stall.getStallNumber() != (target.number() - 1) * STALLS_PER_BLOCK + i + 1) {
                    throw bad("Block has invalid stall numbers");
                }
            }
        }

        Map<Long, Long> finalTrainer = new HashMap<>();
        allAreas.forEach(a -> finalTrainer.put(a.getId(), a.getTrainerId()));
        Map<Long, Long> finalGroom = new HashMap<>();
        allStalls.forEach(s -> finalGroom.put(s.getId(), s.getGroomId()));
        Set<Long> affectedTrainers = new TreeSet<>();
        Set<Long> affectedGrooms = new TreeSet<>();
        Set<Long> incomingTrainers = new TreeSet<>();
        Set<Long> incomingGrooms = new TreeSet<>();

        for (var edit : trainerEdits) {
            Area area = areaById.get(edit.getAreaId());
            if (!Objects.equals(area.getTrainerId(), edit.getExpectedTrainerId())) throw stale("Area trainer assignment");
            if (area.getType() == AreaType.QUARANTINE && edit.getTrainerId() != null) {
                throw bad("Quarantine areas cannot have an assigned trainer");
            }
            if (!Objects.equals(area.getTrainerId(), edit.getTrainerId())) {
                trainerPolicy.requireChangeAllowed(area.getId());
                add(affectedTrainers, area.getTrainerId());
                add(affectedTrainers, edit.getTrainerId());
                for (StableStall stall : allStalls) {
                    if (stall.getAreaId().equals(area.getId())) add(affectedGrooms, stall.getGroomId());
                }
            }
            add(incomingTrainers, edit.getTrainerId());
            finalTrainer.put(area.getId(), edit.getTrainerId());
        }
        for (var edit : groomEdits) {
            var block = blocks.get(new Block(edit.getAreaId(), edit.getBlockNumber()));
            if (!block.stream().map(StableStall::getGroomId).toList().equals(baseline(edit))) {
                throw stale("Block groom assignments");
            }
            for (StableStall stall : block) {
                add(affectedGrooms, stall.getGroomId());
                finalGroom.put(stall.getId(), edit.getGroomId());
            }
            add(affectedGrooms, edit.getGroomId());
            add(incomingGrooms, edit.getGroomId());
            Area area = areaById.get(edit.getAreaId());
            if (edit.getGroomId() != null && area.getType() == AreaType.REGULAR) {
                add(incomingTrainers, finalTrainer.get(area.getId()));
            }
        }

        Set<Long> staffIds = new TreeSet<>(incomingTrainers);
        staffIds.addAll(incomingGrooms);
        staffIds.addAll(affectedTrainers);
        staffIds.addAll(affectedGrooms);
        Map<Long, User> staff = staffIds.isEmpty() ? Map.of() : users.findByIdsForUpdate(staffIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));
        incomingTrainers.forEach(id -> validateStaff(staff.get(id), "HEAD_TRAINER"));
        incomingGrooms.forEach(id -> validateStaff(staff.get(id), "GROOM"));

        for (Long trainerId : affectedTrainers) {
            long before = allAreas.stream().filter(a -> a.getType() == AreaType.REGULAR && trainerId.equals(a.getTrainerId())).count();
            long after = allAreas.stream().filter(a -> a.getType() == AreaType.REGULAR && trainerId.equals(finalTrainer.get(a.getId()))).count();
            if (after > MAX_REGULAR_AREAS_PER_TRAINER && after >= before) {
                throw bad("Trainer exceeds maximum regular area capacity of 2");
            }
        }
        for (Long groomId : affectedGrooms) {
            var before = assignedStalls(allStalls, groomId, null);
            var after = assignedStalls(allStalls, groomId, finalGroom);
            Set<Long> beforeIds = before.stream().map(StableStall::getId).collect(Collectors.toSet());
            boolean reducingLegacy = after.size() < before.size() && after.stream().allMatch(s -> beforeIds.contains(s.getId()));
            if (!validGroomAllocation(after) && !reducingLegacy) {
                throw bad("Groom cannot be assigned to multiple blocks or more than 3 stalls");
            }
        }
        for (Long areaId : targetAreaIds) {
            Area area = areaById.get(areaId);
            boolean hasGroom = allStalls.stream().anyMatch(s -> s.getAreaId().equals(areaId) && finalGroom.get(s.getId()) != null);
            if (area.getType() == AreaType.REGULAR && hasGroom && finalTrainer.get(areaId) == null) {
                throw bad("Regular area has assigned grooms and must have an assigned trainer");
            }
        }

        var targetStallIds = allStalls.stream().filter(s -> blockTargets.contains(blockOf(s))).map(StableStall::getId).toList();
        Map<Long, Horse> horseByStall = targetStallIds.isEmpty() ? Map.of() : horses.findByCurrentStallIdIn(targetStallIds).stream()
                .collect(Collectors.toMap(Horse::getCurrentStallId, h -> h));
        Map<Long, Long> horseGroomChanges = new LinkedHashMap<>();
        for (var edit : groomEdits) {
            for (StableStall stall : blocks.get(new Block(edit.getAreaId(), edit.getBlockNumber()))) {
                Horse horse = horseByStall.get(stall.getId());
                if (horse != null && edit.getGroomId() == null) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT,
                            "Cannot remove Groom from a block with horses. Swap Groom instead.");
                }
                if (horse != null && !Objects.equals(stall.getGroomId(), edit.getGroomId())) {
                    horseGroomChanges.put(horse.getId(), edit.getGroomId());
                }
            }
        }
        int changedWorkouts;
        try {
            changedWorkouts = horseGroomChanges.isEmpty() ? 0 : workouts.reassignFutureWorkoutsToGrooms(horseGroomChanges);
        } catch (IllegalStateException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, ex.getMessage(), ex);
        }

        List<Area> changedAreas = new ArrayList<>();
        List<StableStall> changedStalls = new ArrayList<>();
        for (var edit : trainerEdits) {
            Area area = areaById.get(edit.getAreaId());
            if (!Objects.equals(area.getTrainerId(), edit.getTrainerId())) {
                area.setTrainerId(edit.getTrainerId());
                changedAreas.add(areas.save(area));
            }
        }
        for (StableStall stall : allStalls) {
            Long groomId = finalGroom.get(stall.getId());
            if (!Objects.equals(stall.getGroomId(), groomId)) {
                stall.setGroomId(groomId);
                changedStalls.add(stalls.save(stall));
            }
        }
        for (Long groomId : affectedGrooms) {
            var allocation = assignedStalls(allStalls, groomId, finalGroom);
            if (!validGroomAllocation(allocation)) continue; // A legacy repair may still have more work to do.
            GroomProfile profile = profiles.findById(groomId).orElseGet(() -> {
                GroomProfile p = new GroomProfile();
                p.setUserId(groomId);
                return p;
            });
            Area area = allocation.isEmpty() ? null : areaById.get(allocation.getFirst().getAreaId());
            profile.setTrainerId(area != null && area.getType() == AreaType.REGULAR ? finalTrainer.get(area.getId()) : null);
            profiles.save(profile);
        }
        return new StableAssignmentResponse(changedAreas, changedStalls, changedWorkouts, "Assignments updated successfully");
    }

    private static List<Long> baseline(BlockGroomAssignmentDto edit) {
        if (edit.getExpectedGroomIds() != null) {
            if (edit.isExpectedGroomIdSet()) throw bad("Use expectedGroomIds or expectedGroomId, not both");
            if (edit.getExpectedGroomIds().size() != STALLS_PER_BLOCK) throw bad("expectedGroomIds must contain three entries");
            return edit.getExpectedGroomIds();
        }
        if (!edit.isExpectedGroomIdSet()) throw bad("expectedGroomIds is required, including three nulls for an empty block");
        return Collections.nCopies(STALLS_PER_BLOCK, edit.getExpectedGroomId());
    }

    private static List<StableStall> assignedStalls(List<StableStall> all, Long groomId, Map<Long, Long> projected) {
        return all.stream().filter(s -> groomId.equals(projected == null ? s.getGroomId() : projected.get(s.getId()))).toList();
    }

    private static boolean validGroomAllocation(List<StableStall> allocation) {
        return allocation.size() <= STALLS_PER_BLOCK && allocation.stream().map(StableAssignmentService::blockOf).distinct().count() <= 1;
    }

    private static void validateStaff(User user, String role) {
        if (user == null) throw bad(role + " not found");
        if (user.getRole() == null || !role.equals(user.getRole().getName())) throw bad("User must have role " + role);
        if (!user.isActive()) throw bad(role + " is inactive");
    }

    private static void add(Set<Long> values, Long id) { if (id != null) values.add(id); }
    private static Block blockOf(StableStall s) { return new Block(s.getAreaId(), (s.getStallNumber() - 1) / STALLS_PER_BLOCK + 1); }
    private record Block(Long areaId, int number) { }
    private static ResponseStatusException bad(String message) { return new ResponseStatusException(HttpStatus.BAD_REQUEST, message); }
    private static ResponseStatusException stale(String target) {
        return new ResponseStatusException(HttpStatus.CONFLICT, target + " changed since loaded. Please refresh.");
    }
}
