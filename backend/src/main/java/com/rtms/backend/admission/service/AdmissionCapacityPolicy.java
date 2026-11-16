package com.rtms.backend.admission.service;

/**
 * Capacity rules for the quarantine → regular-stall pipeline.
 *
 * <p>The invariant is: every horse that is in quarantine, or is about to enter it, must have a
 * regular stall waiting for it. Two gates enforce it:
 *
 * <ul>
 *   <li><b>Entry</b> (Groom approves a new admission): the new horse is not in quarantine yet, so
 *       the reserve must cover the horses already there <em>plus</em> this one.</li>
 *   <li><b>Approval</b> (Manager moves a horse out of quarantine): the horse is already counted in
 *       the occupied quarantine stalls, so the reserve only needs to cover the horses currently
 *       there. Adding one again would double-count it.</li>
 * </ul>
 */
public final class AdmissionCapacityPolicy {
    public static final String NO_QUARANTINE_STALL = "NO_QUARANTINE_STALL";
    public static final String REGULAR_RESERVE_INSUFFICIENT = "REGULAR_RESERVE_INSUFFICIENT";

    private AdmissionCapacityPolicy() { }

    /** Regular stalls needed before one more horse may enter quarantine. */
    public static long requiredRegularReserveForEntry(long occupiedQuarantine) {
        return occupiedQuarantine + 1;
    }

    /**
     * Regular stalls needed before a horse may leave quarantine.
     *
     * @param horseAlreadyInQuarantine true when the admission already holds an occupied/reserved
     *                                quarantine stall, so it is included in {@code occupiedQuarantine}
     */
    public static long requiredRegularReserveForApproval(long occupiedQuarantine,
            boolean horseAlreadyInQuarantine) {
        return occupiedQuarantine + (horseAlreadyInQuarantine ? 0 : 1);
    }

    public static boolean isAvailable(long availableQuarantine, long availableRegular,
            long occupiedQuarantine) {
        return availableQuarantine >= 1
                && availableRegular >= requiredRegularReserveForEntry(occupiedQuarantine);
    }

    public static String blockingReason(long availableQuarantine, long availableRegular,
            long occupiedQuarantine) {
        if (availableQuarantine < 1) return NO_QUARANTINE_STALL;
        if (availableRegular < requiredRegularReserveForEntry(occupiedQuarantine)) {
            return REGULAR_RESERVE_INSUFFICIENT;
        }
        return null;
    }

    public static boolean isApprovalAvailable(long availableRegular, long occupiedQuarantine,
            boolean horseAlreadyInQuarantine) {
        return availableRegular >= requiredRegularReserveForApproval(occupiedQuarantine,
                horseAlreadyInQuarantine);
    }

    public static String approvalBlockingReason(long availableRegular, long occupiedQuarantine,
            boolean horseAlreadyInQuarantine) {
        return isApprovalAvailable(availableRegular, occupiedQuarantine, horseAlreadyInQuarantine)
                ? null
                : REGULAR_RESERVE_INSUFFICIENT;
    }
}
