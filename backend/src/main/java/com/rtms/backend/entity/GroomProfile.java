package com.rtms.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "groom_profiles")
public class GroomProfile {

    @Id
    @Column(name = "user_id")
    private Long userId;

    /**
     * Trainer quản lý chuyên môn trực tiếp Groom này (V29).
     * Hiện chưa code nào ghi/đọc — Club Manager sẽ gán khi tạo tài khoản Groom.
     * Giữ lại vì đã có trong thiết kế quan hệ Trainer -> Groom.
     */
    @Column(name = "trainer_id")
    private Long trainerId;

    // ĐÃ XOÁ (V46): field shift kiểu GroomShift — enum chết, xem V46 để biết lý do

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Long getTrainerId() { return trainerId; }
    public void setTrainerId(Long trainerId) { this.trainerId = trainerId; }
}
