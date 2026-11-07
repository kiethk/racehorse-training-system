package com.rtms.backend.medical.enums;

import java.util.List;

public enum CareScheduleStatus {
    SCHEDULED,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED;

    /**
     * Active/open statuses that still require attention.
     */
    public static final List<CareScheduleStatus> OPEN =
            List.of(SCHEDULED, IN_PROGRESS);

    public static final List<CareScheduleStatus> ASSIGNED_OR_BOOKED =
            List.of(SCHEDULED, IN_PROGRESS);

    /** The vet physically has this urgent exam in hand right now. */
    public static final List<CareScheduleStatus> ACTIVE_EXAM =
            List.of(IN_PROGRESS);

    public static final List<CareScheduleStatus> BOOKED_OR_ACTIVE =
            List.of(SCHEDULED, IN_PROGRESS);

}
