package com.rtms.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

public class CreateRaceRegistrationRequest {

    @NotNull(message = "Chiến mã không được để trống")
    private Long horseId;

    @NotBlank(message = "Tên cuộc đua không được để trống")
    @Size(max = 255, message = "Tên cuộc đua tối đa 255 ký tự")
    private String raceName;

    @NotBlank(message = "Hạng mục cuộc đua không được để trống")
    @Size(max = 255, message = "Hạng mục cuộc đua tối đa 255 ký tự")
    private String raceCategory;

    @NotBlank(message = "Địa điểm không được để trống")
    @Size(max = 255, message = "Địa điểm tối đa 255 ký tự")
    private String location;

    @NotNull(message = "Ngày tổ chức không được để trống")
    private LocalDate eventDate;

    private LocalTime eventTime;

    @Size(max = 255, message = "Đơn vị tổ chức tối đa 255 ký tự")
    private String organizer;

    @Size(max = 1000, message = "Link nguồn tối đa 1000 ký tự")
    private String sourceUrl;

    private LocalDate nominationDeadline;

    @Positive(message = "Cự ly phải là số dương")
    private BigDecimal distanceMeters;

    @Size(max = 50, message = "Mặt sân tối đa 50 ký tự")
    private String trackType;

    private String prizeDetails;

    @NotBlank(message = "Lý do đề cử không được để trống")
    private String selectionReason;

    private String trainerNotes;

    public Long getHorseId() {
        return horseId;
    }

    public void setHorseId(Long horseId) {
        this.horseId = horseId;
    }

    public String getRaceName() {
        return raceName;
    }

    public void setRaceName(String raceName) {
        this.raceName = raceName;
    }

    public String getRaceCategory() {
        return raceCategory;
    }

    public void setRaceCategory(String raceCategory) {
        this.raceCategory = raceCategory;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public LocalDate getEventDate() {
        return eventDate;
    }

    public void setEventDate(LocalDate eventDate) {
        this.eventDate = eventDate;
    }

    public LocalTime getEventTime() {
        return eventTime;
    }

    public void setEventTime(LocalTime eventTime) {
        this.eventTime = eventTime;
    }

    public String getOrganizer() {
        return organizer;
    }

    public void setOrganizer(String organizer) {
        this.organizer = organizer;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public void setSourceUrl(String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }

    public LocalDate getNominationDeadline() {
        return nominationDeadline;
    }

    public void setNominationDeadline(LocalDate nominationDeadline) {
        this.nominationDeadline = nominationDeadline;
    }

    public BigDecimal getDistanceMeters() {
        return distanceMeters;
    }

    public void setDistanceMeters(BigDecimal distanceMeters) {
        this.distanceMeters = distanceMeters;
    }

    public String getTrackType() {
        return trackType;
    }

    public void setTrackType(String trackType) {
        this.trackType = trackType;
    }

    public String getPrizeDetails() {
        return prizeDetails;
    }

    public void setPrizeDetails(String prizeDetails) {
        this.prizeDetails = prizeDetails;
    }

    public String getSelectionReason() {
        return selectionReason;
    }

    public void setSelectionReason(String selectionReason) {
        this.selectionReason = selectionReason;
    }

    public String getTrainerNotes() {
        return trainerNotes;
    }

    public void setTrainerNotes(String trainerNotes) {
        this.trainerNotes = trainerNotes;
    }
}
