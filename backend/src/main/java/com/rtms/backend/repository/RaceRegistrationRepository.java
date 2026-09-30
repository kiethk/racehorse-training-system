package com.rtms.backend.repository;

import com.rtms.backend.entity.RaceRegistration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RaceRegistrationRepository extends JpaRepository<RaceRegistration, Long> {

    List<RaceRegistration> findByHorseId(Long horseId);

    List<RaceRegistration> findByHorseIdOrderByCreatedAtDesc(Long horseId);

    List<RaceRegistration> findByTrainerIdOrderByCreatedAtDesc(Long trainerId);

    Optional<RaceRegistration> findByIdAndTrainerId(Long id, Long trainerId);

    List<RaceRegistration> findByTrainerIdAndHorseIdOrderByCreatedAtDesc(Long trainerId, Long horseId);

    List<RaceRegistration> findAllByOrderByCreatedAtDesc();
}