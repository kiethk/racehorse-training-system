package com.rtms.backend.service;

import com.rtms.backend.dto.CreateGroomIncidentReportRequest;
import com.rtms.backend.dto.HandleIncidentRequest;
import com.rtms.backend.entity.GroomIncidentReport;
import com.rtms.backend.enums.IncidentSeverity;
import com.rtms.backend.enums.IncidentStatus;
import com.rtms.backend.repository.GroomIncidentReportRepository;
import com.rtms.backend.repository.HorseRepository;
import com.rtms.backend.security.AuthenticatedUser;
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
public class GroomIncidentReportService {

    private final GroomIncidentReportRepository incidentReportRepository;
    private final HorseRepository horseRepository;
    private final AdmissionFileStorage fileStorage;

    public GroomIncidentReportService(GroomIncidentReportRepository incidentReportRepository,
                                      HorseRepository horseRepository,
                                      AdmissionFileStorage fileStorage) {
        this.incidentReportRepository = incidentReportRepository;
        this.horseRepository = horseRepository;
        this.fileStorage = fileStorage;
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

    /**
     * Đính ảnh vào một báo cáo đã tạo.
     *
     * Vì sao tách làm HAI BƯỚC (tạo báo cáo -> rồi tải ảnh) mà không gộp:
     * cần id của báo cáo để biết ảnh thuộc về ai. Đây đúng cách luồng Owner
     * đang làm — tạo đơn rồi mới tải giấy tờ.
     *
     * Gọi lại lần nữa sẽ THAY ảnh cũ và xoá file cũ khỏi đĩa, tránh rác.
     */
    @Transactional
    public GroomIncidentReport attachImage(Long reportId,
                                           MultipartFile file,
                                           AuthenticatedUser currentUser) {

        GroomIncidentReport report = incidentReportRepository.findById(reportId)
                .orElseThrow(() -> new RuntimeException(
                        "Không tìm thấy báo cáo sự cố #" + reportId));

        // Chỉ người gửi báo cáo mới được đính ảnh.
        // Thú y xem được nhưng không sửa được bằng chứng của Groom.
        if ("GROOM".equalsIgnoreCase(currentUser.getRole())
                && !currentUser.getUserId().equals(report.getGroomId())) {
            throw new AccessDeniedException(
                    "Bạn chỉ được đính ảnh vào báo cáo do chính mình gửi!");
        }

        // AdmissionFileStorage nhận cả PDF. Báo cáo sự cố thì chỉ nhận ảnh —
        // chặn ở đây trước khi ghi xuống đĩa.
        String mediaType = file.getContentType() == null
                ? "" : file.getContentType().toLowerCase(Locale.ROOT);
        if (!mediaType.startsWith("image/")) {
            throw new IllegalArgumentException(
                    "Chỉ nhận ảnh (JPEG, PNG, WebP) cho báo cáo sự cố!");
        }

        String oldKey = report.getImageUrl();
        String newKey = fileStorage.store(file);   // tự kiểm dung lượng + định dạng

        try {
            report.setImageUrl(newKey);
            GroomIncidentReport saved = incidentReportRepository.save(report);
            fileStorage.deleteIfLocal(oldKey);     // dọn ảnh cũ SAU khi ghi DB thành công
            return saved;
        } catch (RuntimeException ex) {
            fileStorage.deleteIfLocal(newKey);     // ghi DB hỏng -> không để lại file mồ côi
            throw ex;
        }
    }

    /**
     * Nạp file ảnh để trả về cho trình duyệt.
     *
     * fileStorage.load() tự ném 404 nếu khoá không bắt đầu bằng "local:",
     * tức là ảnh được lưu ngoài hệ thống — khi đó frontend dùng thẳng imageUrl.
     */
    public Resource loadImage(Long reportId) {
        GroomIncidentReport report = getReportById(reportId);
        if (report.getImageUrl() == null) {
            throw new RuntimeException("Báo cáo #" + reportId + " không có ảnh đính kèm");
        }
        return fileStorage.load(report.getImageUrl());
    }

    /** Suy kiểu nội dung từ đuôi khoá lưu trữ, để trình duyệt hiển thị đúng. */
    public MediaType imageMediaType(Long reportId) {
        String key = getReportById(reportId).getImageUrl();
        String lower = key == null ? "" : key.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".png"))  return MediaType.IMAGE_PNG;
        if (lower.endsWith(".webp")) return MediaType.valueOf("image/webp");
        return MediaType.IMAGE_JPEG;
    }
}

