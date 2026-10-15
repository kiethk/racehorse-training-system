package com.rtms.backend.incident.service;

import com.rtms.backend.common.storage.FileStorageService;
import com.rtms.backend.horse.entity.Horse;
import com.rtms.backend.horse.repository.HorseRepository;
import com.rtms.backend.incident.dto.CreateIncidentReportRequest;
import com.rtms.backend.incident.dto.HandleIncidentRequest;
import com.rtms.backend.incident.entity.HorseIncidentReport;
import com.rtms.backend.incident.enums.IncidentSeverity;
import com.rtms.backend.incident.enums.IncidentStatus;
import com.rtms.backend.incident.repository.HorseIncidentReportRepository;
import com.rtms.backend.medical.entity.CareSchedule;
import com.rtms.backend.medical.enums.CareScheduleStatus;
import com.rtms.backend.medical.enums.CareType;
import com.rtms.backend.medical.repository.CareScheduleRepository;
import com.rtms.backend.medical.service.CareScheduleService;
import com.rtms.backend.security.AuthenticatedUser;
import com.rtms.backend.training.service.TrainingDecisionService;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
public class HorseIncidentReportService {

    private final HorseIncidentReportRepository incidentReportRepository;
    private final HorseRepository horseRepository;
    private final FileStorageService fileStorage;
    private final CareScheduleRepository careScheduleRepository;
    private final CareScheduleService careScheduleService;
    private final TrainingDecisionService trainingDecisionService;

    public HorseIncidentReportService(HorseIncidentReportRepository incidentReportRepository,
                                      HorseRepository horseRepository,
                                      FileStorageService fileStorage,
                                      CareScheduleRepository careScheduleRepository,
                                      CareScheduleService careScheduleService,
                                      TrainingDecisionService trainingDecisionService) {
        this.incidentReportRepository = incidentReportRepository;
        this.horseRepository = horseRepository;
        this.fileStorage = fileStorage;
        this.careScheduleRepository = careScheduleRepository;
        this.careScheduleService = careScheduleService;
        this.trainingDecisionService = trainingDecisionService;
    }

    @Transactional
    public HorseIncidentReport createReport(CreateIncidentReportRequest request, AuthenticatedUser currentUser) {
        if (request.getHorseId() == null) {
            throw new RuntimeException("Horse ID is required");
        }
        Horse horse = horseRepository.findByIdForUpdate(request.getHorseId())
                .orElseThrow(() -> new RuntimeException("Horse not found with id: " + request.getHorseId()));

        if (request.getTitle() == null || request.getTitle().trim().isEmpty()) {
            throw new RuntimeException("Title is required");
        }

        if (request.getDescription() == null || request.getDescription().trim().isEmpty()) {
            throw new RuntimeException("Description is required");
        }

        IncidentSeverity severity = request.getSeverity() != null ? request.getSeverity() : IncidentSeverity.MEDIUM;

        HorseIncidentReport report = new HorseIncidentReport(
                currentUser.getUserId(),
                request.getHorseId(),
                request.getTitle().trim(),
                request.getDescription().trim(),
                request.getImageUrl(),
                severity
        );

        HorseIncidentReport saved = incidentReportRepository.save(report);

        List<CareScheduleStatus> activeStatuses = List.of(
                CareScheduleStatus.REQUESTED, CareScheduleStatus.SCHEDULED, CareScheduleStatus.IN_PROGRESS);
        CareSchedule active = careScheduleRepository
                .findFirstByHorseIdAndCareTypeAndStatusInOrderByCreatedAtDesc(
                        horse.getId(), CareType.URGENT, activeStatuses)
                .orElse(null);
        if (active == null) {
            String scheduleDescription = (report.getDescription() != null && !report.getDescription().trim().isEmpty())
                    ? report.getDescription().trim()
                    : "Incident reported";
            careScheduleService.createSchedule(horse.getId(), CareType.URGENT, scheduleDescription, null, saved.getId());
            active = careScheduleRepository
                    .findFirstByHorseIdAndCareTypeAndStatusInOrderByCreatedAtDesc(
                            horse.getId(), CareType.URGENT, activeStatuses)
                    .orElseThrow(() -> new IllegalStateException("Urgent schedule was not created"));
        } else {
            // Đã có ca khẩn cấp đang mở: báo cáo mới chỉ gắn vào ca đó, ngựa vẫn bị chặn tập.
            trainingDecisionService.block(horse, "Urgent veterinary care pending");
        }
        saved.setCareScheduleId(active.getId());
        return incidentReportRepository.save(saved);
    }

    public List<HorseIncidentReport> getReports(Long horseId,
                                                IncidentStatus status,
                                                AuthenticatedUser currentUser) {
        List<HorseIncidentReport> base;

        if (horseId != null) {
            base = incidentReportRepository.findByHorseId(horseId);
            if ("GROOM".equalsIgnoreCase(currentUser.getRole())) {
                base = base.stream()
                        .filter(r -> currentUser.getUserId().equals(r.getReportedById()))
                        .toList();
            }
        } else if ("GROOM".equalsIgnoreCase(currentUser.getRole())) {
            base = incidentReportRepository.findByReportedById(currentUser.getUserId());
        } else {
            base = incidentReportRepository.findAll();
        }

        // Sắp xếp mới nhất lên đầu
        base = base.stream()
                .sorted((a, b) -> {
                    LocalDateTime ta = a.getReportedAt() != null ? a.getReportedAt() : LocalDateTime.MIN;
                    LocalDateTime tb = b.getReportedAt() != null ? b.getReportedAt() : LocalDateTime.MIN;
                    return tb.compareTo(ta);
                })
                .toList();

        if (status == null) {
            return base;
        }
        return base.stream().filter(r -> r.getStatus() == status).toList();
    }

    @Transactional
    public HorseIncidentReport handleReport(Long id,
                                            HandleIncidentRequest request,
                                            AuthenticatedUser currentUser) {
        HorseIncidentReport report = incidentReportRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Incident report not found with id: " + id));

        IncidentStatus next = request.getStatus();
        if (next == null) {
            throw new IllegalArgumentException("The new status is required");
        }

        IncidentStatus current = report.getStatus();
        boolean valid = switch (current) {
            case REPORTED -> next == IncidentStatus.IN_REVIEW || next == IncidentStatus.DISMISSED;
            case IN_REVIEW -> next == IncidentStatus.RESOLVED || next == IncidentStatus.DISMISSED;
            case RESOLVED, DISMISSED -> false;
        };

        if (!valid) {
            throw new IllegalStateException(String.format(
                    "An incident report cannot move from %s to %s", current, next));
        }

        report.setStatus(next);
        report.setHandledById(currentUser.getUserId());
        report.setHandledAt(LocalDateTime.now());
        if (request.getHandlerNote() != null && !request.getHandlerNote().isBlank()) {
            report.setHandlerNote(request.getHandlerNote().trim());
        }

        return incidentReportRepository.save(report);
    }

    public HorseIncidentReport getReportById(Long id, AuthenticatedUser currentUser) {
        HorseIncidentReport report = incidentReportRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Incident report not found with id: " + id));
        if ("GROOM".equalsIgnoreCase(currentUser.getRole())
                && !currentUser.getUserId().equals(report.getReportedById())) {
            throw new AccessDeniedException("You can only view incident reports you submitted");
        }
        return report;
    }

    @Transactional
    public HorseIncidentReport attachImage(Long reportId,
                                           MultipartFile file,
                                           AuthenticatedUser currentUser) {
        HorseIncidentReport report = incidentReportRepository.findById(reportId)
                .orElseThrow(() -> new RuntimeException("Incident report not found with id: " + reportId));

        if ("GROOM".equalsIgnoreCase(currentUser.getRole())
                && !currentUser.getUserId().equals(report.getReportedById())) {
            throw new AccessDeniedException("You can only attach an image to a report you submitted");
        }

        String mediaType = file.getContentType() == null
                ? "" : file.getContentType().toLowerCase(Locale.ROOT);
        if (!mediaType.startsWith("image/")) {
            throw new IllegalArgumentException("Incident reports accept images only (JPEG, PNG, WebP)");
        }

        String oldKey = report.getImageUrl();
        String newKey = fileStorage.store(file, "rtms/incidents/" + reportId);

        try {
            report.setImageUrl(newKey);
            HorseIncidentReport saved = incidentReportRepository.save(report);
            fileStorage.delete(oldKey);
            return saved;
        } catch (RuntimeException ex) {
            fileStorage.delete(newKey);
            throw ex;
        }
    }

    public Resource loadImage(Long reportId, AuthenticatedUser currentUser) {
        HorseIncidentReport report = getReportById(reportId, currentUser);
        if (report.getImageUrl() == null) {
            throw new RuntimeException("Incident report #" + reportId + " has no image attached");
        }
        return fileStorage.load(report.getImageUrl());
    }

    public MediaType imageMediaType(Long reportId, AuthenticatedUser currentUser) {
        return fileStorage.mediaType(getReportById(reportId, currentUser).getImageUrl());
    }
}
