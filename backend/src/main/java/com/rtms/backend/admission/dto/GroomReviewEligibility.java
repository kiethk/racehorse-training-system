package com.rtms.backend.admission.dto;

/**
 * What the viewing groom may do with an admission in GROOM_REVIEW right now. A snapshot: the approval
 * itself re-checks everything under the stall lock.
 *
 * @param approveBlockedReason NOT_IN_GROOM_POOL, OLDER_ADMISSION_FIRST, NO_QUARANTINE_STALL or
 *                             REGULAR_RESERVE_INSUFFICIENT; null when the groom can approve
 * @param targetStallCode      the groom's stall that approving would reserve; null when they cannot approve
 */
public record GroomReviewEligibility(
        boolean canApprove,
        boolean canReject,
        String approveBlockedReason,
        String targetStallCode) {
}
