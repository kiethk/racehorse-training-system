package com.rtms.backend.repository;

import com.rtms.backend.entity.StableStall;
import com.rtms.backend.enums.StallStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StableStallRepository extends JpaRepository<StableStall, Long> {

    List<StableStall> findByAreaId(Long areaId);

    List<StableStall> findByStatus(StallStatus status);

    List<StableStall> findByAreaIdAndStatus(Long areaId, StallStatus status);

    Optional<StableStall> findByStallCode(String stallCode);
}