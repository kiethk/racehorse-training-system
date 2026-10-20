package com.rtms.backend.admission.dto;

import com.rtms.backend.admission.enums.AdmissionDocumentType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record AdmissionDocumentMetadataRequest (
    @NotNull AdmissionDocumentType documentType,
    LocalDate recordDate,
    @Size(max = 2000) String note) {

}
