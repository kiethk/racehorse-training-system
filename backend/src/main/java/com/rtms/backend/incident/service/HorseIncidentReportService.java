package com.rtms.backend.incident.service;

import com.rtms.backend.common.storage.FileStorageService;
import com.rtms.backend.config.ApiException;
import com.rtms.backend.horse.entity.Horse;
import com.rtms.backend.training.enums.TrainingDecision;
import com.rtms.backend.horse.repository.HorseRepository;
import com.rtms.backend.identity.entity.User;
import com.rtms.backend.identity.repository.UserRepository;
import com.rtms.backend.incident.dto.CreateIncidentReportRequest;
import com.rtms.backend.incident.dto.HandleIncidentRequest;
import com.rtms.backend.incident.entity.HorseIncidentReport;
import com.rtms.backend.incident.enums.IncidentType;
import com.rtms.backend.incident.enums.IncidentStatus;
import com.rtms.backend.incident.repository.HorseIncidentReportRepository;
import com.rtms.backend.medical.entity.CareSchedule;
import com.rtms.backend.medical.enums.CareType;
import com.rtms.backend.medical.repository.CareScheduleRepository;
import com.rtms.backend.medical.service.CareScheduleService;
import com.rtms.backend.security.AuthenticatedUser;
import com.rtms.backend.stable.repository.StableStallRepository;
import com.rtms.backend.training.service.TrainingDecisionService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class HorseIncidentReportService {

    private final HorseIncidentReportRepository incidentReportRepository;
    private final HorseRepository horseRepository;
    private final FileStorageService fileStorage;
    private final CareScheduleRepository careScheduleRepository;
    private final CareScheduleService careScheduleService;
    private final TrainingDecisionService trainingDecisionService;
    private final UserRepository userRepository;
    private final StableStallRepository stableStallRepository;

    public HorseIncidentReportService(HorseIncidentReportRepository incidentReportRepository,
                                      HorseRepository horseRepository,
                                      FileStorageService fileStorage,
                                      CareScheduleRepository careScheduleRepository,
                                      CareScheduleService careScheduleService,
                                      TrainingDecisionService trainingDecisionService,
                                      UserRepository userRepository,
                                      StableStallRepository stableStallRepository) {
        this.incidentReportRepository = incidentReportRepository;
        this.horseRepository = horseRepository;
        this.fileStorage = fileStorage;
        this.careScheduleRepository = careScheduleRepository;
        this.careScheduleService = careScheduleService;
        this.trainingDecisionService = trainingDecisionService;
        this.userRepository = userRepository;
        this.stableStallRepository = stableStallRepository;
    }

    /**
     * Mức độ quyết định ca khám: LOW, MEDIUM thành ca ROUTINE, ngựa vẫn tập. HIGH, CRITICAL thành ca URGENT và chặn tập.
     * Mỗi ngựa chỉ có một ca sự cố đang mở: báo cáo mới gắn vào ca đó. Ca ROUTINE đang khám mà gặp TRAUMA thì
     * tạm dừng (vẫn mở, giữ Vet để khám tiếp sau) và mở ca URGENT song song.
     */
    @Transactional
    public HorseIncidentReport createReport(CreateIncidentReportRequest request, AuthenticatedUser currentUser) {
        Horse horse = horseRepository.findByIdForUpdate(request.getHorseId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "HORSE_NOT_FOUND", "Horse not found"));
        assertCanReport(horse, currentUser);

        HorseIncidentReport saved = incidentReportRepository.save(new HorseIncidentReport(
                currentUser.getUserId(),
                request.getHorseId(),
                request.getTitle().trim(),
                request.getDescription().trim(),
                request.getIncidentType()));

        boolean serious = saved.getIncidentType() == IncidentType.TRAUMA;

        CareSchedule openCase = careScheduleService.findOpenIncidentCase(horse.getId()).orElse(null);
        boolean escalate = serious && openCase != null && openCase.getCareType() == CareType.ROUTINE;

        CareSchedule target = openCase;
        if (escalate) {
            // Tạm dừng trước khi gán ca URGENT: Vet đang khám không bị coi là bận, và ca này không bị offload.
            careScheduleService.interruptForUrgent(openCase);
            target = createCase(horse, saved, serious, openCase.getId());
        } else if (openCase == null) {
            target = createCase(horse, saved, serious, null);
        }
        if (escalate) {
            List<HorseIncidentReport> moved = incidentReportRepository.findByCareScheduleIdOrderByReportedAtAsc(openCase.getId());
            Long newCaseId = target.getId();
            moved.forEach(report -> report.setCareScheduleId(newCaseId));
            incidentReportRepository.saveAll(moved);
        }
        saved.setCareScheduleId(target.getId());
        incidentReportRepository.save(saved);

        syncReportsWithCase(target);
        return saved;
    }

    private CareSchedule createCase(Horse horse, HorseIncidentReport report, boolean serious, Long supersededCaseId) {
        Long caseId = careScheduleService.createSchedule(horse.getId(), serious ? CareType.URGENT : CareType.ROUTINE,
                report.getDescription(), null, report.getId(), supersededCaseId).id();
        report.setCareScheduleId(caseId);
        return careScheduleRepository.findById(caseId)
                .orElseThrow(() -> new IllegalStateException("Incident case was not created"));
    }

    /** Báo cáo hiển thị đúng người đang nhận ca, kể cả báo cáo gắn vào sau khi ca đã có Vet. */
    private void syncReportsWithCase(CareSchedule incidentCase) {
        Long vetId = incidentCase.getVeterinarianId();
        LocalDateTime now = LocalDateTime.now();
        List<HorseIncidentReport> reports = incidentReportRepository.findByCareScheduleIdOrderByReportedAtAsc(incidentCase.getId());
        for (HorseIncidentReport report : reports) {
            if (Objects.equals(report.getHandledById(), vetId)) continue;
            report.setStatus(vetId != null ? IncidentStatus.IN_REVIEW : IncidentStatus.REPORTED);
            report.setHandledById(vetId);
            report.setHandledAt(vetId != null ? now : null);
        }
        incidentReportRepository.saveAll(reports);
    }

    private void assertCanReport(Horse horse, AuthenticatedUser currentUser) {
        if (horse.getCurrentStallId() == null) {
            throw new ApiException(HttpStatus.CONFLICT, "HORSE_NOT_IN_STALL",
                    "Incidents can only be reported for a horse currently in a stall");
        }
        if (isGroom(currentUser) && !stableStallRepository.existsByIdAndGroomId(horse.getCurrentStallId(), currentUser.getUserId())) {
            throw new AccessDeniedException("You can only report incidents for horses in your stalls");
        }
        if (isTrainer(currentUser) && !isTrainerOf(horse.getId(), currentUser)) {
            throw new AccessDeniedException("You can only report incidents for horses in your areas");
        }
    }

    private boolean isTrainerOf(Long horseId, AuthenticatedUser currentUser) {
        return horseRepository.findTrainerIdByHorseId(horseId).filter(currentUser.getUserId()::equals).isPresent();
    }

    private static boolean isGroom(AuthenticatedUser user) {
        return "GROOM".equalsIgnoreCase(user.getRole());
    }

    private static boolean isTrainer(AuthenticatedUser user) {
        return "HEAD_TRAINER".equalsIgnoreCase(user.getRole());
    }

    public List<HorseIncidentReport> getReports(Long horseId,
                                                IncidentStatus status,
                                                AuthenticatedUser currentUser) {
        List<HorseIncidentReport> base;

        if (isGroom(currentUser)) {
            base = incidentReportRepository.findByReportedById(currentUser.getUserId());
        } else if (isTrainer(currentUser)) {
            base = incidentReportRepository.findVisibleToTrainer(currentUser.getUserId());
        } else {
            base = horseId != null ? incidentReportRepository.findByHorseId(horseId) : incidentReportRepository.findAll();
        }
        if (horseId != null) {
            base = base.stream().filter(r -> horseId.equals(r.getHorseId())).toList();
        }

        // Sắp xếp mới nhất lên đầu
        base = base.stream()
                .sorted((a, b) -> {
                    LocalDateTime ta = a.getReportedAt() != null ? a.getReportedAt() : LocalDateTime.MIN;
                    LocalDateTime tb = b.getReportedAt() != null ? b.getReportedAt() : LocalDateTime.MIN;
                    return tb.compareTo(ta);
                })
                .toList();

        if (status != null) {
            base = base.stream().filter(r -> r.getStatus() == status).toList();
        }
        return withHorseNames(withHandlerNames(base));
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

        HorseIncidentReport saved = incidentReportRepository.save(report);
        withHandlerNames(List.of(saved));
        return saved;
    }

    public HorseIncidentReport getReportById(Long id, AuthenticatedUser currentUser) {
        HorseIncidentReport report = incidentReportRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Incident report not found with id: " + id));
        boolean ownReport = currentUser.getUserId().equals(report.getReportedById());
        if (isGroom(currentUser) && !ownReport) {
            throw new AccessDeniedException("You can only view incident reports you submitted");
        }
        if (isTrainer(currentUser) && !ownReport && !isTrainerOf(report.getHorseId(), currentUser)) {
            throw new AccessDeniedException("You can only view incident reports for horses in your areas");
        }
        withHorseNames(withHandlerNames(List.of(report)));
        return report;
    }

    @Transactional
    public HorseIncidentReport attachImage(Long reportId,
                                           MultipartFile file,
                                           AuthenticatedUser currentUser) {
        HorseIncidentReport report = incidentReportRepository.findById(reportId)
                .orElseThrow(() -> new RuntimeException("Incident report not found with id: " + reportId));

        if (!currentUser.getUserId().equals(report.getReportedById())) {
            throw new AccessDeniedException("You can only attach an image to a report you submitted");
        }
        if (report.getStatus() != IncidentStatus.REPORTED && report.getStatus() != IncidentStatus.IN_REVIEW) {
            throw new ApiException(HttpStatus.CONFLICT, "INCIDENT_CLOSED", "This incident report is already closed");
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

    private List<HorseIncidentReport> withHorseNames(List<HorseIncidentReport> reports) {
        List<Long> horseIds = reports.stream().map(HorseIncidentReport::getHorseId).filter(Objects::nonNull).distinct().toList();
        Map<Long, String> names = horseRepository.findAllById(horseIds).stream()
                .collect(Collectors.toMap(Horse::getId, Horse::getName, (a, b) -> a));
        reports.forEach(report -> report.setHorseName(names.get(report.getHorseId())));
        return reports;
    }

    /**
     * Gắn tên vet được giao để người báo biết ai đang nhận ca. Ưu tiên vet đã xử lý báo cáo
     * (handledById); nếu chưa có thì lấy vet của ca urgent mà báo cáo được gắn vào
     * (báo cáo thứ hai trên cùng ngựa dùng chung ca urgent đang mở, nên không có handledById).
     * Mỗi loại dữ liệu chỉ truy vấn một lần cho cả danh sách.
     */
    private List<HorseIncidentReport> withHandlerNames(List<HorseIncidentReport> reports) {
        List<Long> scheduleIds = reports.stream()
                .filter(report -> report.getHandledById() == null)
                .map(HorseIncidentReport::getCareScheduleId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, Long> vetBySchedule = scheduleIds.isEmpty() ? Map.of()
                : careScheduleRepository.findAllById(scheduleIds).stream()
                        .filter(schedule -> schedule.getVeterinarianId() != null)
                        .collect(Collectors.toMap(CareSchedule::getId, CareSchedule::getVeterinarianId, (a, b) -> a));

        Map<HorseIncidentReport, Long> vetByReport = new java.util.IdentityHashMap<>();
        for (HorseIncidentReport report : reports) {
            Long vetId = report.getHandledById() != null ? report.getHandledById()
                    : report.getCareScheduleId() != null ? vetBySchedule.get(report.getCareScheduleId())
                    : null;
            if (vetId != null) vetByReport.put(report, vetId);
        }
        if (vetByReport.isEmpty()) return reports;

        Map<Long, String> names = userRepository.findAllById(vetByReport.values().stream().distinct().toList()).stream()
                .collect(Collectors.toMap(User::getId, User::getFullName, (a, b) -> a));
        reports.forEach(report -> {
            Long vetId = vetByReport.get(report);
            report.setHandlerName(vetId == null ? null : names.get(vetId));
        });
        return reports;
    }
}
