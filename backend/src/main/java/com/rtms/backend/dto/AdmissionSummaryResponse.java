package com.rtms.backend.dto;

import com.rtms.backend.enums.AdmissionStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class AdmissionSummaryResponse {

    private Long admissionId;
    private AdmissionStatus status;
    private String candidateName;
    private String breed;
    private LocalDate dateOfBirth;
    private LocalDateTime submittedAt;
    private Long quarantineStallId;
    private String quarantineStallCode;

    public AdmissionSummaryResponse(
            Long admissionId,
            AdmissionStatus status,
            String candidateName,
            String breed,
            LocalDate dateOfBirth,
            LocalDateTime submittedAt,
            Long quarantineStallId) {
        this(admissionId, status, candidateName, breed, dateOfBirth, submittedAt, quarantineStallId, null);
    }

    public AdmissionSummaryResponse(
            Long admissionId,
            AdmissionStatus status,
            String candidateName,
            String breed,
            LocalDate dateOfBirth,
            LocalDateTime submittedAt,
            Long quarantineStallId,
            String quarantineStallCode) {
        this.admissionId = admissionId;
        this.status = status;
        this.candidateName = candidateName;
        this.breed = breed;
        this.dateOfBirth = dateOfBirth;
        this.submittedAt = submittedAt;
        this.quarantineStallId = quarantineStallId;
        this.quarantineStallCode = quarantineStallCode;
    }

    public Long getAdmissionId() {
        return admissionId;
    }

    public AdmissionStatus getStatus() {
        return status;
    }

    public String getCandidateName() {
        return candidateName;
    }

    public String getBreed() {
        return breed;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public Long getQuarantineStallId() {
        return quarantineStallId;
    }

    /**
     * Mã chuồng cách ly, ví dụ "Q3".
     *
     * Vì sao không dùng quarantineStallId: đó là khoá nội bộ, không in trên
     * biển chuồng. Trainer cầm điện thoại xuống khu cách ly cần mã người đọc
     * được. Không có trường này thì phải mở chi tiết từng đơn mới biết đi đâu.
     */
    public String getQuarantineStallCode() {
        return quarantineStallCode;
    }
}