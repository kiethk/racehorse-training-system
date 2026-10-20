package com.rtms.backend.admission.repository;

import com.rtms.backend.admission.entity.AdmissionDocument;
import com.rtms.backend.admission.enums.AdmissionDocumentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface AdmissionDocumentRepository
        extends JpaRepository<AdmissionDocument, Long> {

    List<AdmissionDocument> findByAdmissionId(Long admissionId);

    /** Batch lookup: all documents of a given type for a set of admission IDs. */
    List<AdmissionDocument> findByAdmissionIdInAndDocumentType(
            Collection<Long> admissionIds, AdmissionDocumentType documentType);
}