package com.rtms.backend.enums;

public enum VetExamType {
    URGENT(400),
    INITIAL(300),
    FOLLOW_UP(200),
    ROUTINE(100);

    private final int defaultPriority;

    VetExamType(int defaultPriority) {
        this.defaultPriority = defaultPriority;
    }

    public int getDefaultPriority() {
        return defaultPriority;
    }
}
