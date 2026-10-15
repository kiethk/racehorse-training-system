package com.rtms.backend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public class BatchCompleteCareScheduleRequest {
    @NotEmpty
    @Valid
    private List<BatchCareScheduleRowRequest> items;

    public BatchCompleteCareScheduleRequest() {}

    public BatchCompleteCareScheduleRequest(List<BatchCareScheduleRowRequest> items) {
        this.items = items;
    }

    public List<BatchCareScheduleRowRequest> getItems() { return items; }
    public void setItems(List<BatchCareScheduleRowRequest> items) { this.items = items; }
}
