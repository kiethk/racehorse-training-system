package com.rtms.backend.identity.repository;

import com.rtms.backend.identity.entity.GroomProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GroomProfileRepository extends JpaRepository<GroomProfile, Long> {
    List<GroomProfile> findByTrainerId(Long trainerId);
    long countByTrainerId(Long trainerId);
}
