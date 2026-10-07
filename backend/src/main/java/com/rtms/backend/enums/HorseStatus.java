package com.rtms.backend.enums;

/**
 * Vòng đời của ngựa trong câu lạc bộ — chỉ nói ngựa đang ở bước nào.
 *
 * Sức khỏe KHÔNG nằm ở đây: ngựa bị thương vẫn là ELIGIBLE, còn việc có được
 * tập hay không do Horse.trainingDecision quyết định.
 */
public enum HorseStatus {
    CANDIDATE,
    ELIGIBLE,
    REJECTED
}
