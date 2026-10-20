package com.rtms.backend.identity.repository;

import com.rtms.backend.identity.entity.VeterinarianProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VeterinarianProfileRepository extends JpaRepository<VeterinarianProfile, Long> {
}
