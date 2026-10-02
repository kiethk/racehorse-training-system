package com.rtms.backend.dto;

import com.rtms.backend.entity.CareSchedule;
import com.rtms.backend.entity.Horse;
import com.rtms.backend.entity.VetOffer;
import com.rtms.backend.enums.CareType;
import com.rtms.backend.enums.VetOfferStatus;

import java.time.LocalDateTime;

public record VetOfferResponse(
        Long id,
        Long careScheduleId,
        Long veterinarianId,
        LocalDateTime proposedScheduledAt,
        VetOfferStatus status,
        int round,
        LocalDateTime offeredAt,
        LocalDateTime expiresAt,
        LocalDateTime respondedAt,
        CareType careType,
        Long admissionId,
        Long horseId,
        String horseName,
        int durationMinutes,
        String description,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static VetOfferResponse from(VetOffer vo) {
        return from(vo, null, null);
    }

    public static VetOfferResponse from(VetOffer vo, CareSchedule cs, Horse horse) {
        if (vo == null) return null;
        return new VetOfferResponse(
                vo.getId(),
                vo.getCareScheduleId(),
                vo.getVeterinarianId(),
                vo.getProposedScheduledAt(),
                vo.getStatus(),
                vo.getRound(),
                vo.getOfferedAt(),
                vo.getExpiresAt(),
                vo.getRespondedAt(),
                cs != null ? cs.getCareType() : null,
                cs != null ? cs.getAdmissionId() : null,
                cs != null ? cs.getHorseId() : null,
                horse != null ? horse.getName() : null,
                cs != null ? cs.getDurationMinutes() : 0,
                cs != null ? cs.getDescription() : null,
                vo.getCreatedAt(),
                vo.getUpdatedAt()
        );
    }
}
