package com.rtms.backend.dto;

/**
 * Một chiến mã trong lời ghi danh theo nhóm.
 */
public class HorseEnrollmentRequest {

    private Long horseId;

    /**
     * Groom phụ trách. Để null thì hệ thống tự suy từ chuồng của con ngựa
     * (StableStall.groomId) — đúng thực tế: người chăm con ngựa cũng là
     * người dắt nó ra sân.
     * Chỉ điền tay khi cần ghi đè (Groom chính nghỉ ốm chẳng hạn).
     */
    private Long groomId;

    public Long getHorseId() { return horseId; }
    public void setHorseId(Long horseId) { this.horseId = horseId; }

    public Long getGroomId() { return groomId; }
    public void setGroomId(Long groomId) { this.groomId = groomId; }
}
