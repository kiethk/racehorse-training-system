package com.rtms.backend.identity.controller;

import com.rtms.backend.common.dto.ApiResponse;
import com.rtms.backend.identity.dto.AccountSummaryResponse;
import com.rtms.backend.identity.dto.EligibleReplacementTrainerResponse;
import com.rtms.backend.identity.dto.StaffStatusUpdateRequest;
import com.rtms.backend.identity.service.AccountManagementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountManagementControllerTest {

    @Mock
    private AccountManagementService accountManagementService;

    private AccountManagementController controller;

    @BeforeEach
    void setUp() {
        controller = new AccountManagementController(accountManagementService);
    }

    @Test
    void getEligibleReplacements_returnsListOfEligibleTrainers() {
        Long trainerId = 5L;
        EligibleReplacementTrainerResponse item = new EligibleReplacementTrainerResponse(10L, "Active Trainer", "trainer@rtms.com", 1, 1, true);
        when(accountManagementService.getEligibleReplacementTrainers(trainerId)).thenReturn(List.of(item));

        ApiResponse<List<EligibleReplacementTrainerResponse>> response = controller.getEligibleReplacements(trainerId);

        assertTrue(response.isSuccess());
        assertEquals(1, response.getData().size());
        assertEquals(10L, response.getData().get(0).getId());
        verify(accountManagementService).getEligibleReplacementTrainers(trainerId);
    }

    @Test
    void updateAccountStatus_withReplacementTrainerId_delegatesToService() {
        Long trainerId = 5L;
        StaffStatusUpdateRequest request = new StaffStatusUpdateRequest();
        request.setActive(false);
        request.setReplacementTrainerId(10L);

        AccountSummaryResponse summary = new AccountSummaryResponse();
        summary.setUserId(trainerId);
        summary.setActive(false);

        when(accountManagementService.updateAccountStatus(any(), eq(trainerId), eq(false), eq(10L)))
                .thenReturn(summary);

        ApiResponse<AccountSummaryResponse> response = controller.updateAccountStatus(trainerId, request);

        assertTrue(response.isSuccess());
        assertFalse(response.getData().isActive());
        verify(accountManagementService).updateAccountStatus(any(), eq(trainerId), eq(false), eq(10L));
    }
}
