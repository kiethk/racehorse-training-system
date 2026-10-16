package com.rtms.backend.stable.service;

import com.rtms.backend.stable.entity.Area;
import com.rtms.backend.stable.entity.StableStall;
import com.rtms.backend.stable.enums.AreaType;
import com.rtms.backend.stable.enums.StallStatus;
import com.rtms.backend.stable.repository.AreaRepository;
import com.rtms.backend.stable.repository.StableStallRepository;
import com.rtms.backend.stable.util.AreaCodeGenerator;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class AreaService {

    public static final int STALLS_PER_AREA = 12;

    private final AreaRepository areaRepository;
    private final StableStallRepository stableStallRepository;
    private final EntityManager entityManager;

    public AreaService(AreaRepository areaRepository,
                       StableStallRepository stableStallRepository,
                       EntityManager entityManager) {
        this.areaRepository = areaRepository;
        this.stableStallRepository = stableStallRepository;
        this.entityManager = entityManager;
    }

    /**
     * Creates a new Area and atomically creates all 12 stalls with AVAILABLE status.
     * Thread-safe and cluster-safe using PostgreSQL advisory transaction lock.
     */
    @Transactional
    public synchronized Area createArea(AreaType type) {
        Objects.requireNonNull(type, "Area type must not be null");

        // Cluster/DB-level lock for PostgreSQL to serialize concurrent creation requests
        entityManager.createNativeQuery(
                    "SELECT 1 FROM pg_advisory_xact_lock(hashtextextended(:key, 0))")
                    .setParameter("key", "AREA_CREATION_" + type.name())
                    .getSingleResult();

        List<Area> existingAreas = areaRepository.findByType(type);
        List<String> existingCodes = existingAreas.stream()
                .map(Area::getCode)
                .collect(Collectors.toList());

        String nextCode = (type == AreaType.REGULAR)
                ? AreaCodeGenerator.nextRegularCode(existingCodes)
                : AreaCodeGenerator.nextQuarantineCode(existingCodes);

        String name = AreaCodeGenerator.generateAreaName(type, nextCode);

        Area area = new Area();
        area.setCode(nextCode);
        area.setName(name);
        area.setType(type);
        area.setTrainerId(null);

        Area savedArea = areaRepository.save(area);

        List<StableStall> stalls = new ArrayList<>(STALLS_PER_AREA);
        for (int i = 1; i <= STALLS_PER_AREA; i++) {
            StableStall stall = new StableStall();
            stall.setAreaId(savedArea.getId());
            stall.setStallNumber(i);
            stall.setStallCode(AreaCodeGenerator.formatStallCode(type, nextCode, i));
            stall.setStatus(StallStatus.AVAILABLE);
            stall.setGroomId(null);
            stalls.add(stall);
        }

        stableStallRepository.saveAll(stalls);

        return savedArea;
    }
}
