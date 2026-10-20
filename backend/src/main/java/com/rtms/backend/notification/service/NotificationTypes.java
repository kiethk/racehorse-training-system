package com.rtms.backend.notification.service;

/**
 * Stable machine-readable notification event codes stored in {@code notifications.notification_type}.
 * Clients route/render by (notificationType, referenceType, referenceId); never by title/message.
 */
public final class NotificationTypes {

    public static final String ADMISSION_VET_ASSIGNED = "ADMISSION_VET_ASSIGNED";
    public static final String ADMISSION_TRAINER_ASSIGNED = "ADMISSION_TRAINER_ASSIGNED";
    public static final String CARE_SCHEDULE_URGENT_ASSIGNED = "CARE_SCHEDULE_URGENT_ASSIGNED";
    public static final String URGENT_CASE_CLOSED = "URGENT_CASE_CLOSED";
    public static final String VACCINE_DUE_SOON = "VACCINE_DUE_SOON";
    public static final String VACCINE_OVERDUE = "VACCINE_OVERDUE";
    public static final String VACCINE_COMPLETED = "VACCINE_COMPLETED";
    public static final String ROUTINE_ASSIGNED = "ROUTINE_ASSIGNED";

    public static final String REFERENCE_ADMISSION = "ADMISSION";
    public static final String REFERENCE_CARE_SCHEDULE = "CARE_SCHEDULE";
    public static final String REFERENCE_VACCINATION = "VACCINATION";
    public static final String REFERENCE_HORSE = "HORSE";

    private NotificationTypes() {
    }
}