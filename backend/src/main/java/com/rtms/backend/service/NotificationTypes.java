package com.rtms.backend.service;

/**
 * Stable machine-readable notification event codes stored in {@code notifications.notification_type}.
 * Clients route/render by (notificationType, referenceType, referenceId); never by title/message.
 */
public final class NotificationTypes {

    public static final String ADMISSION_VET_ASSIGNED = "ADMISSION_VET_ASSIGNED";
    public static final String ADMISSION_TRAINER_ASSIGNED = "ADMISSION_TRAINER_ASSIGNED";
    public static final String CARE_SCHEDULE_TRAINER_ASSIGNED = "CARE_SCHEDULE_TRAINER_ASSIGNED";

    public static final String REFERENCE_ADMISSION = "ADMISSION";
    public static final String REFERENCE_CARE_SCHEDULE = "CARE_SCHEDULE";

    private NotificationTypes() {
    }
}