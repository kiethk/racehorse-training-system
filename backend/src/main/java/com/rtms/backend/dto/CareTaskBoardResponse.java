package com.rtms.backend.dto;

import java.util.List;

public class CareTaskBoardResponse {
    private List<CareTaskItemResponse> items;
    private CareTasksSummaryResponse summary;

    public CareTaskBoardResponse() {}

    public CareTaskBoardResponse(List<CareTaskItemResponse> items, CareTasksSummaryResponse summary) {
        this.items = items;
        this.summary = summary;
    }

    public List<CareTaskItemResponse> getItems() { return items; }
    public void setItems(List<CareTaskItemResponse> items) { this.items = items; }
    public CareTasksSummaryResponse getSummary() { return summary; }
    public void setSummary(CareTasksSummaryResponse summary) { this.summary = summary; }
}
