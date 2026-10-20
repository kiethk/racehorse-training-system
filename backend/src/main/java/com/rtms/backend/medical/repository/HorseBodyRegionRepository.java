package com.rtms.backend.medical.repository;

import com.rtms.backend.medical.entity.HorseBodyRegion;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HorseBodyRegionRepository extends JpaRepository<HorseBodyRegion, Long> {
    List<HorseBodyRegion> findByIsActiveTrueOrderByIdAsc();

}
