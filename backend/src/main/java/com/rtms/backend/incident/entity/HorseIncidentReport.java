package com.rtms.backend.incident.entity;

import com.rtms.backend.incident.enums.IncidentStatus;
import com.rtms.backend.incident.enums.IncidentType;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "horse_incident_reports")
public class HorseIncidentReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reported_by_id", nullable = false)
    private Long reportedById;

    @Column(name = "horse_id", nullable = false)
    private Long horseId;

    @Column(name = "care_schedule_id")
    private Long careScheduleId;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", length = 50)
    private IncidentType incidentType = IncidentType.PROBLEM;

    @Column(name = "reported_at")
    private LocalDateTime reportedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private IncidentStatus status = IncidentStatus.REPORTED;

    /** Thú y (hoặc Quản lý) đã tiếp nhận sự cố này. */
    @Column(name = "handled_by_id")
    private Long handledById;

    @Column(name = "handled_at")
    private LocalDateTime handledAt;

    @Column(name = "handler_note", columnDefinition = "TEXT")
    private String handlerNote;

    /** Tên vet được giao / đã xử lý (handledById). Chỉ để trả ra API, không lưu DB. */
    @Transient
    private String handlerName;

    /** Tên người gửi báo cáo (reportedById). Chỉ để trả ra API, không lưu DB. (Reporter's name, API only.) */
    @Transient
    private String reportedByName;

    public String getReportedByName() {
        return reportedByName;
    }

    public void setReportedByName(String reportedByName) {
        this.reportedByName = reportedByName;
    }

    /** Tên ngựa, vì Groom không còn thấy ngựa đã rời chuồng của mình. Chỉ để trả ra API, không lưu DB. */
    @Transient
    private String horseName;

    public String getHorseName() {
        return horseName;
    }

    public void setHorseName(String horseName) {
        this.horseName = horseName;
    }

    public HorseIncidentReport() {
    }

    public HorseIncidentReport(Long reportedById, Long horseId, String title, String description,
                               IncidentType incidentType) {
        this.reportedById = reportedById;
        this.horseId = horseId;
        this.title = title;
        this.description = description;
        this.incidentType = incidentType != null ? incidentType : IncidentType.PROBLEM;
        this.reportedAt = LocalDateTime.now();
        this.status = IncidentStatus.REPORTED;
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getReportedById() {
        return reportedById;
    }

    public void setReportedById(Long reportedById) {
        this.reportedById = reportedById;
    }

    /** Compatibility helper for older Groom-specific clients/logic. */
    public Long getGroomId() {
        return reportedById;
    }

    public void setGroomId(Long groomId) {
        this.reportedById = groomId;
    }

    public Long getHorseId() {
        return horseId;
    }

    public void setHorseId(Long horseId) {
        this.horseId = horseId;
    }

    public Long getCareScheduleId() {
        return careScheduleId;
    }

    public void setCareScheduleId(Long careScheduleId) {
        this.careScheduleId = careScheduleId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public IncidentType getIncidentType() {
        return incidentType;
    }

    public void setIncidentType(IncidentType incidentType) {
        this.incidentType = incidentType;
    }

    public LocalDateTime getReportedAt() {
        return reportedAt;
    }

    public void setReportedAt(LocalDateTime reportedAt) {
        this.reportedAt = reportedAt;
    }

    public IncidentStatus getStatus() {
        return status;
    }

    public void setStatus(IncidentStatus status) {
        this.status = status;
    }

    public Long getHandledById() {
        return handledById;
    }

    public void setHandledById(Long handledById) {
        this.handledById = handledById;
    }

    public LocalDateTime getHandledAt() {
        return handledAt;
    }

    public void setHandledAt(LocalDateTime handledAt) {
        this.handledAt = handledAt;
    }

    public String getHandlerNote() {
        return handlerNote;
    }

    public void setHandlerNote(String handlerNote) {
        this.handlerNote = handlerNote;
    }

    public String getHandlerName() {
        return handlerName;
    }

    public void setHandlerName(String handlerName) {
        this.handlerName = handlerName;
    }
}
