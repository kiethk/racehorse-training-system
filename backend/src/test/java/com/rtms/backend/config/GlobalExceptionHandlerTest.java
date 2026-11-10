package com.rtms.backend.config;

import com.rtms.backend.common.dto.ApiResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleIllegalState_returnsConflictStatus() {
        IllegalStateException ex = new IllegalStateException("Admission is already approved");
        ResponseEntity<ApiResponse<Object>> response = handler.handleIllegalState(ex);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Admission is already approved", response.getBody().getMessage());
        assertEquals("INVALID_REVIEW_STATE", response.getBody().getErrorCode());
    }

    @Test
    void handleBadRequest_returnsBadRequestStatus() {
        IllegalArgumentException ex = new IllegalArgumentException("Invalid parameter");
        ResponseEntity<ApiResponse<Object>> response = handler.handleBadRequest(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Invalid parameter", response.getBody().getMessage());
        assertEquals("VALIDATION_ERROR", response.getBody().getErrorCode());
    }
}
