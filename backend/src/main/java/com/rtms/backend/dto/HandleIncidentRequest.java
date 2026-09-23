package com.rtms.backend.dto;

import com.rtms.backend.enums.IncidentStatus;

/**
 * Thú y / Quản lý chuyển trạng thái một báo cáo sự cố.
 *
 * Chuyển trạng thái hợp lệ:
 *   REPORTED  -> IN_REVIEW  (tiếp nhận)
 *   REPORTED  -> DISMISSED  (báo động giả, không cần khám)
 *   IN_REVIEW -> RESOLVED   (đã khám và xử lý xong)
 *   IN_REVIEW -> DISMISSED
 */
public class HandleIncidentRequest {

    private IncidentStatus status;
    private String handlerNote;

    public IncidentStatus getStatus() { return status; }
    public void setStatus(IncidentStatus status) { this.status = status; }

    public String getHandlerNote() { return handlerNote; }
    public void setHandlerNote(String handlerNote) { this.handlerNote = handlerNote; }
}
