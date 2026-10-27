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
    /** A new admission is waiting in the quarantine grooms' review pool. */
    public static final String ADMISSION_NEW_FOR_GROOM = "ADMISSION_NEW_FOR_GROOM";
    /** The admission's horse was given a stall this groom looks after. */
    public static final String ADMISSION_GROOM_ASSIGNED = "ADMISSION_GROOM_ASSIGNED";
    /** Admission accepted by quarantine groom, quarantine stall reserved, awaiting horse arrival. */
    public static final String ADMISSION_WAITING_FOR_ARRIVAL = "ADMISSION_WAITING_FOR_ARRIVAL";
    /** Admission officially approved by manager, regular stall allocated, horse is eligible. */
    public static final String ADMISSION_APPROVED = "ADMISSION_APPROVED";
    /** Admission rejected by groom or manager. */
    public static final String ADMISSION_REJECTED = "ADMISSION_REJECTED";
    /** Quarantine reservation expired after 14 days without arrival. */
    public static final String ADMISSION_ARRIVAL_EXPIRED = "ADMISSION_ARRIVAL_EXPIRED";
    public static final String HORSE_EDIT_REQUESTED = "HORSE_EDIT_REQUESTED";
    public static final String HORSE_EDIT_REVIEWED = "HORSE_EDIT_REVIEWED";

    public static final String REFERENCE_ADMISSION = "ADMISSION";
    public static final String REFERENCE_CARE_SCHEDULE = "CARE_SCHEDULE";
    public static final String REFERENCE_VACCINATION = "VACCINATION";
    public static final String REFERENCE_HORSE = "HORSE";
    public static final String REFERENCE_HORSE_EDIT_REQUEST = "HORSE_EDIT_REQUEST";

    private NotificationTypes() {
    }
}