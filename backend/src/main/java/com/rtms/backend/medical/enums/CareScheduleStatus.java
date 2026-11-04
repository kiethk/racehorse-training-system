package com.rtms.backend.medical.enums;

import java.util.List;

public enum CareScheduleStatus {
    REQUESTED,
    SCHEDULED,
    IN_PROGRESS,
    PAUSED,
    /** AD-026: Vet has entered and started the exam, but it exceeded planned duration. */
    DELAYED,
    /** AD-022 / AD-026: Appointment time passed more than 30 minutes, but Vet NEVER clicked Start Exam. */
    OVERDUE,
    COMPLETED;

    /**
     * Every status that still owes the veterinarian some work (AD-022 / AD-026 lifecycle refactor).
     * Use this instead of hand-listing statuses wherever the old code passed
     * REQUESTED/SCHEDULED/IN_PROGRESS, so a future status cannot be silently forgotten.
     */
    public static final List<CareScheduleStatus> OPEN =
            List.of(REQUESTED, SCHEDULED, IN_PROGRESS, PAUSED, DELAYED, OVERDUE);

    /** OPEN minus the unassigned queue entry, for caseload and workload queries. */
    public static final List<CareScheduleStatus> ASSIGNED_OR_BOOKED =
            List.of(SCHEDULED, IN_PROGRESS, PAUSED, DELAYED, OVERDUE);

    /** The vet physically has this exam in hand right now (used by slot/preempt guards). */
    public static final List<CareScheduleStatus> ACTIVE_EXAM =
            List.of(IN_PROGRESS, DELAYED, OVERDUE);

    /** The former {SCHEDULED, IN_PROGRESS} plus DELAYED/OVERDUE: a booked or held exam taking a slot. */
    public static final List<CareScheduleStatus> BOOKED_OR_ACTIVE =
            List.of(SCHEDULED, IN_PROGRESS, DELAYED, OVERDUE);

}
