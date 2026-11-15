package com.rtms.backend.admission.service;

import com.rtms.backend.admission.enums.AdmissionStatus;
import com.rtms.backend.admission.repository.AdmissionApplicationRepository;
import com.rtms.backend.identity.entity.User;
import com.rtms.backend.identity.repository.GroomProfileRepository;
import com.rtms.backend.identity.repository.UserRepository;
import com.rtms.backend.stable.repository.StableStallRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class GroomWorkloadService {
    private final StableStallRepository stalls;
    private final GroomProfileRepository profiles;
    private final AdmissionApplicationRepository admissions;
    private final UserRepository users;

    public GroomWorkloadService(StableStallRepository stalls, GroomProfileRepository profiles,
                                AdmissionApplicationRepository admissions, UserRepository users) {
        this.stalls = stalls;
        this.profiles = profiles;
        this.admissions = admissions;
        this.users = users;
    }

    public record GroomCandidate(Long id, String name, long workload, boolean full) {}

    /** Candidate locks are held until the assignment transaction commits. */
    @Transactional
    public Optional<Long> selectGroom(Set<Long> excludeGroomIds, boolean requireFreeStall) {
        List<Long> ids = stalls.findQuarantineGroomIds().stream().sorted().toList();
        if (ids.isEmpty()) {
            return Optional.empty();
        }
        // Upsert and lock in the same order, including profiles missing at rollout.
        ids.forEach(profiles::ensureProfile);
        Map<Long, LocalDateTime> arrivals = new HashMap<>();
        profiles.findByUserIdsForUpdate(ids).forEach(p -> arrivals.put(p.getUserId(), p.getLastArrivalConfirmedAt()));
        Map<Long, Long> workloads = workloads(ids);
        Set<Long> free = new HashSet<>(stalls.findGroomIdsWithFreeQuarantineStall(ids));
        return ids.stream()
                .filter(id -> !excludeGroomIds.contains(id))
                .filter(id -> !requireFreeStall || free.contains(id))
                .min(Comparator.comparingLong((Long id) -> workloads.getOrDefault(id, 0L))
                        .thenComparing(arrivals::get, Comparator.nullsFirst(Comparator.naturalOrder()))
                        .thenComparing(Comparator.naturalOrder()));
    }

    @Transactional(readOnly = true)
    public boolean isFull(Long groomId) {
        return stalls.findGroomIdsWithFreeQuarantineStall(List.of(groomId)).isEmpty();
    }

    @Transactional(readOnly = true)
    public List<GroomCandidate> describeQuarantineGrooms() {
        List<Long> ids = stalls.findQuarantineGroomIds();
        if (ids.isEmpty()) {
            return List.of();
        }
        Map<Long, Long> workloads = workloads(ids);
        Set<Long> free = new HashSet<>(stalls.findGroomIdsWithFreeQuarantineStall(ids));
        Map<Long, User> byId = users.findAllById(ids).stream().collect(Collectors.toMap(User::getId, u -> u));
        return ids.stream().map(id -> new GroomCandidate(id, byId.get(id).getFullName(),
                workloads.getOrDefault(id, 0L), !free.contains(id))).toList();
    }

    private Map<Long, Long> workloads(List<Long> ids) {
        Map<Long, Long> result = new HashMap<>();
        addCounts(result, stalls.countHorsesByGroomIds(ids));
        addCounts(result, admissions.countPendingByAssignedGroomIds(ids, AdmissionStatus.GROOM_REVIEW));
        return result;
    }

    private void addCounts(Map<Long, Long> result, List<Object[]> rows) {
        for (Object[] row : rows) {
            result.merge(((Number) row[0]).longValue(), ((Number) row[1]).longValue(), Long::sum);
        }
    }
}
