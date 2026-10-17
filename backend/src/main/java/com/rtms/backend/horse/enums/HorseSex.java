package com.rtms.backend.horse.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

/** Biological sex; castration is not a separate sex. */
public enum HorseSex {
    MALE,
    FEMALE;

    /** Reject numeric enum ordinals and unsupported labels at the API boundary. */
    @JsonCreator
    public static HorseSex fromJson(String value) {
        return HorseSex.valueOf(value);
    }
}
