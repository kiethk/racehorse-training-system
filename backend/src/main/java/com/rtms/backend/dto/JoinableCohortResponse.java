package com.rtms.backend.dto;
import com.rtms.backend.enums.TrainingDay;

import java.time.LocalDate;
import java.util.Set;

/**
 * Gợi ý nhóm để ghi danh chung, giúp tiết kiệm khe giờ vàng.
 *
 * Hệ thống chỉ GỢI Ý, không ép. Trainer nhìn waitDays (ngựa phải nằm không
 * bao lâu) đối chiếu sharedSessions (được chung bao nhiêu buổi) rồi tự quyết.
 */
public class JoinableCohortResponse {

    private String cohortStatus;          // UPCOMING | ACTIVE
    private Long courseId;
    private String courseName;

    /**
     * Ngày nên cho con ngựa mới bắt đầu.
     *  - Nhóm UPCOMING: chính là startDate của nhóm -> đồng bộ TRỌN khoá.
     *  - Nhóm ACTIVE:   ngày gần nhất nhóm quay về bài đầu khoá (i % n == 0)
     *                   -> đồng bộ từ đó tới hết khoá của nhóm.
     */
    private LocalDate suggestedStartDate;

    private Set<TrainingDay> trainingDays;
    private Integer horseCount;

    /**
     * Số buổi ĐỒNG PHA — cùng ngày, cùng bài tập.
     *
     * KHÔNG phải "số buổi chung lot". Đồng pha là ĐIỀU KIỆN CẦN để chung lot,
     * không phải bảo đảm: nhóm vẫn bị tách sang lot thứ hai nếu vượt sức chứa
     * (BR-10), hoặc nếu hai con cùng một Groom (BR-09).
     */
    private Integer sharedSessions;

    private Integer totalSessions;
    private Long waitDays;                // số ngày phải chờ tính từ hôm nay

    /**
     * Sức chứa một lot, để giao diện tự cảnh báo khi nhóm sắp tràn.
     * Trả từ backend thay vì để frontend hardcode số 6.
     */
    private Integer lotCapacity;

    private String note;

    public JoinableCohortResponse(String cohortStatus, Long courseId, String courseName,
                                  LocalDate suggestedStartDate, Set<TrainingDay> trainingDays,
                                  Integer horseCount, Integer sharedSessions,
                                  Integer totalSessions, Long waitDays,
                                  Integer lotCapacity, String note) {
        this.cohortStatus = cohortStatus;
        this.courseId = courseId;
        this.courseName = courseName;
        this.suggestedStartDate = suggestedStartDate;
        this.trainingDays = trainingDays;
        this.horseCount = horseCount;
        this.sharedSessions = sharedSessions;
        this.totalSessions = totalSessions;
        this.waitDays = waitDays;
        this.lotCapacity = lotCapacity;
        this.note = note;
    }

    public Integer getLotCapacity() { return lotCapacity; }
    public void setLotCapacity(Integer lotCapacity) { this.lotCapacity = lotCapacity; }

    public String getCohortStatus() { return cohortStatus; }
    public Long getCourseId() { return courseId; }
    public String getCourseName() { return courseName; }
    public LocalDate getSuggestedStartDate() { return suggestedStartDate; }
    public Set<TrainingDay> getTrainingDays() { return trainingDays; }
    public Integer getHorseCount() { return horseCount; }
    public Integer getSharedSessions() { return sharedSessions; }
    public Integer getTotalSessions() { return totalSessions; }
    public Long getWaitDays() { return waitDays; }
    public String getNote() { return note; }
}
