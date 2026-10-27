package com.rtms.backend.horse.repository;

import com.rtms.backend.admission.enums.AdmissionDocumentType;
import com.rtms.backend.horse.entity.HorseEditRequestDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface HorseEditRequestDocumentRepository extends JpaRepository<HorseEditRequestDocument, Long> {

    List<HorseEditRequestDocument> findByRequestIdOrderByIdAsc(Long requestId);

    List<HorseEditRequestDocument> findByRequestIdIn(Collection<Long> requestIds);

    /** The horse's official documents that came from approved edit requests. */
    List<HorseEditRequestDocument> findByHorseIdAndCurrentTrue(Long horseId);

    Optional<HorseEditRequestDocument> findFirstByHorseIdAndDocumentTypeAndCurrentTrue(
            Long horseId, AdmissionDocumentType documentType);

    List<HorseEditRequestDocument> findByHorseIdInAndDocumentTypeAndCurrentTrue(
            Collection<Long> horseIds, AdmissionDocumentType documentType);
}
