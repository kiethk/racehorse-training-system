package com.rtms.backend.medical.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * Biểu đồ sức khỏe trên dashboard. Khoảng ngày đi kèm để giao diện ghi đúng phạm vi và vẽ cả những ngày không ai đo.
 * (The health chart on the dashboard. The date range comes with it so the UI states the exact period and also draws
 * the days nobody measured.)
 *
 * @param days chỉ những ngày có lần đo, ngày tăng dần (only days with readings, ascending)
 */
public record HealthTrendResponse(LocalDate fromDate,
                                  LocalDate toDate,
                                  List<HealthDayResponse> days) {
}
