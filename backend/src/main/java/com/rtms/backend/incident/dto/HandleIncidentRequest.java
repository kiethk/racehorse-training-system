package com.rtms.backend.incident.dto;

import com.rtms.backend.incident.enums.IncidentStatus;

/**
 * Thú y / Quản lý chuyển trạng thái một báo cáo sự cố.
 */
public class HandleIncidentRequest {

    private IncidentStatus status;
    private String handlerNote;

    public IncidentStatus getStatus() {
        return status;
    }

    public void setStatus(IncidentStatus status) {
        this.status = status;
    }

    public String getHandlerNote() {
        return handlerNote;
    }

    public void setHandlerNote(String handlerNote) {
        this.handlerNote = handlerNote;
    }
}
