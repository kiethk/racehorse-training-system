package com.rtms.backend.dto;

import com.rtms.backend.enums.TaskSource;

import java.time.LocalTime;

/**
 * Một dòng trên màn hình "Today Checklist" của Groom.
 * Gom từ 3 nguồn dữ liệu độc lập, sắp xếp tuần tự theo giờ từ 05:00 tới 18:00.
 */
public class TodayTaskItemResponse {

    private TaskSource source;

    /** Id trong bảng gốc — để frontend bấm xem chi tiết hoặc gọi API tick hoàn thành. */
    private Long refId;

    private LocalTime startTime;

    /** Chỉ WORKOUT mới có giờ kết thúc. SOP và thú y là mốc đơn. */
    private LocalTime endTime;

    private Long horseId;
    private String horseName;
    private String title;
    private String note;

    /** Trạng thái gốc: is_completed (SOP) / WorkoutStatus / status lịch thú y. */
    private String status;

    /** Chỉ nguồn SOP mới tick được. Workout do Trainer đóng, thú y do Vet đóng. */
    private boolean actionable;

    public TodayTaskItemResponse(TaskSource source, Long refId, LocalTime startTime,
                                 LocalTime endTime, Long horseId, String horseName,
                                 String title, String note, String status, boolean actionable) {
        this.source = source;
        this.refId = refId;
        this.startTime = startTime;
        this.endTime = endTime;
        this.horseId = horseId;
        this.horseName = horseName;
        this.title = title;
        this.note = note;
        this.status = status;
        this.actionable = actionable;
    }

    public TaskSource getSource() { return source; }
    public Long getRefId() { return refId; }
    public LocalTime getStartTime() { return startTime; }
    public LocalTime getEndTime() { return endTime; }
    public Long getHorseId() { return horseId; }
    public String getHorseName() { return horseName; }
    public String getTitle() { return title; }
    public String getNote() { return note; }
    public String getStatus() { return status; }
    public boolean isActionable() { return actionable; }
}
