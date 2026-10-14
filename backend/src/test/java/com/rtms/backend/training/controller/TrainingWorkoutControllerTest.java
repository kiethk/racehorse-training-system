package com.rtms.backend.training.controller;

import com.rtms.backend.common.dto.ApiResponse;
import com.rtms.backend.security.AuthenticatedUser;
import com.rtms.backend.training.dto.WorkoutResultPreviewResponse;
import com.rtms.backend.training.service.TrainingWorkoutService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrainingWorkoutControllerTest {

    @Mock
    private TrainingWorkoutService workoutService;

    @Test
    void previewDisablesCachingAndReturnsGeneratedMeasurements() {
        AuthenticatedUser trainer = new AuthenticatedUser(9L, "trainer@example.com", "HEAD_TRAINER");
        WorkoutResultPreviewResponse preview = new WorkoutResultPreviewResponse(
                BigDecimal.valueOf(1_180), BigDecimal.valueOf(44.5),
                BigDecimal.valueOf(57.2), BigDecimal.valueOf(42.1), 151, 181, 91);
        when(workoutService.generateResultPreview(300L, trainer)).thenReturn(preview);

        TrainingWorkoutController controller = new TrainingWorkoutController(workoutService);
        ResponseEntity<ApiResponse<WorkoutResultPreviewResponse>> response =
                controller.previewWorkoutResult(300L, trainer);

        assertEquals("no-store", response.getHeaders().getCacheControl());
        assertSame(preview, response.getBody().getData());
    }
}
