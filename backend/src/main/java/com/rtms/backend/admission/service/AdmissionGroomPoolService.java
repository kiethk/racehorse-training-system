package com.rtms.backend.admission.service;

import com.rtms.backend.admission.entity.AdmissionApplication;
import com.rtms.backend.admission.enums.AdmissionStatus;
import com.rtms.backend.stable.repository.StableStallRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Decides which grooms see the GROOM_REVIEW pool. A groom who approves an admission takes the horse into
 * one of their own quarantine stalls, so the pool goes to whoever should take the next horse:
 * <ul>
 *   <li>eligible: active grooms with at least one free quarantine stall;</li>
 *   <li>least loaded: eligible grooms whose quarantine stalls hold the fewest horses (ties all qualify);</li>
 *   <li>after the fallback window an admission is shown to every eligible groom, so one absent groom
 *       cannot hold the queue.</li>
 * </ul>
 * Loads change with every reservation, so callers that act on the answer must first hold
 * {@link StableStallRepository#lockAdmissionCapacityStallsForUpdate()}.
 */
@Service
public class AdmissionGroomPoolService {

    /** What one groom may see of the pool at this moment. */
    public record PoolView(boolean leastLoaded, boolean eligible, LocalDateTime fallbackCutoff) {
        public boolean canSee(AdmissionApplication admission) {
            if (admission.getStatus() != AdmissionStatus.GROOM_REVIEW) return false;
            if (leastLoaded) return true;
            LocalDateTime startedAt = admission.getGroomReviewStartedAt();
            return eligible && startedAt != null && !startedAt.isAfter(fallbackCutoff);
        }
    }

    record GroomLoad(Long groomId, long load, long free) { }

    static final long DEFAULT_FALLBACK_HOURS = 24;

    private final StableStallRepository stalls;
    private final long fallbackHours;
    private final Clock clock;

    @Autowired
    public AdmissionGroomPoolService(StableStallRepository stalls,
            @Value("${rtms.admission.groom-pool-fallback-hours:24}") long fallbackHours) {
        this(stalls, fallbackHours, Clock.systemDefaultZone());
    }

    /** Default window, for callers built by hand (tests, legacy constructors). */
    AdmissionGroomPoolService(StableStallRepository stalls) {
        this(stalls, DEFAULT_FALLBACK_HOURS, Clock.systemDefaultZone());
    }

    AdmissionGroomPoolService(StableStallRepository stalls, long fallbackHours, Clock clock) {
        this.stalls = stalls;
        this.fallbackHours = fallbackHours;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public PoolView viewFor(Long groomId) {
        List<GroomLoad> eligible = eligibleLoads();
        long minLoad = minLoad(eligible);
        GroomLoad mine = eligible.stream().filter(g -> g.groomId().equals(groomId)).findFirst().orElse(null);
        return new PoolView(mine != null && mine.load() == minLoad, mine != null, fallbackCutoff());
    }

    /** Grooms who see every admission in the pool right now. */
    @Transactional(readOnly = true)
    public List<Long> leastLoadedGroomIds() {
        List<GroomLoad> eligible = eligibleLoads();
        long minLoad = minLoad(eligible);
        return eligible.stream().filter(g -> g.load() == minLoad).map(GroomLoad::groomId).toList();
    }

    /** Grooms who see an admission once it has waited past the fallback window. */
    @Transactional(readOnly = true)
    public List<Long> eligibleGroomIds() {
        return eligibleLoads().stream().map(GroomLoad::groomId).toList();
    }

    public LocalDateTime fallbackCutoff() {
        return LocalDateTime.now(clock).minusHours(fallbackHours);
    }

    private List<GroomLoad> eligibleLoads() {
        return stalls.findQuarantineGroomLoads().stream()
                .map(row -> new GroomLoad(((Number) row[0]).longValue(),
                        ((Number) row[1]).longValue(), ((Number) row[2]).longValue()))
                .filter(g -> g.free() > 0)
                .toList();
    }

    private static long minLoad(List<GroomLoad> eligible) {
        return eligible.stream().mapToLong(GroomLoad::load).min().orElse(Long.MAX_VALUE);
    }
}
