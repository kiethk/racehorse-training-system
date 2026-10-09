package com.rtms.backend.stable.repository;

import com.rtms.backend.stable.entity.Area;
import com.rtms.backend.stable.enums.AreaType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AreaRepository extends JpaRepository<Area, Long> {

    Optional<Area> findByCode(String code);

    List<Area> findByType(AreaType type);

    /** REGULAR areas not yet assigned to any trainer, ordered by code ascending (deterministic). */
    @Query("SELECT a FROM Area a WHERE a.type = 'REGULAR' AND a.trainerId IS NULL ORDER BY a.code ASC")
    List<Area> findUnassignedRegularAreasOrdered();

    /** REGULAR areas already assigned to the given trainer. */
    List<Area> findByTypeAndTrainerId(AreaType type, Long trainerId);

    /** Count REGULAR areas managed by the given trainer. */
    @Query("SELECT COUNT(a) FROM Area a WHERE a.type = 'REGULAR' AND a.trainerId = :trainerId")
    long countRegularAreasByTrainerId(@Param("trainerId") Long trainerId);
}