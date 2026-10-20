package com.rtms.backend.admission.repository;

import com.rtms.backend.admission.entity.CandidateHorseProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.Collection;
import java.util.List;

@Repository
public interface CandidateHorseProfileRepository
        extends JpaRepository<CandidateHorseProfile, Long> {

    Optional<CandidateHorseProfile> findByAdmissionId(Long admissionId);

    List<CandidateHorseProfile> findByAdmissionIdIn(Collection<Long> admissionIds);
}
