package com.rtms.backend.dto;

import com.rtms.backend.enums.AdmissionStatus;

public class AdmissionSummaryResponse {

    private final Long id;
    private final AdmissionStatus status;
    private final String candidateName;
    private final String candidateBreed;
    private final String quarantineStallCode;

    public AdmissionSummaryResponse(
            Long id,
            AdmissionStatus status,
            String candidateName,
            String candidateBreed,
            String quarantineStallCode) {
        this.id = id;
        this.status = status;
        this.candidateName = candidateName;
        this.candidateBreed = candidateBreed;
        this.quarantineStallCode = quarantineStallCode;
    }

    public Long getId() {
        return id;
    }

    public AdmissionStatus getStatus() {
        return status;
    }

    public String getCandidateName() {
        return candidateName;
    }

    public String getCandidateBreed() {
        return candidateBreed;
    }

    public String getQuarantineStallCode() {
        return quarantineStallCode;
    }
}
