package com.rtms.backend.admission.enums;

public enum ReviewDecision {
    APPROVED,
    REJECTED,
    /** Vet review: accept the examination and complete it normally (null is also treated as accept). */
    ACCEPT,
    /** Vet review: reject the admission application. */
    REJECT
}