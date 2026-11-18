package com.rtms.backend.admission.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdmissionCapacityPolicyTest {

    @Test
    @DisplayName("Entry: cần thêm một chuồng thường cho ngựa sắp vào cách ly")
    void entryRequiresOneRegularStallBeyondQuarantineOccupancy() {
        assertEquals(4, AdmissionCapacityPolicy.requiredRegularReserveForEntry(3));
        assertTrue(AdmissionCapacityPolicy.isAvailable(1, 4, 3));
        assertFalse(AdmissionCapacityPolicy.isAvailable(1, 3, 3));
        assertEquals(AdmissionCapacityPolicy.REGULAR_RESERVE_INSUFFICIENT,
                AdmissionCapacityPolicy.blockingReason(1, 3, 3));
    }

    @Test
    @DisplayName("Entry: hết chuồng cách ly thì chặn trước cả khi xét reserve")
    void entryRequiresAFreeQuarantineStall() {
        assertEquals(AdmissionCapacityPolicy.NO_QUARANTINE_STALL,
                AdmissionCapacityPolicy.blockingReason(0, 99, 0));
        assertFalse(AdmissionCapacityPolicy.isAvailable(0, 99, 0));
    }

    @Test
    @DisplayName("Approval: ngựa đã nằm trong khu cách ly nên không cộng thêm một lần nữa")
    void approvalDoesNotDoubleCountHorseAlreadyInQuarantine() {
        assertEquals(5, AdmissionCapacityPolicy.requiredRegularReserveForApproval(5, true));
        assertTrue(AdmissionCapacityPolicy.isApprovalAvailable(5, 5, true));
        assertFalse(AdmissionCapacityPolicy.isApprovalAvailable(4, 5, true));
        assertEquals(AdmissionCapacityPolicy.REGULAR_RESERVE_INSUFFICIENT,
                AdmissionCapacityPolicy.approvalBlockingReason(4, 5, true));
        assertNull(AdmissionCapacityPolicy.approvalBlockingReason(5, 5, true));
    }

    @Test
    @DisplayName("Approval: admission không có chuồng cách ly vẫn là một ngựa cần chuồng thường")
    void approvalWithoutQuarantineStallRequiresOneMoreRegularStall() {
        assertEquals(6, AdmissionCapacityPolicy.requiredRegularReserveForApproval(5, false));
        assertFalse(AdmissionCapacityPolicy.isApprovalAvailable(5, 5, false));
        assertTrue(AdmissionCapacityPolicy.isApprovalAvailable(6, 5, false));
    }
}
