package com.rtms.backend.identity.controller;

import com.rtms.backend.common.dto.ApiResponse;
import com.rtms.backend.identity.dto.EligibleReplacementTrainerResponse;
import com.rtms.backend.identity.dto.StaffStatusUpdateRequest;
import com.rtms.backend.identity.dto.StaffSummaryResponse;
import com.rtms.backend.identity.service.StaffManagementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StaffManagementControllerTest {

    @Mock
    private StaffManagementService staffManagementService;

    private StaffManagementController controller;

    @BeforeEach
    void setUp() {
        controller = new StaffManagementController(staffManagementService);
    }

    @Test
    void getEligibleReplacements_returnsListOfEligibleTrainers() {
        Long trainerId = 5L;
        EligibleReplacementTrainerResponse item = new EligibleReplacementTrainerResponse(10L, "Active Trainer", "trainer@rtms.com", 1, 1, true);
        when(staffManagementService.getEligibleReplacementTrainers(trainerId)).thenReturn(List.of(item));

        ApiResponse<List<EligibleReplacementTrainerResponse>> response = controller.getEligibleReplacements(trainerId);

        assertTrue(response.isSuccess());
        assertEquals(1, response.getData().size());
        assertEquals(10L, response.getData().get(0).getId());
        verify(staffManagementService).getEligibleReplacementTrainers(trainerId);
    }

    @Test
    void updateStaffStatus_withReplacementTrainerId_delegatesToService() {
        Long trainerId = 5L;
        StaffStatusUpdateRequest request = new StaffStatusUpdateRequest();
        request.setActive(false);
        request.setReplacementTrainerId(10L);

        StaffSummaryResponse summary = new StaffSummaryResponse();
        summary.setUserId(trainerId);
        summary.setActive(false);

        when(staffManagementService.updateStaffStatus(eq(trainerId), eq(false), eq(10L)))
                .thenReturn(summary);

        ApiResponse<StaffSummaryResponse> response = controller.updateStaffStatus(trainerId, request);

        assertTrue(response.isSuccess());
        assertFalse(response.getData().isActive());
        verify(staffManagementService).updateStaffStatus(eq(trainerId), eq(false), eq(10L));
    }
}
