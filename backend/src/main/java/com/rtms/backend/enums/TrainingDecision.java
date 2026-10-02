package com.rtms.backend.enums;

public enum TrainingDecision {
    ALLOWED,
    RESTRICTED,
    BLOCKED;

    public TrainingStatus toTrainingStatus() {
        return switch (this) {
            case ALLOWED -> TrainingStatus.ALLOWED;
            case RESTRICTED -> TrainingStatus.RESTRICTED;
            case BLOCKED -> TrainingStatus.BLOCKED;
        };
    }
}