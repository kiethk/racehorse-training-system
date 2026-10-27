package com.rtms.backend.identity.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

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

    /** Lần gần nhất Groom nhận một ngựa mới vào chuồng của mình; NULL = chưa từng nhận (V202610261300). */
    @Column(name = "last_horse_assigned_at")
    private LocalDateTime lastHorseAssignedAt;

    // ĐÃ XOÁ (V46): field shift kiểu GroomShift — enum chết, xem V46 để biết lý do

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Long getTrainerId() { return trainerId; }
    public void setTrainerId(Long trainerId) { this.trainerId = trainerId; }

    public LocalDateTime getLastHorseAssignedAt() { return lastHorseAssignedAt; }
    public void setLastHorseAssignedAt(LocalDateTime lastHorseAssignedAt) { this.lastHorseAssignedAt = lastHorseAssignedAt; }
}
