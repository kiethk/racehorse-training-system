package com.rtms.backend.dto;

import com.rtms.backend.enums.AdmissionDocumentType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record AdmissionDocumentMetadataRequest (
    @NotNull AdmissionDocumentType documentType,
    LocalDate recordDate,
    @Size(max = 2000) String note) {

}
