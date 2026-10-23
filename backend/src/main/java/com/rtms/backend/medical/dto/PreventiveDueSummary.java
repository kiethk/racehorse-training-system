package com.rtms.backend.medical.dto;

/** Số liệu cho các thẻ trên tab "To do": mục quá hạn / chưa có dữ liệu, mục sắp đến hạn, số ngựa cần xử lý. */
public record PreventiveDueSummary(int overdue, int dueSoon, int horses) {
}
