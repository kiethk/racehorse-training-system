package com.rtms.backend.service;

import com.rtms.backend.dto.CreateGroomIncidentReportRequest;
import com.rtms.backend.dto.HandleIncidentRequest;
import com.rtms.backend.entity.GroomIncidentReport;
import com.rtms.backend.enums.IncidentSeverity;
import com.rtms.backend.enums.IncidentStatus;
import com.rtms.backend.repository.GroomIncidentReportRepository;
import com.rtms.backend.repository.HorseRepository;
import com.rtms.backend.security.AuthenticatedUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class GroomIncidentReportService {

    private final GroomIncidentReportRepository incidentReportRepository;
    private final HorseRepository horseRepository;

    public GroomIncidentReportService(GroomIncidentReportRepository incidentReportRepository,
                                      HorseRepository horseRepository) {
        this.incidentReportRepository = incidentReportRepository;
        this.horseRepository = horseRepository;
    }

    @Transactional
    public GroomIncidentReport createReport(CreateGroomIncidentReportRequest request, AuthenticatedUser currentUser) {
        if (request.getHorseId() == null) {
            throw new RuntimeException("Horse ID is required");
        }
        horseRepository.findById(request.getHorseId())
                .orElseThrow(() -> new RuntimeException("Horse not found with id: " + request.getHorseId()));

        if (request.getTitle() == null || request.getTitle().trim().isEmpty()) {
            throw new RuntimeException("Title is required");
        }

        if (request.getDescription() == null || request.getDescription().trim().isEmpty()) {
            throw new RuntimeException("Description is required");
        }

        IncidentSeverity severity = request.getSeverity() != null ? request.getSeverity() : IncidentSeverity.MEDIUM;

        GroomIncidentReport report = new GroomIncidentReport(
                currentUser.getUserId(),
                request.getHorseId(),
                request.getTitle().trim(),
                request.getDescription().trim(),
                request.getImageUrl(),
                severity
        );

        return incidentReportRepository.save(report);
    }

    public List<GroomIncidentReport> getReports(Long horseId,
                                                IncidentStatus status,
                                                AuthenticatedUser currentUser) {
        List<GroomIncidentReport> base;

        if (horseId != null) {
            base = incidentReportRepository.findByHorseId(horseId);
            if ("GROOM".equalsIgnoreCase(currentUser.getRole())) {
                base = base.stream()
                        .filter(r -> currentUser.getUserId().equals(r.getGroomId()))
                        .toList();
            }
        } else if ("GROOM".equalsIgnoreCase(currentUser.getRole())) {
            base = incidentReportRepository.findByGroomId(currentUser.getUserId());
        } else {
            base = incidentReportRepository.findAllByOrderByReportedAtDesc();
        }

        if (status == null) {
            return base;
        }
        return base.stream().filter(r -> r.getStatus() == status).toList();
    }

    /**
     * Tiếp nhận / kết luận một báo cáo sự cố.
     *
     * PHẠM VI: phần nối sang bệnh án (health_records.source_incident_id) thuộc
     * module Thú y. Ở đây ta chỉ quản lý VÒNG ĐỜI của báo cáo. Khi đồng đội làm
     * phần bệnh án, họ gọi API này với status = RESOLVED sau khi lưu bệnh án.
     */
    @Transactional
    public GroomIncidentReport handleReport(Long id,
                                            HandleIncidentRequest request,
                                            AuthenticatedUser currentUser) {
        GroomIncidentReport report = incidentReportRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy báo cáo sự cố #" + id));

        IncidentStatus next = request.getStatus();
        if (next == null) {
            throw new IllegalArgumentException("Thiếu trạng thái mới!");
        }

        IncidentStatus current = report.getStatus();
        boolean valid = switch (current) {
            case REPORTED  -> next == IncidentStatus.IN_REVIEW || next == IncidentStatus.DISMISSED;
            case IN_REVIEW -> next == IncidentStatus.RESOLVED  || next == IncidentStatus.DISMISSED;
            case RESOLVED, DISMISSED -> false;   // trạng thái kết thúc
        };

        if (!valid) {
            throw new IllegalStateException(String.format(
                    "Không thể chuyển báo cáo từ %s sang %s!", current, next));
        }

        report.setStatus(next);
        report.setHandledById(currentUser.getUserId());
        report.setHandledAt(LocalDateTime.now());
        if (request.getHandlerNote() != null && !request.getHandlerNote().isBlank()) {
            report.setHandlerNote(request.getHandlerNote().trim());
        }

        return incidentReportRepository.save(report);
    }

    public GroomIncidentReport getReportById(Long id) {
        return incidentReportRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Incident report not found with id: " + id));
    }
}
