package com.rtms.backend.dto;

import com.rtms.backend.enums.AdmissionStatus;
import jakarta.persistence.Column;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class AdmissionSummaryResponse {

    private Long admissionId;
    private AdmissionStatus status;
    private String candidateName;
    private String breed;
    private LocalDate dateOfBirth;
    private LocalDateTime submittedAt;
    private Long quarantineStallId;
    private String quarantineStallCode;
    private String imageUrl;


    /**
     * Dấu vết bước Huấn luyện viên.
     *
     * Vì sao cần ở DANH SÁCH chứ không chỉ ở màn chi tiết: đánh giá xong thì
     * hồ sơ chuyển sang bước Quản lý, nên không còn trạng thái nào mang nghĩa
     * "Trainer đã duyệt". Không có hai trường này thì màn hình chỉ suy được
     * "hồ sơ đã qua bước Trainer" chứ không biết AI đã duyệt — nhiều Trainer
     * sẽ thấy lẫn hồ sơ của nhau.
     *
     * trainerReviewedAt còn dùng để bắt cả hồ sơ bị Quản lý TỪ CHỐI sau khi
     * Trainer đã đánh giá — trường hợp mà lọc theo status bỏ sót, vì REJECTED
     * cũng có thể do Groom hoặc Thú y đặt từ trước đó.
     */
    private Long trainerId;
    private LocalDateTime trainerReviewedAt;

    // =================================================================
    // CONSTRUCTOR
    //
    // Chỉ constructor ĐẦY ĐỦ (11 tham số) gán giá trị. Mọi bản rút gọn đều
    // uỷ quyền xuống nó và truyền null cho phần thiếu.
    //
    // Quy tắc này quan trọng: nếu một bản rút gọn tự gán tay, thêm trường mới
    // sau này sẽ phải sửa nhiều chỗ và rất dễ sót. Và tuyệt đối không để bản
    // nào gọi this(...) với ĐÚNG danh sách tham số của chính nó — đó là đệ quy
    // vô hạn, trình biên dịch báo "recursive constructor invocation".
    // =================================================================

    /** Chỉ dữ liệu cơ bản. */
    public AdmissionSummaryResponse(
            Long admissionId,
            AdmissionStatus status,
            String candidateName,
            String breed,
            LocalDate dateOfBirth,
            LocalDateTime submittedAt,
            Long quarantineStallId) {
        this(admissionId, status, candidateName, breed, dateOfBirth, submittedAt,
                quarantineStallId, null, null, null, null);
    }

    /** Thêm mã chuồng cách ly. */
    public AdmissionSummaryResponse(
            Long admissionId,
            AdmissionStatus status,
            String candidateName,
            String breed,
            LocalDate dateOfBirth,
            LocalDateTime submittedAt,
            Long quarantineStallId,
            String quarantineStallCode) {
        this(admissionId, status, candidateName, breed, dateOfBirth, submittedAt,
                quarantineStallId, quarantineStallCode, null, null, null);
    }

    /** Thêm ảnh chiến mã — dùng ở màn hình Chủ ngựa (OwnerAdmissionService). */
    public AdmissionSummaryResponse(
            Long admissionId,
            AdmissionStatus status,
            String candidateName,
            String breed,
            LocalDate dateOfBirth,
            LocalDateTime submittedAt,
            Long quarantineStallId,
            String quarantineStallCode,
            String imageUrl) {
        this(admissionId, status, candidateName, breed, dateOfBirth, submittedAt,
                quarantineStallId, quarantineStallCode, imageUrl, null, null);
    }

    /** Bản ĐẦY ĐỦ — nơi duy nhất gán giá trị. */
    public AdmissionSummaryResponse(
            Long admissionId,
            AdmissionStatus status,
            String candidateName,
            String breed,
            LocalDate dateOfBirth,
            LocalDateTime submittedAt,
            Long quarantineStallId,
            String quarantineStallCode,
            String imageUrl,
            Long trainerId,
            LocalDateTime trainerReviewedAt
    ) {
        this.admissionId = admissionId;
        this.status = status;
        this.candidateName = candidateName;
        this.breed = breed;
        this.dateOfBirth = dateOfBirth;
        this.submittedAt = submittedAt;
        this.quarantineStallId = quarantineStallId;
        this.quarantineStallCode = quarantineStallCode;
        this.trainerId = trainerId;
        this.trainerReviewedAt = trainerReviewedAt;
        this.imageUrl = imageUrl;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public Long getAdmissionId() {
        return admissionId;
    }

    public AdmissionStatus getStatus() {
        return status;
    }

    public String getCandidateName() {
        return candidateName;
    }

    public String getBreed() {
        return breed;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public Long getQuarantineStallId() {
        return quarantineStallId;
    }

    /**
     * Mã chuồng cách ly, ví dụ "Q3".
     *
     * Vì sao không dùng quarantineStallId: đó là khoá nội bộ, không in trên
     * biển chuồng. Trainer cầm điện thoại xuống khu cách ly cần mã người đọc
     * được. Không có trường này thì phải mở chi tiết từng đơn mới biết đi đâu.
     */
    public String getQuarantineStallCode() {
        return quarantineStallCode;
    }

    public Long getTrainerId() {
        return trainerId;
    }

    public LocalDateTime getTrainerReviewedAt() {
        return trainerReviewedAt;
    }
}