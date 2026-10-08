package com.rtms.backend.dto;

import com.rtms.backend.enums.TrainingDay;

import java.time.LocalDate;
import java.util.List;

/**
 * GHI DANH THEO NHÓM.
 *
 * Ba trường quyết định sự đồng bộ (courseId, startDate, trainingDays) nằm
 * NGOÀI danh sách ngựa -> mọi con trong một lời gọi BẮT BUỘC đồng bộ.
 * Đây là ràng buộc ở cấp cấu trúc request, không phải validation có thể quên.
 *
 * Vì sao quan trọng: hai con ngựa chỉ chung lot suốt cả khoá khi thoả CẢ BA
 * điều kiện. Lệch một buổi hay lệch một thứ trong tuần là lệch pha VĨNH VIỄN,
 * vì bài tập chọn theo courseSubjects[i % n] và hiệu i_A - i_B không đổi.
 *
 * ĐÃ BỎ so với bản cũ:
 *   - horseId (1 con)      -> thay bằng horses (N con)
 *   - groomId (cấp request) -> chuyển vào từng phần tử, và cho phép null
 *   - startTime / endTime   -> giờ giấc giờ do lot quyết định (BR-04 đã bỏ)
 */
public class CreateHorseTrainingPlanRequest {

    /** Danh sách chiến mã ghi danh cùng lúc. Tối thiểu 1 con. */
    private List<HorseEnrollmentRequest> horses;

    private Long courseId;

    private LocalDate startDate;

    /**
     * Các thứ trong tuần muốn tập, ví dụ ["MONDAY","WEDNESDAY","FRIDAY"].
     * BẮT BUỘC — không còn mặc định ngầm T2-4-6 như bản cũ, vì giá trị này
     * giờ được LƯU XUỐNG DB và trở thành dữ liệu thật của hồ sơ.
     */
    private List<TrainingDay> trainingDays;

    private String notes;

    public List<HorseEnrollmentRequest> getHorses() { return horses; }
    public void setHorses(List<HorseEnrollmentRequest> horses) { this.horses = horses; }

    public Long getCourseId() { return courseId; }
    public void setCourseId(Long courseId) { this.courseId = courseId; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public List<TrainingDay> getTrainingDays() { return trainingDays; }
    public void setTrainingDays(List<TrainingDay> trainingDays) { this.trainingDays = trainingDays; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}