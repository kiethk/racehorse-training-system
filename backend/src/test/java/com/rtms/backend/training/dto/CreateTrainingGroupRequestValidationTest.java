package com.rtms.backend.training.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Một chỗ trống trong danh sách ngựa phải bị từ chối khi kiểm tra request, không để lọt tới service. (An empty entry in the horse list is rejected by request validation and never reaches the service.) */
class CreateTrainingGroupRequestValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void aNullHorseIdIsRejected() {
        assertFalse(validator.validate(new CreateTrainingGroupRequest("Group", Arrays.asList(5L, null))).isEmpty());
    }

    @Test
    void realHorseIdsAndAnEmptyListAreAccepted() {
        assertTrue(validator.validate(new CreateTrainingGroupRequest("Group", List.of(5L, 6L))).isEmpty());
        assertTrue(validator.validate(new CreateTrainingGroupRequest("Group", List.of())).isEmpty());
    }
}
