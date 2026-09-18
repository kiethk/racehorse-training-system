package com.rtms.backend.repository;

import com.rtms.backend.entity.GroomProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GroomProfileRepository extends JpaRepository<GroomProfile, Long> {
}
